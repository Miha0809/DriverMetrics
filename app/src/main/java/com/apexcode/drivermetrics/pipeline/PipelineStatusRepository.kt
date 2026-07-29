package com.apexcode.drivermetrics.pipeline

import com.apexcode.drivermetrics.core.model.TaxiOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared between the accessibility service (writer) and MainActivity's debug status screen
 * (reader), so the driver can see the pipeline is actually firing without needing adb logcat.
 */
@Singleton
class PipelineStatusRepository @Inject constructor() {
    private val _status = MutableStateFlow(PipelineStatus())
    val status: StateFlow<PipelineStatus> = _status.asStateFlow()

    fun recordEvent(packageName: String) {
        _status.update { it.copy(lastEventPackage = packageName, lastEventAt = Instant.now()) }
    }

    fun recordOrder(order: TaxiOrder?) {
        _status.update { it.copy(lastOrder = order, lastOrderAt = Instant.now()) }
    }
}
