package com.apexcode.drivermetrics.core.model

/**
 * Route(s) drawn on the mini-map. By default only pickup (client's location) -> dropoff is shown;
 * the "доїзд" leg (driver's current location -> pickup) is factored into the €/год metric
 * regardless via [TaxiOrder.pickupEta], but only drawn here too when the driver's per-aggregator
 * [com.apexcode.drivermetrics.core.model.settings.RouteDisplayMode] is
 * DRIVER_TO_PICKUP_AND_DROPOFF (9.2), in which case [driverLocation] is set and [geometry] is the
 * doїзд leg followed by the trip leg. See OrderOrchestrator.
 */
data class MapRoute(
    val pickup: LatLng?,
    val dropoff: LatLng?,
    val geometry: List<LatLng> = emptyList(),
    val driverLocation: LatLng? = null,
)
