package com.albertomedina.apark

import android.app.Application
import com.albertomedina.apark.di.androidModule
import com.albertomedina.apark.di.initKoin
import org.koin.android.ext.koin.androidContext

class AparkApp: Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(){
            androidContext(this@AparkApp)
            modules(androidModule)
        }
    }
}