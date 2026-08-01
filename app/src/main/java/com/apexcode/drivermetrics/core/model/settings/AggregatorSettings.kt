package com.apexcode.drivermetrics.core.model.settings

import kotlinx.serialization.Serializable

/**
 * Everything a driver can personalize for one aggregator (9.1). [evaluationCriteria] and
 * [filterRules] are keyed by the criterion/rule's stable string id (see
 * [com.apexcode.drivermetrics.metrics.StandardCriteria] and
 * [com.apexcode.drivermetrics.metrics.StandardFilterRules]) rather than dedicated fields, so 9.6's
 * "add new criteria/filters without changing the data model" holds: a new criterion is a new
 * object implementing the plug-in interface, not a new field here.
 */
@Serializable
data class AggregatorSettings(
    val routeDisplayMode: RouteDisplayMode = RouteDisplayMode.CLIENT_TO_DROPOFF_ONLY,
    val evaluationCriteria: Map<String, CriterionThreshold> = emptyMap(),
    val filterRules: Map<String, Double> = emptyMap(),
    val displayOptions: Map<String, Boolean> = emptyMap(),
    // Off by default: preserves the existing €/год formula for drivers who haven't opened
    // settings. When on, MetricsCalculator adds MetricsCalculator.FREE_WAITING_TIME_MINUTES to
    // the order's total time before deriving €/год, per the driver's real free waiting allowance.
    val includeFreeWaitingTime: Boolean = false,
)

/** Missing id = on, so existing settings blobs (saved before a new toggle existed) still show it. */
fun AggregatorSettings.isDisplayOptionEnabled(id: String): Boolean = displayOptions[id] ?: true
