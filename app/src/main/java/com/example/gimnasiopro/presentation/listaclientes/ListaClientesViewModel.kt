package com.example.gimnasiopro.presentation.listaclientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.ConexionTrainerCliente
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ClienteConConexion(
    val cliente: UserHelper.UserInfo,
    val conexion: ConexionTrainerCliente
)

sealed class ListaClientesUiState {
    object Loading : ListaClientesUiState()
    data class Success(val clientes: List<ClienteConConexion>) : ListaClientesUiState()
    data class Error(val message: String) : ListaClientesUiState()
}

class ListaClientesViewModel : ViewModel() {

    private val auth          = FirebaseAuth.getInstance()
    private val conexionRepo  = ConexionRepository()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow<ListaClientesUiState>(ListaClientesUiState.Loading)
    val state: StateFlow<ListaClientesUiState> = _state.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init { loadClientes() }

    fun loadClientes() {
        _state.value = ListaClientesUiState.Loading
        viewModelScope.launch {
            try {
                val conexiones = conexionRepo.getClientesConectados(currentUserId).first()
                val lista = conexiones.mapNotNull { conexion ->
                    UserHelper.getUserInfo(conexion.clienteId)?.let { info ->
                        ClienteConConexion(info, conexion)
                    }
                }
                _state.value = ListaClientesUiState.Success(lista)
            } catch (e: Exception) {
                _state.value = ListaClientesUiState.Error("Error al cargar clientes: ${e.message}")
            }
        }
    }

    fun finalizarConexion(item: ClienteConConexion) {
        val conexionId = item.conexion.id ?: return
        viewModelScope.launch {
            try {
                val result = conexionRepo.finalizarConexion(conexionId)
                if (result.isSuccess) {
                    _toast.value = "Conexión finalizada"
                    loadClientes()
                } else {
                    _toast.value = "Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _toast.value = "Error: ${e.message}"
            }
        }
    }

    fun clearToast() = _toast.update { null }
}
