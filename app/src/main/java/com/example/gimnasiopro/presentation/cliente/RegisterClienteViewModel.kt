package com.example.gimnasiopro.presentation.cliente

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.repository.ClienteRepository
import com.example.gimnasiopro.domain.model.Cliente
import kotlinx.coroutines.launch


class RegisterClienteViewModel : ViewModel() {

    private val repository = ClienteRepository()

    private val _uiState = MutableLiveData<RegisterClienteUiState>(RegisterClienteUiState.Idle)
    val uiState: LiveData<RegisterClienteUiState> = _uiState

    /**
     * Registrar nuevo cliente
     */
    fun registerCliente(
        userId: String,
        email: String,
        nombre: String,
        telefono: String,
        peso: Double,
        altura: Double,
        edad: Int,
        genero: String,
        objetivo: String,
        nivel: String,
        fotoUrl: String,
        emailVerificado: Boolean
    ) {
        _uiState.value = RegisterClienteUiState.Loading

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
            fotoUrl = fotoUrl,
            trainerId = null,  // Sin trainer asignado inicialmente
            activo = true,
            emailVerificado = emailVerificado,
            fechaRegistro = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = repository.registerCliente(cliente)

            _uiState.value = if (result.isSuccess) {
                RegisterClienteUiState.Success
            } else {
                RegisterClienteUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterClienteUiState.Idle
    }
}

/**
 * Estados de la UI
 */
sealed class RegisterClienteUiState {
    object Idle : RegisterClienteUiState()
    object Loading : RegisterClienteUiState()
    object Success : RegisterClienteUiState()
    data class Error(val message: String) : RegisterClienteUiState()
}
