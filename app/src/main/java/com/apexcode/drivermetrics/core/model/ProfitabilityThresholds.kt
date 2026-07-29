package com.apexcode.drivermetrics.core.model

data class ProfitabilityThresholds(
    val greenAtOrAbovePricePerHour: Double = 15.0,
    val redBelowPricePerHour: Double = 8.0,
) {
    fun classify(pricePerHour: Double): ProfitabilityLevel = when {
        pricePerHour >= greenAtOrAbovePricePerHour -> ProfitabilityLevel.GREEN
        pricePerHour < redBelowPricePerHour -> ProfitabilityLevel.RED
        else -> ProfitabilityLevel.YELLOW
    }
}
