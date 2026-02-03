package com.albertomedina.apark.di

import com.albertomedina.apark.data.location.IosLocationSource
import com.albertomedina.apark.data.location.LocationSource
import org.koin.dsl.module

val iosModule = module {

    single<LocationSource> { IosLocationSource() }
}