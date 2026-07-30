package com.apexcode.drivermetrics.di

import com.apexcode.drivermetrics.geocoding.NominatimApi
import com.apexcode.drivermetrics.routing.OsrmApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Nominatim's usage policy asks clients to identify themselves; customize this if you plan
    // on real/sustained usage. See https://operations.osmfoundation.org/policies/nominatim/
    private const val USER_AGENT = "DriverMetrics/1.0 (personal driver-assist app)"

    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

    // Public demo server — best-effort, not for production load. Self-host for real usage.
    private const val OSRM_BASE_URL = "https://router.project-osrm.org/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // Both Nominatim and OSRM here are public best-effort servers (see their base URL
        // comments below) — OkHttp's 10s default per-call timeout meant a slow/hung response
        // could leave the mini-map "loading" far longer than a best-effort server is worth
        // waiting for. Failing fast just falls back to the no-map placeholder.
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideNominatimApi(client: OkHttpClient, json: Json): NominatimApi =
        Retrofit.Builder()
            .baseUrl(NOMINATIM_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NominatimApi::class.java)

    @Provides
    @Singleton
    fun provideOsrmApi(client: OkHttpClient, json: Json): OsrmApi =
        Retrofit.Builder()
            .baseUrl(OSRM_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsrmApi::class.java)
}
