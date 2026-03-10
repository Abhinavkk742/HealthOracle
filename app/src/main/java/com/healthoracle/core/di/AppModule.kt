package com.healthoracle.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.local.AppDatabase
import com.healthoracle.data.local.DiabetesClassifier
import com.healthoracle.data.local.SkinDiseaseClassifier
import com.healthoracle.data.local.dao.AppointmentDao
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
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    // NEW: Room Database Providers
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "healthoracle_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao {
        return db.appointmentDao
    }
}