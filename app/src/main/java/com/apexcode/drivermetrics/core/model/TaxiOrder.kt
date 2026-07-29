package com.apexcode.drivermetrics.core.model

import java.math.BigDecimal
import java.time.Instant

data class TaxiOrder(
    val pickupAddress: String?,
    val pickupLatLng: LatLng?,
    val dropoffAddress: String?,
    val dropoffLatLng: LatLng?,
    val price: BigDecimal,
    val currency: String,
    val sourceApplication: String,
    val timestamp: Instant,
    /** "Доїзд" leg (driver's current location -> pickup), read directly off the order screen.
     *  The aggregator's own ETA can be optimistic (Bolt in particular), so OrderOrchestrator
     *  treats this only as an instant provisional value shown while the real OSRM route is being
     *  computed, and as a last-resort fallback if that computation fails. Null when the parser
     *  couldn't find it. */
    val pickupEta: RouteLeg? = null,
    /** Pickup -> dropoff leg, read directly off the order screen — same caveat as [pickupEta]. */
    val trip: RouteLeg? = null,
    /** Passenger's star rating, read directly off the order screen. Null when the parser
     *  couldn't find it — the passenger-rating evaluation criterion is simply skipped then. */
    val passengerRating: Double? = null,
)
