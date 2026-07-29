package com.apexcode.drivermetrics.core.model

/**
 * All fields default to 0 so they are opt-in: a driver who hasn't configured their real costs
 * yet still sees metrics based on the raw order price, matching the base formulas from the
 * spec. More fields (tolls, depreciation, deadhead, partner commission, ...) can be added here
 * later without breaking existing callers.
 */
data class CostSettings(
    val fuelCostPerKm: Double = 0.0,
    val commissionPercent: Double = 0.0,
)
