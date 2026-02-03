package com.albertomedina.apark.di

import com.albertomedina.apark.BuildConfig
import com.albertomedina.apark.data.location.AndroidLocationSource
import com.albertomedina.apark.data.location.LocationSource
import org.koin.dsl.module


val androidModule = module {

    single { AppConfig(isDebug = BuildConfig.DEBUG) }
    single<LocationSource> { AndroidLocationSource (get()) }
}