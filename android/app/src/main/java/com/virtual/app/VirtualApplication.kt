package com.virtual.app

import android.app.Application
import android.util.Log

class VirtualApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("VIRTUAL", "VirtualApplication started. Version: 1.0.0")
    }
}
