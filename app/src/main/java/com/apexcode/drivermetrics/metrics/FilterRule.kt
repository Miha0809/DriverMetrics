package com.apexcode.drivermetrics.metrics

/**
 * One pluggable hard gate (9.5): if configured and violated, the whole order is forced to RED
 * regardless of what [EvaluationCriterion]s say. [id] is the stable key a driver's configured
 * limit is stored under in [com.apexcode.drivermetrics.core.model.settings.AggregatorSettings.filterRules].
 * A new rule is a new object added to [StandardFilterRules.ALL] — [ProfitabilityEvaluator] needs
 * no changes.
 */
interface FilterRule {
    val id: String

    /** HIGHER_IS_BETTER -> configured value is a minimum; LOWER_IS_BETTER -> a maximum. */
    val direction: CriterionDirection

    /** Null when this order doesn't carry the data this rule needs; such rules are skipped. */
    fun valueOf(context: EvaluationContext): Double?
}
