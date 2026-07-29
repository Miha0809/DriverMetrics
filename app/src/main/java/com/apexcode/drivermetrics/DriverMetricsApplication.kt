package com.apexcode.drivermetrics

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class DriverMetricsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid requires an identifying User-Agent and writable cache dirs per OSM tile
        // usage policy; configured once here rather than per MapView.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }
}
