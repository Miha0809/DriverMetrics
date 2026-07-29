package com.apexcode.drivermetrics.metrics

import com.apexcode.drivermetrics.core.model.ProfitabilityLevel
import com.apexcode.drivermetrics.core.model.ProfitabilityThresholds
import com.apexcode.drivermetrics.core.model.settings.CriterionThreshold

/**
 * Combines the driver's evaluation criteria (9.3) and filter rules (9.5) into the single
 * green/yellow/red indicator. Pure and DI-free like [MetricsCalculator], so it's unit-testable
 * without Android.
 */
object ProfitabilityEvaluator {

    /**
     * @param evaluationCriteria driver-configured bounds, keyed by [EvaluationCriterion.id]; a
     *   criterion missing from this map, or with both bounds null, is ignored entirely.
     * @param filterRules driver-configured hard limits, keyed by [FilterRule.id]; ignored the
     *   same way when absent. Any configured, violated rule forces RED outright.
     * @param fallbackThresholds used only when no evaluation criteria are configured at all, to
     *   preserve the original €/год-only behavior for drivers who haven't opened settings yet.
     */
    fun evaluate(
        context: EvaluationContext,
        evaluationCriteria: Map<String, CriterionThreshold>,
        filterRules: Map<String, Double>,
        fallbackThresholds: ProfitabilityThresholds = ProfitabilityThresholds(),
        criteria: List<EvaluationCriterion> = StandardCriteria.ALL,
        rules: List<FilterRule> = StandardFilterRules.ALL,
    ): ProfitabilityLevel {
        val configuredLevels = criteria.mapNotNull { criterion ->
            val threshold = evaluationCriteria[criterion.id] ?: return@mapNotNull null
            if (threshold.redBelow == null && threshold.greenAtOrAbove == null) return@mapNotNull null
            val value = criterion.valueOf(context) ?: return@mapNotNull null
            classify(value, threshold, criterion.direction)
        }

        // Worst configured criterion wins (RED > YELLOW > GREEN, matching enum declaration order).
        val baseLevel = configuredLevels.maxByOrNull { it.ordinal }
            ?: fallbackThresholds.classify(context.pricePerHour)

        val anyFilterViolated = rules.any { rule ->
            val limit = filterRules[rule.id] ?: return@any false
            val value = rule.valueOf(context) ?: return@any false
            when (rule.direction) {
                CriterionDirection.HIGHER_IS_BETTER -> value < limit
                CriterionDirection.LOWER_IS_BETTER -> value > limit
            }
        }

        return if (anyFilterViolated) ProfitabilityLevel.RED else baseLevel
    }

    private fun classify(
        value: Double,
        threshold: CriterionThreshold,
        direction: CriterionDirection,
    ): ProfitabilityLevel {
        val isRed = threshold.redBelow?.let { bound ->
            when (direction) {
                CriterionDirection.HIGHER_IS_BETTER -> value < bound
                CriterionDirection.LOWER_IS_BETTER -> value > bound
            }
        } ?: false
        if (isRed) return ProfitabilityLevel.RED

        val isGreen = threshold.greenAtOrAbove?.let { bound ->
            when (direction) {
                CriterionDirection.HIGHER_IS_BETTER -> value >= bound
                CriterionDirection.LOWER_IS_BETTER -> value <= bound
            }
        } ?: false
        return if (isGreen) ProfitabilityLevel.GREEN else ProfitabilityLevel.YELLOW
    }
}
