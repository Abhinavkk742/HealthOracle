package com.healthoracle.presentation.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.UserAccount
import com.healthoracle.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DoctorDashboardViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _patients = MutableStateFlow<List<UserAccount>>(emptyList())
    val patients: StateFlow<List<UserAccount>> = _patients.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentDoctorId = Firebase.auth.currentUser?.uid ?: ""

    init {
        fetchPatients()
    }

    private fun fetchPatients() {
        if (currentDoctorId.isEmpty()) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getPatientsForDoctor(currentDoctorId)
                .catch { e ->
                    e.printStackTrace()
                    _isLoading.value = false
                }
                .collect { patientList ->
                    _patients.value = patientList
                    _isLoading.value = false
                }
        }
    }
}