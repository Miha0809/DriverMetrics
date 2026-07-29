package com.apexcode.drivermetrics.core.model

data class RouteInfo(
    val distanceKm: Double,
    val durationMin: Double,
    val geometry: List<LatLng> = emptyList(),
)
