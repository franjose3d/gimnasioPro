package com.example.gimnasiopro.presentation.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.repository.TrainerRepository
import com.example.gimnasiopro.domain.model.Trainer
import kotlinx.coroutines.launch

/**
 * Estados de la UI para el registro de trainers
 */
sealed class RegisterTrainerUiState {
    object Idle : RegisterTrainerUiState()
    object Loading : RegisterTrainerUiState()
    object Success : RegisterTrainerUiState()
    data class Error(val message: String) : RegisterTrainerUiState()
}

/**
 * ViewModel SIMPLIFICADO para registro de trainers.
 *
 * Habla directo con el Repository, sin UseCase intermedio.
 */
class RegisterTrainerViewModel(
    private val repository: TrainerRepository = TrainerRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<RegisterTrainerUiState>(RegisterTrainerUiState.Idle)
    val uiState: LiveData<RegisterTrainerUiState> = _uiState

    /**
     * Registrar trainer
     *
     * Todas las validaciones ya se hicieron en la Activity,
     * aquí solo guardamos en Firestore.
     */
    fun registerTrainer(
        userId: String,
        email: String,
        nombre: String,
        dni: String,
        poblacion: String,
        municipio: String,
        telefono: String,
        sobreMi: String,
        fotoUrl: String,
        certificadoUrl: String,
        tarifa: Double,
        emailVerificado: Boolean = false
    ) {
        _uiState.value = RegisterTrainerUiState.Loading

        viewModelScope.launch {
            try {
                val trainer = Trainer(
                    userId = userId,
                    email = email,
                    nombre = nombre,
                    dni = dni,
                    poblacion = poblacion,
                    municipio = municipio,
                    telefono = telefono,
                    sobreMi = sobreMi,
                    fotoUrl = fotoUrl,
                    certificadoUrl = certificadoUrl,
                    tarifa = tarifa,
                    verificado = false,  // Siempre false, admin debe aprobar
                    emailVerificado = emailVerificado,
                    numeroClientes = 0,
                    fechaRegistro = System.currentTimeMillis()
                )

                val result = repository.registerTrainer(trainer)

                result.fold(
                    onSuccess = {
                        _uiState.value = RegisterTrainerUiState.Success
                    },
                    onFailure = { error ->
                        _uiState.value = RegisterTrainerUiState.Error(
                            error.message ?: "Error desconocido"
                        )
                    }
                )

            } catch (e: Exception) {
                _uiState.value = RegisterTrainerUiState.Error(
                    e.message ?: "Error al registrar"
                )
            }
        }
    }

    /**
     * Resetear el estado a Idle
     */
    fun resetState() {
        _uiState.value = RegisterTrainerUiState.Idle
    }
}