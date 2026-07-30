package com.apexcode.drivermetrics.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.apexcode.drivermetrics.core.model.MapRoute
import com.apexcode.drivermetrics.core.model.OrderMetrics
import com.apexcode.drivermetrics.core.model.RouteInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.settings.MapSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Owns the single TYPE_APPLICATION_OVERLAY window used to show order metrics. Requires the
 * "draw over other apps" permission (Settings.canDrawOverlays) — MainActivity walks the driver
 * through granting it since it can't be requested as a normal runtime permission.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapSettingsRepository: MapSettingsRepository,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val state = mutableStateOf<OverlayState?>(null)
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    // A fresh owner per shown window rather than one reused for the controller's whole
    // lifetime: performRestore() can only be called once while a lifecycle is still in its
    // initial state, so reusing the same owner across a hide() -> show() cycle (which happens
    // constantly as orders appear/disappear) crashed with "performRestore cannot be called when
    // owner is RESUMED".
    private var currentLifecycleOwner: OverlayLifecycleOwner? = null
    // Set by the overlay's own close (X) button. OrderOrchestrator's present() calls show()
    // twice per order — once with stats only, again once the map route finishes fetching — so
    // without this, closing the overlay while that second call is still in flight would just
    // have it pop back open. Cleared the moment a different order is shown.
    private var dismissedOrder: TaxiOrder? = null

    /**
     * Called from the accessibility service's background pipeline coroutine, but WindowManager
     * and Lifecycle both require the main thread — hence the dispatcher switch here rather than
     * pushing that requirement onto every caller.
     */
    suspend fun show(
        order: TaxiOrder,
        metrics: OrderMetrics,
        route: RouteInfo,
        mapRoute: MapRoute? = null,
        showStats: Boolean = true,
        showMap: Boolean = true,
    ) {
        withContext(Dispatchers.Main.immediate) {
            if (order == dismissedOrder) return@withContext
            dismissedOrder = null
            state.value = OverlayState(order, metrics, route, mapRoute, showStats, showMap)
            ensureViewAdded()
            applyWindowHeight(showMap)
        }
    }

    suspend fun hide() {
        withContext(Dispatchers.Main.immediate) {
            dismissedOrder = null
            state.value = null
            removeViewIfPresent()
        }
    }

    /** Called from the overlay's own close (X) button — same as [hide], but remembers the
     *  dismissed order so it stays closed for it. See [dismissedOrder]. */
    private suspend fun dismiss() {
        withContext(Dispatchers.Main.immediate) {
            dismissedOrder = state.value?.order
            state.value = null
            removeViewIfPresent()
        }
    }

    private fun ensureViewAdded() {
        if (composeView != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission not granted, cannot show the metrics overlay")
            return
        }

        val owner = OverlayLifecycleOwner()
        owner.performRestore()
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val wideMapZoom by mapSettingsRepository.wideMapZoom.collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                state.value?.let {
                    OverlayContent(
                        metrics = it.metrics,
                        route = it.route,
                        mapRoute = it.mapRoute,
                        wideMapZoom = wideMapZoom,
                        showStats = it.showStats,
                        showMap = it.showMap,
                        onClose = { scope.launch { dismiss() } },
                    )
                }
            }
        }

        val params = buildLayoutParams(state.value?.showMap ?: true)

        try {
            windowManager.addView(view, params)
            composeView = view
            layoutParams = params
            currentLifecycleOwner = owner
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add the overlay window", e)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private fun buildLayoutParams(showMap: Boolean) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        heightPx(showMap),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
        y = TOP_EDGE_SHIFT_PX
    }

    /**
     * The window's own height, not just its content: with a full route to draw, the same
     * fraction-of-screen height as before; with the map turned off (9's показ на екрані toggle),
     * just tall enough for the stats line, so the window's bottom edge sits right under it
     * instead of leaving empty overlay space. Gravity.TOP + a fixed `y` (see [buildLayoutParams])
     * means shrinking height only moves the bottom edge — the top edge never moves.
     */
    private fun heightPx(showMap: Boolean): Int = if (showMap) {
        (context.resources.displayMetrics.heightPixels * TOP_HALF_FRACTION).roundToInt() - TOP_EDGE_SHIFT_PX
    } else {
        (COMPACT_OVERLAY_HEIGHT.value * context.resources.displayMetrics.density).roundToInt()
    }

    private fun applyWindowHeight(showMap: Boolean) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val newHeight = heightPx(showMap)
        if (params.height == newHeight) return
        params.height = newHeight
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resize the overlay window", e)
        }
    }

    private fun removeViewIfPresent() {
        val view = composeView
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove the overlay window", e)
            }
        }
        composeView = null
        layoutParams = null
        currentLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        currentLifecycleOwner = null
    }

    /** Call when the owning service is torn down, so a stuck window never outlives it. */
    fun dispose() {
        removeViewIfPresent()
    }

    private companion object {
        const val TAG = "OverlayController"
        const val TOP_HALF_FRACTION = 0.5
        const val TOP_EDGE_SHIFT_PX = 0
    }
}
