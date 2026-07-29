package com.apexcode.drivermetrics.core.model.settings

/**
 * Per-aggregator toggles for what the overlay shows. Kept as a generic id -> Boolean map on
 * [AggregatorSettings] (same shape as evaluationCriteria/filterRules) rather than dedicated
 * fields, so a new toggle is one new id here plus one read site in the overlay — no model change.
 * A missing id defaults to "on" (see [AggregatorSettings.isDisplayOptionEnabled]), matching the
 * app's original always-show behavior for drivers who haven't opened settings.
 */
object StandardDisplayOptions {
    const val SHOW_STATS = "show_stats"
    const val SHOW_MAP = "show_map"
}
