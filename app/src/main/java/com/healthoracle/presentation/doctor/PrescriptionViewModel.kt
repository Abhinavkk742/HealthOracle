package com.healthoracle.presentation.doctor

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.model.Prescription
import com.healthoracle.data.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrescriptionViewModel @Inject constructor(
    private val repository: PrescriptionRepository
) : ViewModel() {

    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    fun loadPrescriptions(patientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPatientPrescriptions(patientId).collect { list ->
                    _prescriptions.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                // ✅ FIX: Safely catch any unexpected errors so the app never closes
                Log.e("PrescriptionVM", "Error loading prescriptions", e)
                _isLoading.value = false
            }
        }
    }

    fun uploadPrescription(patientId: String, doctorId: String, imageUri: Uri, notes: String) {
        viewModelScope.launch {
            _uploadStatus.value = "Uploading Image..."
            val imageUrl = repository.uploadPrescriptionImage(imageUri)

            if (imageUrl != null) {
                _uploadStatus.value = "Saving Record..."
                val success = repository.savePrescription(patientId, doctorId, imageUrl, notes)
                if (success) {
                    _uploadStatus.value = "Success"
                } else {
                    _uploadStatus.value = "Error saving record"
                }
            } else {
                _uploadStatus.value = "Error uploading image"
            }
        }
    }

    fun clearUploadStatus() {
        _uploadStatus.value = null
    }
}