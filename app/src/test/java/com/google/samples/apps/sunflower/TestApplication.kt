package com.google.samples.apps.sunflower

import android.app.Application
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.samples.apps.sunflower.data.AppDatabase
import com.google.samples.apps.sunflower.data.GardenPlantingRepository
import com.google.samples.apps.sunflower.data.PlantRepository
import org.robolectric.shadows.ShadowLog

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // uncomment below to print logcat
        // ShadowLog.stream = System.out
        WorkManagerTestInitHelper.initializeTestWorkManager(this)

        // We must get AppDatabase instance again
        // because Robolectric creates database files whenever test is started.
        val db = AppDatabase.getInstance(this)
        // We must update any references of old DAO,
        // which is recreated whenever AppDatabase is recreated.
        GardenPlantingRepository.updateDao(db.gardenPlantingDao())
        PlantRepository.updateDao(db.plantDao())
    }
}