package com.healthoracle

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HealthOracleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // NEW: Wrapped in a try-catch to prevent crash on app reload
            val config = mapOf(
                "cloud_name" to "dpj8tzdte" // <-- PASTE YOUR CLOUD NAME HERE
            )
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // MediaManager is already initialized, safely ignore this error!
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}