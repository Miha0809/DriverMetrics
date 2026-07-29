package com.apexcode.drivermetrics.metrics

import com.apexcode.drivermetrics.core.model.TaxiOrder

/**
 * Everything an [EvaluationCriterion] or [FilterRule] might need to read a value off of. Adding a
 * new field here to support a future criterion is safe — existing criteria/rules just ignore it.
 */
data class EvaluationContext(
    val order: TaxiOrder,
    val effectivePrice: Double,
    val pricePerHour: Double,
    val pricePerKm: Double,
    val pickupDistanceKm: Double?,
    val pickupDurationMin: Double?,
)
