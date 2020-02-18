package com.google.samples.apps.sunflower

import android.app.Application
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        WorkManagerTestInitHelper.initializeTestWorkManager(this, Configuration.Builder().build())

    }
}