package com.apexcode.drivermetrics.core.model

data class OrderMetrics(
    val pricePerKm: Double,
    val pricePerHour: Double,
    val etaMinutes: Double,
    val profitability: ProfitabilityLevel,
)
