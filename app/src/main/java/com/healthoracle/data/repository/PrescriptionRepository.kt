package com.healthoracle.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.data.model.Prescription
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class PrescriptionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    // ✅ This already uploads the photo straight to Cloudinary
    suspend fun uploadPrescriptionImage(imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val tempFile = File(context.cacheDir, "prescription_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", "krfgajle")
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/dpj8tzdte/image/upload")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                tempFile.delete()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    return@withContext jsonObject.getString("secure_url")
                }
            } catch (e: Exception) {
                Log.e("PrescriptionRepo", "Upload failed", e)
            }
            return@withContext null
        }
    }

    suspend fun savePrescription(patientId: String, doctorId: String, imageUrl: String, notes: String): Boolean {
        return try {
            val prescriptionId = UUID.randomUUID().toString()
            val prescription = Prescription(
                id = prescriptionId,
                patientId = patientId,
                doctorId = doctorId,
                imageUrl = imageUrl,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )

            firestore.collection("prescriptions")
                .document(prescriptionId)
                .set(prescription)
                .await()
            true
        } catch (e: Exception) {
            Log.e("PrescriptionRepo", "Failed to save to Firestore", e)
            false
        }
    }

    fun getPatientPrescriptions(patientId: String): Flow<List<Prescription>> = callbackFlow {
        val listener = firestore.collection("prescriptions")
            .whereEqualTo("patientId", patientId)
            // ✅ FIX: Removed the Firebase .orderBy() that was causing the crash
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PrescriptionRepo", "Listen failed", error)
                    trySend(emptyList()) // Send an empty list instead of crashing
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val prescriptions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Prescription::class.java)
                    }
                    // ✅ FIX: Sort the photos by time locally on the device instead
                    val sortedList = prescriptions.sortedByDescending { it.timestamp }
                    trySend(sortedList)
                }
            }

        awaitClose { listener.remove() }
    }
}