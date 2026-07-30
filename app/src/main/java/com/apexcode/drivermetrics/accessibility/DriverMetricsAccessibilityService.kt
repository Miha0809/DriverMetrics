package com.apexcode.drivermetrics.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        pipelineStatusRepository.recordEvent(packageName)
        scheduleAnalysis(packageName)
    }

    private fun scheduleAnalysis(packageName: String) {
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
            analyze(packageName)
        }
    }

    private suspend fun analyze(packageName: String) {
        val root = rootInActiveWindow
        if (root == null) {
            currentOrderRepository.update(null)
            return
        }
        val order = parserRegistry.parseOrder(packageName, root)
        pipelineStatusRepository.recordOrder(order)
        currentOrderRepository.update(order)
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
