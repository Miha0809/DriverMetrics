package com.apexcode.drivermetrics.core.model

/** A distance/duration pair as shown directly on an order screen (no routing API involved). */
data class RouteLeg(val distanceKm: Double, val durationMin: Double)
