package com.apexcode.drivermetrics.pipeline

import com.apexcode.drivermetrics.core.model.TaxiOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "the order currently on screen" (null when there is none) — written
 * by both the accessibility service (real detections) and MainActivity's debug buttons (simulated
 * ones), and collected by [OrderOrchestrator] to drive the overlay. Funneling both sources through
 * one StateFlow, rather than each calling the overlay pipeline directly, is what lets the
 * collectLatest in OrderOrchestrator cancel stale in-flight work the instant the order changes.
 */
@Singleton
class CurrentOrderRepository @Inject constructor() {
    private val _currentOrder = MutableStateFlow<TaxiOrder?>(null)
    val currentOrder: StateFlow<TaxiOrder?> = _currentOrder.asStateFlow()

    /**
     * Skips the update when [order] is the same order already on screen, differing only by
     * [TaxiOrder.timestamp]. The accessibility service re-parses the same visible order on every
     * layout-change event (every ~300ms while it's on screen), each time producing a TaxiOrder
     * with a fresh timestamp — without this check, every one of those re-parses would count as
     * "the order changed" and restart OrderOrchestrator's collectLatest block, which is what made
     * the overlay's map keep disappearing and reappearing throughout a single order.
     */
    fun update(order: TaxiOrder?) {
        val current = _currentOrder.value
        if (order != null && current != null && current.copy(timestamp = order.timestamp) == order) {
            return
        }
        _currentOrder.value = order
    }
}
