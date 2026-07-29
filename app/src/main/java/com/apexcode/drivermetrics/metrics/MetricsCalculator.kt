package com.apexcode.drivermetrics.metrics

import com.apexcode.drivermetrics.core.model.CostSettings
import com.apexcode.drivermetrics.core.model.OrderMetrics
import com.apexcode.drivermetrics.core.model.ProfitabilityThresholds
import com.apexcode.drivermetrics.core.model.RouteInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.model.settings.CriterionThreshold

/**
 * Pure functions only — no Android/DI dependencies — so this is trivially unit-testable.
 */
object MetricsCalculator {

    /**
     * @param tripRoute route from pickup (Point A) to dropoff (Point B) — the paid ride.
     * @param pickupRoute route from the driver's current location to Point A ("доїзд"); null
     *   when the driver's location isn't known yet, treated as zero deadhead time.
     * @param evaluationCriteria driver-configured criteria (9.3); empty (the default) preserves
     *   the original €/год-only [thresholds] behavior via [ProfitabilityEvaluator]'s fallback.
     * @param filterRules driver-configured hard filters (9.5); empty (the default) means none
     *   can force the result to RED.
     */
    fun compute(
        order: TaxiOrder,
        tripRoute: RouteInfo,
        pickupRoute: RouteInfo? = null,
        costSettings: CostSettings = CostSettings(),
        thresholds: ProfitabilityThresholds = ProfitabilityThresholds(),
        evaluationCriteria: Map<String, CriterionThreshold> = emptyMap(),
        filterRules: Map<String, Double> = emptyMap(),
    ): OrderMetrics {
        val price = order.price.toDouble()
        val effectivePrice = price * (1 - costSettings.commissionPercent / 100.0) -
            costSettings.fuelCostPerKm * tripRoute.distanceKm

        val etaMinutes = pickupRoute?.durationMin ?: 0.0
        val totalMinutes = etaMinutes + tripRoute.durationMin

        // €/km is the price spread over the whole distance actually driven for this order —
        // the deadhead to the client plus the paid trip — not just the paid trip distance.
        val totalDistanceKm = (pickupRoute?.distanceKm ?: 0.0) + tripRoute.distanceKm

        val pricePerKm = safeDivide(effectivePrice, totalDistanceKm)
        val pricePerHour = safeDivide(effectivePrice, totalMinutes / 60.0)

        val context = EvaluationContext(
            order = order,
            effectivePrice = effectivePrice,
            pricePerHour = pricePerHour,
            pricePerKm = pricePerKm,
            pickupDistanceKm = pickupRoute?.distanceKm,
            pickupDurationMin = pickupRoute?.durationMin,
        )

        return OrderMetrics(
            pricePerKm = pricePerKm,
            pricePerHour = pricePerHour,
            etaMinutes = etaMinutes,
            profitability = ProfitabilityEvaluator.evaluate(
                context = context,
                evaluationCriteria = evaluationCriteria,
                filterRules = filterRules,
                fallbackThresholds = thresholds,
            ),
        )
    }

    private fun safeDivide(numerator: Double, denominator: Double): Double =
        if (denominator <= 0.0) 0.0 else numerator / denominator
}
