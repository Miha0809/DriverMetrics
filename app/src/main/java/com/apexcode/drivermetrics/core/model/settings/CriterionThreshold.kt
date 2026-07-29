package com.apexcode.drivermetrics.core.model.settings

import kotlinx.serialization.Serializable

/**
 * User-configured bounds for one evaluation criterion (9.3). Both bounds are opt-in — a driver
 * who only cares about a hard floor sets [redBelow] and leaves [greenAtOrAbove] null, so the
 * criterion can only ever push a value to RED or leave it at YELLOW, never GREEN. A criterion
 * whose entry has both bounds null is treated by [com.apexcode.drivermetrics.metrics.ProfitabilityEvaluator]
 * as "not configured" and ignored, matching "якщо числа не вказані — не беруться до уваги".
 */
@Serializable
data class CriterionThreshold(
    val redBelow: Double? = null,
    val greenAtOrAbove: Double? = null,
)
