package com.example.gimnasiopro.presentation.auth

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.firestore.UserDataMigrationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class EmailNotVerified(val email: String) : LoginUiState()
    object VerificationSent   : LoginUiState()
    object PasswordResetSent  : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Returns true if there is already an active, verified session. */
    fun isAlreadyLoggedIn(): Boolean {
        val user = auth.currentUser
        return user != null && user.isEmailVerified
    }

    fun login(email: String, password: String) {
        if (!validateEmail(email) || password.isBlank()) {
            _uiState.value = LoginUiState.Error(
                when {
                    email.isBlank() -> "Email obligatorio"
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email inválido"
                    else -> "Contraseña obligatoria"
                }
            )
            return
        }

        _uiState.value = LoginUiState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    _uiState.value = LoginUiState.Error("Error al obtener usuario")
                    return@addOnSuccessListener
                }

                if (!user.isEmailVerified) {
                    _uiState.value = LoginUiState.EmailNotVerified(user.email ?: "")
                    return@addOnSuccessListener
                }

                // Migrate local data to Firebase in background then signal success
                viewModelScope.launch {
                    migrarDatosEnBackground()
                    _uiState.value = LoginUiState.Success
                }
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("no user record") == true ||
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                        "Email o contraseña incorrectos"
                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"
                    e.message?.contains("too many requests") == true ->
                        "Demasiados intentos. Espera un momento"
                    else -> "Error: ${e.message}"
                }
                _uiState.value = LoginUiState.Error(msg)
            }
    }

    fun resendVerificationEmail() {
        val user = auth.currentUser ?: run {
            _uiState.value = LoginUiState.Error("No hay usuario activo")
            return
        }
        _uiState.value = LoginUiState.Loading
        user.sendEmailVerification()
            .addOnSuccessListener {
                auth.signOut()
                _uiState.value = LoginUiState.VerificationSent
            }
            .addOnFailureListener { e ->
                _uiState.value = LoginUiState.Error("Error al reenviar: ${e.message}")
            }
    }

    fun sendPasswordReset(email: String) {
        if (!validateEmail(email)) {
            _uiState.value = LoginUiState.Error("Email inválido")
            return
        }
        _uiState.value = LoginUiState.Loading
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _uiState.value = LoginUiState.PasswordResetSent
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("no user record") == true ->
                        "No existe una cuenta con este email"
                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"
                    else -> "Error: ${e.message}"
                }
                _uiState.value = LoginUiState.Error(msg)
            }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private fun validateEmail(email: String) =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private suspend fun migrarDatosEnBackground() {
        try {
            val app = getApplication<GimnasioproApplication>()
            val helper = UserDataMigrationHelper(
                context = app,
                rutinaRepository = app.rutinaRepository,
                registroEntrenamientoRepository = app.registroEntrenamientoRepository,
                rutinaDiaSemanaRepository = app.rutinaDiaSemanaRepository,
                estadisticaRepository = app.estadisticaRepository
            )
            app.rutinaRepositoryHibrido.migrarRutinasAFirebase()
            if (!helper.datosYaMigrados()) {
                helper.migrarDatosUsuario()
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error en migración: ${e.message}")
        }
    }
}
