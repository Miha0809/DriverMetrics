package com.apexcode.drivermetrics.metrics

/**
 * One pluggable input to the green/yellow/red indicator (9.3). [id] is the stable key a driver's
 * [com.apexcode.drivermetrics.core.model.settings.CriterionThreshold] is stored under, so it must
 * never change once shipped. A new criterion is a new object implementing this interface added to
 * [StandardCriteria.ALL] — [ProfitabilityEvaluator] needs no changes.
 */
interface EvaluationCriterion {
    val id: String
    val direction: CriterionDirection

    /** Null when this order doesn't carry the data this criterion needs (e.g. no rating parsed). */
    fun valueOf(context: EvaluationContext): Double?
}
