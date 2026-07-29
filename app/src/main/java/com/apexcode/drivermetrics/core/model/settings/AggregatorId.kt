package com.apexcode.drivermetrics.core.model.settings

/**
 * One entry per supported aggregator. Adding a new aggregator (e.g. FreeNow) is a single new
 * case here plus a parser that sets [com.apexcode.drivermetrics.core.model.TaxiOrder.sourceApplication]
 * to its [sourceApplication] — nothing about the settings storage or evaluation engine changes.
 */
enum class AggregatorId(val sourceApplication: String, val displayName: String) {
    BOLT("bolt", "Bolt"),
    UBER("uber", "Uber"),
    FREE_NOW("freenow", "FreeNow"),
    ;

    companion object {
        fun fromSourceApplication(sourceApplication: String): AggregatorId? =
            entries.firstOrNull { it.sourceApplication == sourceApplication }
    }
}
