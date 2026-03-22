package com.example.gimnasiopro.presentation.buscartrainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class BuscarTrainerUiState {
    object Loading : BuscarTrainerUiState()
    data class Success(val trainers: List<UserHelper.UserInfo>) : BuscarTrainerUiState()
    data class Error(val message: String) : BuscarTrainerUiState()
    data class SolicitudEnviada(val trainerNombre: String) : BuscarTrainerUiState()
}

class BuscarTrainerViewModel : ViewModel() {

    private val auth             = FirebaseAuth.getInstance()
    private val conexionRepo     = ConexionRepository()
    private val notifRepo        = NotificacionRepository()
    private val currentUserId    = auth.currentUser?.uid ?: ""

    private var currentUserNombre: String? = null

    private val _state = MutableStateFlow<BuscarTrainerUiState>(BuscarTrainerUiState.Loading)
    val state: StateFlow<BuscarTrainerUiState> = _state.asStateFlow()

    init { loadTrainers() }

    fun loadTrainers() {
        _state.value = BuscarTrainerUiState.Loading
        viewModelScope.launch {
            try {
                val info = UserHelper.getUserInfo(currentUserId)
                currentUserNombre = info?.nombre ?: "Cliente"
                val trainers = UserHelper.getTrainersDisponibles()
                _state.value = BuscarTrainerUiState.Success(trainers)
            } catch (e: Exception) {
                _state.value = BuscarTrainerUiState.Error("Error al cargar trainers: ${e.message}")
            }
        }
    }

    fun enviarSolicitud(trainer: UserHelper.UserInfo, mensaje: String) {
        _state.value = BuscarTrainerUiState.Loading
        viewModelScope.launch {
            try {
                val result = conexionRepo.solicitarConexion(
                    clienteId = currentUserId,
                    trainerId = trainer.userId,
                    mensaje   = mensaje
                )
                if (result.isSuccess) {
                    val conexionId = result.getOrNull() ?: ""
                    notifRepo.enviarSolicitudConexion(
                        trainerId     = trainer.userId,
                        clienteId     = currentUserId,
                        clienteNombre = currentUserNombre ?: "Cliente",
                        mensaje       = mensaje,
                        conexionId    = conexionId
                    )
                    _state.value = BuscarTrainerUiState.SolicitudEnviada(trainer.nombre ?: "Trainer")
                } else {
                    _state.value = BuscarTrainerUiState.Error(
                        result.exceptionOrNull()?.message ?: "No se pudo enviar la solicitud"
                    )
                }
            } catch (e: Exception) {
                _state.value = BuscarTrainerUiState.Error("Error: ${e.message}")
            }
        }
    }
}
