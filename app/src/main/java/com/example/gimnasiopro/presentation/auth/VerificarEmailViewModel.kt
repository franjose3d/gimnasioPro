package com.example.gimnasiopro.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.data.repository.ClienteRepository
import com.example.gimnasiopro.data.repository.TrainerRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VerificarEmailUiState {
    object Waiting           : VerificarEmailUiState()
    object Checking          : VerificarEmailUiState()
    object Verified          : VerificarEmailUiState()
    object ResendSuccess     : VerificarEmailUiState()
    data class Error(val message: String) : VerificarEmailUiState()
}

class VerificarEmailViewModel : ViewModel() {

    private val auth              = FirebaseAuth.getInstance()
    private val clienteRepository = ClienteRepository()
    private val trainerRepository = TrainerRepository()

    private val _uiState = MutableStateFlow<VerificarEmailUiState>(VerificarEmailUiState.Waiting)
    val uiState: StateFlow<VerificarEmailUiState> = _uiState.asStateFlow()

    val userEmail: String get() = auth.currentUser?.email ?: ""

    init {
        val user = auth.currentUser
        if (user?.isEmailVerified == true) {
            _uiState.value = VerificarEmailUiState.Verified
            actualizarFirestore(user.uid)
        } else {
            startPolling()
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (_uiState.value !is VerificarEmailUiState.Verified) {
                delay(3_000)
                checkVerification()
            }
        }
    }

    fun checkVerification() {
        _uiState.value = VerificarEmailUiState.Checking
        val user = auth.currentUser ?: run {
            _uiState.value = VerificarEmailUiState.Error("Sesión no encontrada")
            return
        }
        user.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    _uiState.value = VerificarEmailUiState.Verified
                    actualizarFirestore(user.uid)
                } else {
                    _uiState.value = VerificarEmailUiState.Waiting
                }
            } else {
                _uiState.value = VerificarEmailUiState.Error(task.exception?.message ?: "Error al verificar")
            }
        }
    }

    fun resendVerification() {
        val user = auth.currentUser ?: return
        user.sendEmailVerification()
            .addOnSuccessListener { _uiState.value = VerificarEmailUiState.ResendSuccess }
            .addOnFailureListener { e -> _uiState.value = VerificarEmailUiState.Error("Error: ${e.message}") }
    }

    fun signOut() { auth.signOut() }

    private fun actualizarFirestore(userId: String) {
        viewModelScope.launch {
            try {
                when (UserHelper.getUserInfo(userId)?.tipo) {
                    "cliente" -> clienteRepository.updateEmailVerified(userId, true)
                    "trainer" -> trainerRepository.updateEmailVerified(userId, true)
                    else      -> {
                        clienteRepository.updateEmailVerified(userId, true)
                        trainerRepository.updateEmailVerified(userId, true)
                    }
                }
            } catch (_: Exception) { /* non-critical */ }
        }
    }
}
