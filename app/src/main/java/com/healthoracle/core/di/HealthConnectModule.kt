// ─────────────────────────────────────────────────────────────────────────────
// FILE: app/src/main/java/com/healthoracle/core/di/HealthConnectModule.kt
//
// Provides HealthConnectManager as a singleton via Hilt.
// No changes needed to existing AppModule.kt.
// ─────────────────────────────────────────────────────────────────────────────
package com.healthoracle.core.di

import android.content.Context
import com.healthoracle.data.healthconnect.HealthConnectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    @Provides
    @Singleton
    fun provideHealthConnectManager(
        @ApplicationContext context: Context
    ): HealthConnectManager = HealthConnectManager(context)
}
