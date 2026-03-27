package com.healthoracle.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.data.local.dao.WalkSessionDao
import com.healthoracle.data.local.entity.WalkSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkRepository @Inject constructor(
    private val walkSessionDao: WalkSessionDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun getAllSessions(): Flow<List<WalkSession>> = walkSessionDao.getAllSessions()

    suspend fun saveSession(session: WalkSession): Long {
        // 1. Save locally
        val localId = walkSessionDao.insert(session)
        val updatedSession = session.copy(id = localId)

        // 2. Save to Firebase
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("walk_history").document(localId.toString())
                    .set(updatedSession)
                    .addOnSuccessListener {
                        Log.d("WalkTracker", "✅ Successfully saved walk to Firebase!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WalkTracker", "❌ Failed to save to Firebase: ${e.message}")
                    }
                    .await()
            } catch (e: Exception) {
                Log.e("WalkTracker", "❌ Exception saving to Firebase: ${e.message}")
            }
        } else {
            Log.e("WalkTracker", "❌ Cannot save to Firebase: User is NOT logged in!")
        }

        return localId
    }

    suspend fun getSessionById(id: Long): WalkSession? = walkSessionDao.getSessionById(id)

    suspend fun deleteSession(session: WalkSession) {
        // 1. Delete locally
        walkSessionDao.delete(session)

        // 2. Delete from Firebase
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("walk_history").document(session.id.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("WalkTracker", "❌ Exception deleting from Firebase: ${e.message}")
            }
        }
    }

    // Fetch from Firebase and save locally
    // Fetch from Firebase and save locally
    suspend fun syncWalksFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        try {
            android.util.Log.d("WalkTracker", "🔄 Fetching walks from Firebase...")
            val snapshot = firestore.collection("users").document(userId)
                .collection("walk_history")
                .get()
                .await()

            android.util.Log.d("WalkTracker", "📥 Found ${snapshot.documents.size} walks in Firebase!")

            val sessionsFromFirebase = snapshot.documents.mapNotNull { doc ->
                try {
                    val session = doc.toObject(WalkSession::class.java)
                    session?.copy(id = doc.id.toLongOrNull() ?: 0L)
                } catch (e: Exception) {
                    android.util.Log.e("WalkTracker", "❌ Error parsing Firebase data: ${e.message}")
                    null
                }
            }

            // CRITICAL FIX: Force local database writes to the Background IO Thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                sessionsFromFirebase.forEach { session ->
                    try {
                        walkSessionDao.insert(session)
                        android.util.Log.d("WalkTracker", "✅ Synced walk ${session.id} locally!")
                    } catch (e: Exception) {
                        android.util.Log.e("WalkTracker", "❌ Error saving synced data locally: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WalkTracker", "❌ Error fetching from Firebase: ${e.message}")
        }
    }
}