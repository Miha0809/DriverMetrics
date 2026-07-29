package com.apexcode.drivermetrics.location

import com.apexcode.drivermetrics.core.model.LatLng

interface CurrentLocationProvider {
    /** Returns null if location permission isn't granted or no provider has a fix yet. */
    fun getLastKnownLocation(): LatLng?
}
