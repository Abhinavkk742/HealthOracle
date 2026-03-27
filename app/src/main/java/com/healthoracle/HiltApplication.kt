package com.healthoracle

import android.app.Application
import com.cloudinary.android.MediaManager
import com.healthoracle.data.local.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthOracleApp : Application() {

    @Inject
    lateinit var database: AppDatabase

    companion object {
        private var instance: HealthOracleApp? = null

        fun getDatabase(): AppDatabase? = instance?.database
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            val config = mapOf("cloud_name" to "dpj8tzdte")
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // MediaManager already initialized
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}