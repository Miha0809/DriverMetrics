package com.apexcode.drivermetrics.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.parser.ParserRegistry
import com.apexcode.drivermetrics.overlay.OverlayController
import com.apexcode.drivermetrics.pipeline.CurrentOrderRepository
import com.apexcode.drivermetrics.pipeline.OrderOrchestrator
import com.apexcode.drivermetrics.pipeline.PipelineStatusRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches window changes across the supported driver apps (see
 * res/xml/accessibility_service_config.xml), debounces bursts of layout-change events, and
 * hands the active node tree to the ParserRegistry. Never crashes on a bad tree — every failure
 * degrades to "no order" (see ParserRegistry).
 */
@AndroidEntryPoint
class DriverMetricsAccessibilityService : AccessibilityService() {

    @Inject lateinit var parserRegistry: ParserRegistry
    @Inject lateinit var currentOrderRepository: CurrentOrderRepository
    @Inject lateinit var overlayController: OverlayController
    @Inject lateinit var pipelineStatusRepository: PipelineStatusRepository
    @Inject lateinit var orderOrchestrator: OrderOrchestrator

    private var serviceScope: CoroutineScope? = null
    private var pendingAnalysis: Job? = null
    private var burstStartElapsedMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val scope = CoroutineScope(SupervisorJob())
        serviceScope = scope
        orderOrchestrator.start(scope)
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        // TYPE_WINDOWS_CHANGED — fired when the set of windows on screen changes, e.g. Uber's
        // own new-order popup appearing as a separate window without stealing focus, unlike
        // Bolt which brings its main screen to the foreground — doesn't reliably carry a useful
        // packageName of its own, so this is purely a "something worth re-checking" signal.
        pipelineStatusRepository.recordEvent(event.packageName?.toString() ?: "(windows changed)")
        scheduleAnalysis()
    }

    private fun scheduleAnalysis() {
        val scope = serviceScope ?: return
        val now = SystemClock.elapsedRealtime()
        if (pendingAnalysis == null) {
            burstStartElapsedMs = now
        }
        pendingAnalysis?.cancel()

        val elapsedInBurst = now - burstStartElapsedMs
        val delayMs = if (elapsedInBurst >= HARD_CAP_MS) 0L else DEBOUNCE_MS

        pendingAnalysis = scope.launch {
            delay(delayMs)
            pendingAnalysis = null
            analyze()
        }
    }

    private suspend fun analyze() {
        val order = findOrder()
        pipelineStatusRepository.recordOrder(order)
        currentOrderRepository.update(order)
    }

    /**
     * Scans every currently visible window belonging to a tracked aggregator app (the same set
     * declared in accessibility_service_config.xml's packageNames) — not just rootInActiveWindow
     * — and returns the first order any registered parser recognizes.
     *
     * rootInActiveWindow alone only ever exposes ONE window, and for Uber that isn't
     * necessarily the right one: a new order shows up in Uber's own popup/alert-style window
     * layered on top of whatever else is on screen, which doesn't take focus and so never
     * becomes the "active" window — Bolt, by contrast, brings its main screen to the foreground,
     * which is why rootInActiveWindow alone used to work for it. Trying every tracked window
     * (deduped by window id, rootInActiveWindow included as a safety net) instead of special
     * -casing Uber keeps this generic for any future aggregator that shows orders the same way.
     */
    private fun findOrder(): TaxiOrder? {
        val trackedPackages = serviceInfo?.packageNames?.toSet() ?: emptySet()
        if (trackedPackages.isEmpty()) return null

        val seenWindowIds = mutableSetOf<Int>()
        val roots = mutableListOf<Pair<String, AccessibilityNodeInfo>>()

        windows?.forEach { window ->
            val root = window.root ?: return@forEach
            val pkg = root.packageName?.toString() ?: return@forEach
            if (pkg in trackedPackages && seenWindowIds.add(window.id)) {
                roots += pkg to root
            }
        }

        rootInActiveWindow?.let { active ->
            val pkg = active.packageName?.toString()
            val activeWindowId = active.window?.id
            if (pkg in trackedPackages && (activeWindowId == null || seenWindowIds.add(activeWindowId))) {
                roots += pkg!! to active
            }
        }

        return roots.firstNotNullOfOrNull { (pkg, root) -> parserRegistry.parseOrder(pkg, root) }
    }

    override fun onInterrupt() {
        // Required override; no persistent state to tear down here.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope?.cancel()
        serviceScope = null
        overlayController.dispose()
    }

    private companion object {
        const val TAG = "DriverMetricsA11y"
        const val DEBOUNCE_MS = 120L
        const val HARD_CAP_MS = 350L
    }
}
