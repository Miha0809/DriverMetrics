package com.apexcode.drivermetrics.parser.bolt

import com.apexcode.drivermetrics.core.parser.OrderParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface BoltParserModule {
    @Binds
    @IntoSet
    fun bindBoltParser(impl: BoltParser): OrderParser
}
