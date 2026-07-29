package com.apexcode.drivermetrics.metrics

import com.apexcode.drivermetrics.core.model.ProfitabilityLevel
import com.apexcode.drivermetrics.core.model.ProfitabilityThresholds
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.model.settings.CriterionThreshold
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ProfitabilityEvaluatorTest {

    private fun context(
        pricePerHour: Double = 20.0,
        pricePerKm: Double = 2.0,
        passengerRating: Double? = null,
        pickupDistanceKm: Double? = null,
        pickupDurationMin: Double? = null,
    ) = EvaluationContext(
        order = TaxiOrder(
            pickupAddress = "A",
            pickupLatLng = null,
            dropoffAddress = "B",
            dropoffLatLng = null,
            price = BigDecimal("20"),
            currency = "EUR",
            sourceApplication = "test",
            timestamp = Instant.EPOCH,
            passengerRating = passengerRating,
        ),
        effectivePrice = 20.0,
        pricePerHour = pricePerHour,
        pricePerKm = pricePerKm,
        pickupDistanceKm = pickupDistanceKm,
        pickupDurationMin = pickupDurationMin,
    )

    @Test
    fun `falls back to legacy price-per-hour thresholds when no criteria are configured`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 5.0),
            evaluationCriteria = emptyMap(),
            filterRules = emptyMap(),
            fallbackThresholds = ProfitabilityThresholds(greenAtOrAbovePricePerHour = 20.0, redBelowPricePerHour = 10.0),
        )
        assertEquals(ProfitabilityLevel.RED, level)
    }

    @Test
    fun `unconfigured criterion (both bounds null) is ignored`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 1.0),
            evaluationCriteria = mapOf(StandardCriteria.PRICE_PER_HOUR to CriterionThreshold()),
            filterRules = emptyMap(),
        )
        // Falls back to default ProfitabilityThresholds (green >= 15, red < 8) since the
        // configured-but-empty entry doesn't count as configured.
        assertEquals(ProfitabilityLevel.RED, level)
    }

    @Test
    fun `criterion with only a red floor never reaches green`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 1000.0),
            evaluationCriteria = mapOf(StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(redBelow = 10.0)),
            filterRules = emptyMap(),
        )
        assertEquals(ProfitabilityLevel.YELLOW, level)
    }

    @Test
    fun `worst of several configured criteria wins`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 100.0, pricePerKm = 0.1),
            evaluationCriteria = mapOf(
                StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(greenAtOrAbove = 20.0),
                StandardCriteria.PRICE_PER_KM to CriterionThreshold(redBelow = 1.0),
            ),
            filterRules = emptyMap(),
        )
        assertEquals(ProfitabilityLevel.RED, level)
    }

    @Test
    fun `criterion missing its data (no rating parsed) is skipped`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 100.0, passengerRating = null),
            evaluationCriteria = mapOf(
                StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(greenAtOrAbove = 20.0),
                StandardCriteria.PASSENGER_RATING to CriterionThreshold(redBelow = 4.5),
            ),
            filterRules = emptyMap(),
        )
        assertEquals(ProfitabilityLevel.GREEN, level)
    }

    @Test
    fun `a violated filter forces red even when criteria say green`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 100.0, pickupDistanceKm = 12.0),
            evaluationCriteria = mapOf(StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(greenAtOrAbove = 20.0)),
            filterRules = mapOf(StandardFilterRules.MAX_DISTANCE_TO_CLIENT_KM to 5.0),
        )
        assertEquals(ProfitabilityLevel.RED, level)
    }

    @Test
    fun `an unviolated filter does not affect the result`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 100.0, pickupDistanceKm = 2.0),
            evaluationCriteria = mapOf(StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(greenAtOrAbove = 20.0)),
            filterRules = mapOf(StandardFilterRules.MAX_DISTANCE_TO_CLIENT_KM to 5.0),
        )
        assertEquals(ProfitabilityLevel.GREEN, level)
    }

    @Test
    fun `filter missing its data is skipped rather than treated as violated`() {
        val level = ProfitabilityEvaluator.evaluate(
            context = context(pricePerHour = 100.0, pickupDistanceKm = null),
            evaluationCriteria = mapOf(StandardCriteria.PRICE_PER_HOUR to CriterionThreshold(greenAtOrAbove = 20.0)),
            filterRules = mapOf(StandardFilterRules.MAX_DISTANCE_TO_CLIENT_KM to 5.0),
        )
        assertEquals(ProfitabilityLevel.GREEN, level)
    }
}
