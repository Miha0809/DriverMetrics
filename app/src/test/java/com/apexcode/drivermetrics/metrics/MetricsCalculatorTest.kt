package com.apexcode.drivermetrics.metrics

import com.apexcode.drivermetrics.core.model.CostSettings
import com.apexcode.drivermetrics.core.model.ProfitabilityLevel
import com.apexcode.drivermetrics.core.model.ProfitabilityThresholds
import com.apexcode.drivermetrics.core.model.RouteInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class MetricsCalculatorTest {

    private fun order(price: String) = TaxiOrder(
        pickupAddress = "A",
        pickupLatLng = null,
        dropoffAddress = "B",
        dropoffLatLng = null,
        price = BigDecimal(price),
        currency = "EUR",
        sourceApplication = "test",
        timestamp = Instant.EPOCH,
    )

    @Test
    fun `price per km divides price by trip distance`() {
        val metrics = MetricsCalculator.compute(
            order = order("10"),
            tripRoute = RouteInfo(distanceKm = 5.0, durationMin = 10.0),
        )
        assertEquals(2.0, metrics.pricePerKm, 0.0001)
    }

    @Test
    fun `price per km sums pickup deadhead distance and trip distance`() {
        val metrics = MetricsCalculator.compute(
            order = order("15"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 20.0),
            pickupRoute = RouteInfo(distanceKm = 5.0, durationMin = 10.0),
        )
        // total distance = 5 (deadhead) + 10 (trip) = 15 km -> 15 / 15 = 1 €/km
        assertEquals(1.0, metrics.pricePerKm, 0.0001)
    }

    @Test
    fun `price per hour includes pickup deadhead time plus trip time`() {
        val metrics = MetricsCalculator.compute(
            order = order("15"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 20.0),
            pickupRoute = RouteInfo(distanceKm = 2.0, durationMin = 10.0),
        )
        // total time = 10 (deadhead) + 20 (trip) = 30 min = 0.5h -> 15 / 0.5 = 30 €/h
        assertEquals(30.0, metrics.pricePerHour, 0.0001)
        assertEquals(10.0, metrics.etaMinutes, 0.0001)
    }

    @Test
    fun `missing pickup route is treated as zero deadhead time`() {
        val metrics = MetricsCalculator.compute(
            order = order("10"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 30.0),
        )
        // total time = 30 min = 0.5h -> 10 / 0.5 = 20 €/h
        assertEquals(20.0, metrics.pricePerHour, 0.0001)
        assertEquals(0.0, metrics.etaMinutes, 0.0001)
    }

    @Test
    fun `zero distance or duration does not throw and yields zero rather than infinity`() {
        val metrics = MetricsCalculator.compute(
            order = order("10"),
            tripRoute = RouteInfo(distanceKm = 0.0, durationMin = 0.0),
        )
        assertEquals(0.0, metrics.pricePerKm, 0.0001)
        assertEquals(0.0, metrics.pricePerHour, 0.0001)
    }

    @Test
    fun `commission and fuel cost reduce the effective price used for both metrics`() {
        val metrics = MetricsCalculator.compute(
            order = order("20"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 30.0),
            costSettings = CostSettings(fuelCostPerKm = 0.5, commissionPercent = 10.0),
        )
        // effective price = 20 * 0.9 - 0.5 * 10 = 18 - 5 = 13
        assertEquals(1.3, metrics.pricePerKm, 0.0001)
        assertEquals(26.0, metrics.pricePerHour, 0.0001)
    }

    @Test
    fun `profitability classifies green, yellow and red using thresholds`() {
        val thresholds = ProfitabilityThresholds(greenAtOrAbovePricePerHour = 20.0, redBelowPricePerHour = 10.0)

        val green = MetricsCalculator.compute(
            order = order("20"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 60.0),
            thresholds = thresholds,
        )
        assertEquals(ProfitabilityLevel.GREEN, green.profitability)

        val yellow = MetricsCalculator.compute(
            order = order("15"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 60.0),
            thresholds = thresholds,
        )
        assertEquals(ProfitabilityLevel.YELLOW, yellow.profitability)

        val red = MetricsCalculator.compute(
            order = order("5"),
            tripRoute = RouteInfo(distanceKm = 10.0, durationMin = 60.0),
            thresholds = thresholds,
        )
        assertEquals(ProfitabilityLevel.RED, red.profitability)
    }
}
