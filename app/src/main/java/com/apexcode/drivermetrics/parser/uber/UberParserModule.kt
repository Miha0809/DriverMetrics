package com.apexcode.drivermetrics.parser.uber

import com.apexcode.drivermetrics.core.parser.OrderParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface UberParserModule {
    @Binds
    @IntoSet
    fun bindUberParser(impl: UberParser): OrderParser
}
