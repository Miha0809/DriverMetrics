package com.apexcode.drivermetrics.di

import com.apexcode.drivermetrics.core.parser.OrderParser
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Guarantees an (possibly empty) Set<OrderParser> is injectable even before any aggregator is
 * bound. Each parser implementation adds its own @Binds @IntoSet in its own module — this file
 * doesn't need to change when a new aggregator is added.
 */
@Module
@InstallIn(SingletonComponent::class)
interface ParserModule {
    @Multibinds
    fun bindOrderParsers(): Set<OrderParser>
}
