package com.healthoracle.core.di

import android.content.Context
import androidx.room.Room
import com.healthoracle.data.local.AppDatabase
import com.healthoracle.data.local.DiabetesClassifier
import com.healthoracle.data.local.SkinDiseaseClassifier
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.TodoDao
import com.healthoracle.data.local.dao.WalkSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSkinDiseaseClassifier(
        @ApplicationContext context: Context
    ): SkinDiseaseClassifier = SkinDiseaseClassifier(context)

    @Provides
    @Singleton
    fun provideDiabetesClassifier(
        @ApplicationContext context: Context
    ): DiabetesClassifier = DiabetesClassifier(context)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "healthoracle_db"
        )
            .addMigrations(AppDatabase.MIGRATION_2_3)  // ← switched from fallbackToDestructiveMigration
            .build()
    }

    @Provides
    @Singleton
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao {
        return db.appointmentDao()
    }

    @Provides
    @Singleton
    fun provideWalkSessionDao(db: AppDatabase): WalkSessionDao {
        return db.walkSessionDao()
    }

    // ── ADD THIS ──────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideTodoDao(db: AppDatabase): TodoDao {
        return db.todoDao()
    }
    // ─────────────────────────────────────────────────────────────────────────
}