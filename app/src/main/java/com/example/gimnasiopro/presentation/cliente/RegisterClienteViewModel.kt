package com.example.gimnasiopro.presentation.cliente

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.repository.ClienteRepository
import com.example.gimnasiopro.domain.model.Cliente
import com.example.gimnasiopro.utils.UserPhotoManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterClienteUiState {
    object Idle    : RegisterClienteUiState()
    object Loading : RegisterClienteUiState()
    object Success : RegisterClienteUiState()
    data class Error(val message: String) : RegisterClienteUiState()
}

class RegisterClienteViewModel(application: Application) : AndroidViewModel(application) {

    private val auth       = FirebaseAuth.getInstance()
    private val repository = ClienteRepository()

    private val _uiState = MutableStateFlow<RegisterClienteUiState>(RegisterClienteUiState.Idle)
    val uiState: StateFlow<RegisterClienteUiState> = _uiState.asStateFlow()

    fun register(
        email: String,
        password: String,
        nombre: String,
        telefonoSinPrefijo: String,
        peso: Double,
        altura: Double,
        edad: Int,
        genero: String,
        objetivo: String,
        nivel: String,
        fotoUri: Uri?
    ) {
        val validationError = validate(email, password, nombre, telefonoSinPrefijo, peso, altura, edad)
        if (validationError != null) {
            _uiState.value = RegisterClienteUiState.Error(validationError)
            return
        }

        _uiState.value = RegisterClienteUiState.Loading
        val telefono = "+34$telefonoSinPrefijo"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user   = result.user
                val userId = user?.uid ?: ""

                fotoUri?.let { uri -> UserPhotoManager.guardarFotoLocal(getApplication(), userId, uri) }
                user?.sendEmailVerification()

                viewModelScope.launch {
                    val cliente = Cliente(
                        userId = userId,
                        email = email,
                        nombre = nombre,
                        telefono = telefono,
                        peso = peso,
                        altura = altura,
                        edad = edad,
                        genero = genero,
                        objetivo = objetivo,
                        nivel = nivel,
                        fotoUrl = fotoUri?.toString() ?: "",
                        trainerId = null,
                        activo = true,
                        emailVerificado = false,
                        fechaRegistro = System.currentTimeMillis()
                    )
                    val res = repository.registerCliente(cliente)
                    _uiState.value = if (res.isSuccess) {
                        RegisterClienteUiState.Success
                    } else {
                        RegisterClienteUiState.Error(res.exceptionOrNull()?.message ?: "Error desconocido")
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = RegisterClienteUiState.Error(
                    when {
                        e.message?.contains("already in use") == true -> "Este email ya está registrado"
                        e.message?.contains("password") == true       -> "Contraseña inválida"
                        else                                           -> "Error: ${e.message}"
                    }
                )
            }
    }

    fun resetState() { _uiState.value = RegisterClienteUiState.Idle }

    private fun validate(
        email: String, password: String, nombre: String,
        telefono: String, peso: Double, altura: Double, edad: Int
    ): String? = when {
        email.isBlank()                                           -> "Email obligatorio"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches()          -> "Email inválido"
        password.isBlank()                                        -> "Contraseña obligatoria"
        password.length < 6                                       -> "Mínimo 6 caracteres"
        nombre.isBlank()                                          -> "Nombre obligatorio"
        telefono.isBlank()                                        -> "Teléfono obligatorio"
        !telefono.matches("^[0-9]{9}$".toRegex())                -> "Teléfono inválido (9 dígitos)"
        peso > 0 && (peso <= 0 || peso > 300)                    -> "Peso inválido (1-300 kg)"
        altura > 0 && (altura <= 0 || altura > 250)              -> "Altura inválida (1-250 cm)"
        edad > 0 && (edad < 16 || edad > 100)                    -> "Edad inválida (16-100 años)"
        else                                                       -> null
    }
}
