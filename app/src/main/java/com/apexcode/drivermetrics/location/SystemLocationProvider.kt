package com.apexcode.drivermetrics.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.apexcode.drivermetrics.core.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Plain LocationManager (no Play Services dependency) — used only for the "доїзд" (pickup
 * deadhead) leg of the ETA. Returns null rather than requesting a fresh fix, since the pickup
 * ETA only needs to be roughly right and must not add latency to the ~1s analysis budget.
 */
class SystemLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : CurrentLocationProvider {

    override fun getLastKnownLocation(): LatLng? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        return locationManager.allProviders
            .mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
            ?.let { LatLng(it.latitude, it.longitude) }
    }
}
