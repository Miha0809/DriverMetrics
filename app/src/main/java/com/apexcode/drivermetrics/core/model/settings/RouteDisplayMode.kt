package com.apexcode.drivermetrics.core.model.settings

import kotlinx.serialization.Serializable

/** Which legs of the order the mini-map draws. See 9.2 in the settings spec. */
@Serializable
enum class RouteDisplayMode {
    /** Client's pickup point -> dropoff only. The original, still-default behavior. */
    CLIENT_TO_DROPOFF_ONLY,

    /** Driver's current location -> pickup, plus pickup -> dropoff. */
    DRIVER_TO_PICKUP_AND_DROPOFF,
}
