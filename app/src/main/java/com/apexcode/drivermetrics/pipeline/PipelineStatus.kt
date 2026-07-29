package com.apexcode.drivermetrics.pipeline

import com.apexcode.drivermetrics.core.model.TaxiOrder
import java.time.Instant

data class PipelineStatus(
    val lastEventPackage: String? = null,
    val lastEventAt: Instant? = null,
    val lastOrder: TaxiOrder? = null,
    val lastOrderAt: Instant? = null,
)
