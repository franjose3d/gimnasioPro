package com.example.gimnasiopro.presentation.contacta

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.Mensaje
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactaUiState(
    val isLoading: Boolean = true,
    val tipoUsuario: String = "",       // "cliente" | "trainer"
    // --- cliente ---
    val tieneTrainer: Boolean = false,
    val tieneSolicitudPendiente: Boolean = false,
    val trainerNombre: String = "",
    val trainerEstado: String = "",
    val trainerId: String = "",         // para abrir chat
    // --- trainer ---
    val numClientes: Int = 0,
    // --- common ---
    val toast: String? = null,
    val error: String? = null
)

class ContactaViewModel(application: Application) : AndroidViewModel(application) {

    private val auth         = FirebaseAuth.getInstance()
    private val conexionRepo = ConexionRepository()
    private val notifRepo    = NotificacionRepository()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(ContactaUiState())
    val state: StateFlow<ContactaUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val info = UserHelper.getUserInfo(currentUserId)
                val tipo = info?.tipo ?: ""
                when (tipo) {
                    "cliente" -> loadClienteData()
                    "trainer" -> loadTrainerData()
                    else      -> _state.update { it.copy(isLoading = false, error = "Tipo de usuario desconocido") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun loadClienteData() {
        try {
            val conexion = conexionRepo.getTrainerConectado(currentUserId).first()
            if (conexion != null) {
                val trainerInfo = UserHelper.getUserInfo(conexion.trainerId)
                _state.update { it.copy(
                    isLoading = false,
                    tipoUsuario = "cliente",
                    tieneTrainer = true,
                    tieneSolicitudPendiente = false,
                    trainerNombre = trainerInfo?.nombre ?: "Tu Trainer",
                    trainerEstado = "Conexión activa",
                    trainerId = conexion.trainerId
                )}
            } else {
                val solicitud = conexionRepo.getSolicitudPendienteDeCliente(currentUserId).first()
                _state.update { it.copy(
                    isLoading = false,
                    tipoUsuario = "cliente",
                    tieneTrainer = false,
                    tieneSolicitudPendiente = solicitud != null,
                    trainerNombre = if (solicitud != null) "Esperando respuesta..." else "",
                    trainerEstado = if (solicitud != null) "Solicitud pendiente" else ""
                )}
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, tipoUsuario = "cliente") }
        }
    }

    private suspend fun loadTrainerData() {
        try {
            val clientes = conexionRepo.getClientesConectados(currentUserId).first()
            _state.update { it.copy(
                isLoading = false,
                tipoUsuario = "trainer",
                numClientes = clientes.size
            )}
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, tipoUsuario = "trainer", numClientes = 0) }
        }
    }

    // ── Trainer action: invite client ─────────────────────────────────────────

    fun enviarInvitacion(emailOTelefono: String) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                var valor = emailOTelefono.trim()
                val soloDigitos = valor.replace(" ", "").replace("-", "")
                val esTelefono = !valor.contains("@") &&
                    soloDigitos.all { it.isDigit() } &&
                    soloDigitos.length in 6..15
                if (esTelefono && !valor.startsWith("+")) valor = "+34$soloDigitos"

                val clienteId = if (esTelefono) UserHelper.getUserIdByPhone(valor)
                                else UserHelper.getUserIdByEmail(valor)

                _state.update { it.copy(isLoading = false) }

                if (clienteId == null) {
                    val tipo = if (esTelefono) "teléfono" else "email"
                    toast("No se encontró ningún cliente con ese $tipo")
                    return@launch
                }
                val userInfo = UserHelper.getUserInfo(clienteId)
                if (userInfo?.tipo != "cliente") {
                    toast("El usuario encontrado no es un cliente")
                    return@launch
                }
                if (clienteId == currentUserId) {
                    toast("No puedes invitarte a ti mismo")
                    return@launch
                }
                // Verificar conexiones existentes
                val yaConectado = conexionRepo.getClientesConectados(currentUserId).first().any { it.clienteId == clienteId }
                if (yaConectado) { toast("Ya tienes conexión con este cliente"); return@launch }
                val pendiente = conexionRepo.getConexionPendienteEntreUsuarios(clienteId, currentUserId)
                if (pendiente != null) { toast("Ya existe una solicitud pendiente con este cliente"); return@launch }

                // Crear conexión y enviar notificación
                val conexionResult = conexionRepo.solicitarConexionComoTrainer(clienteId, currentUserId)
                val conexionId = conexionResult.getOrNull() ?: ""
                val trainerInfo = UserHelper.getUserInfo(currentUserId)
                val resultado = notifRepo.enviarInvitacionTrainer(
                    clienteId     = clienteId,
                    trainerId     = currentUserId,
                    trainerNombre = trainerInfo?.nombre ?: "Trainer",
                    conexionId    = conexionId
                )
                if (resultado.isSuccess) {
                    toast("Invitación enviada a ${userInfo.nombre ?: "cliente"}")
                } else {
                    toast("Error al enviar invitación: ${resultado.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                toast("Error: ${e.message}")
            }
        }
    }

    // ── Cliente action: desvincular trainer ──────────────────────────────────

    fun desvincularTrainer() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val conexion = conexionRepo.getTrainerConectado(currentUserId).first()
                if (conexion == null || conexion.id == null) {
                    _state.update { it.copy(isLoading = false) }
                    toast("No se encontró conexión activa")
                    return@launch
                }
                val result = conexionRepo.finalizarConexion(conexion.id!!)
                if (result.isSuccess) {
                    // Limpiar historial de chat local
                    try {
                        val db = GymDatabase.getDatabase(getApplication())
                        val convId = Mensaje.generarConversacionId(currentUserId, conexion.trainerId)
                        db.mensajeDao().eliminarConversacion(convId)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo limpiar historial: ${e.message}")
                    }
                    toast("Te has desvinculado correctamente")
                    loadClienteData()
                } else {
                    _state.update { it.copy(isLoading = false) }
                    toast("Error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                toast("Error: ${e.message}")
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }

    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }

    companion object { private const val TAG = "ContactaViewModel" }
}
