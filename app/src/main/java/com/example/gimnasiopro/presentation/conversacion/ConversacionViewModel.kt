package com.example.gimnasiopro.presentation.conversacion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.Mensaje
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.data.local.MensajeLocal
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversacionUiState(
    val mensajes: List<MensajeLocal> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val texto: String = "",
    val otroNombre: String = "",
    val currentUserNombre: String = "",
    val toast: String? = null,
    val error: String? = null,
    // null = conversación nueva sin mensajes
    // > 0  = días completos restantes
    // 0    = expira hoy (< 24 h)
    // -1   = expirada y eliminada
    val diasRestantes: Int? = null
)

class ConversacionViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val auth          = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val otroUsuarioId     = savedState.get<String>("OTRO_USUARIO_ID") ?: ""
    private val otroUsuarioNombre = savedState.get<String>("OTRO_USUARIO_NOMBRE") ?: "Conversación"

    private val conversacionId = Mensaje.generarConversacionId(currentUserId, otroUsuarioId)

    private val db               = GymDatabase.getDatabase(application)
    private val mensajeRepository = MensajeRepository(db.mensajeDao())

    private val _state = MutableStateFlow(ConversacionUiState(otroNombre = otroUsuarioNombre))
    val state: StateFlow<ConversacionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadCurrentUserName()
            calcularExpiracion()
            observeMensajes()
        }
        marcarLeidos()
    }

    private suspend fun calcularExpiracion() {
        val primerMensaje = mensajeRepository.getFechaPrimerMensaje(conversacionId) ?: return
        val DAY_MS = 24 * 60 * 60 * 1000L
        val expiresAt = primerMensaje + 7L * DAY_MS
        val msRestantes = expiresAt - System.currentTimeMillis()

        if (msRestantes <= 0) {
            // Expirada: borrar de Room y marcar en estado
            mensajeRepository.eliminarConversacion(conversacionId)
            _state.update { it.copy(diasRestantes = -1, mensajes = emptyList(), isLoading = false) }
        } else {
            // Días completos que quedan (0 = menos de 24 h, ≥1 = días enteros)
            val dias = (msRestantes / DAY_MS).toInt()
            _state.update { it.copy(diasRestantes = dias) }
        }
    }

    private suspend fun loadCurrentUserName() {
        try {
            val info = UserHelper.getUserInfo(currentUserId)
            _state.update { it.copy(currentUserNombre = info?.nombre ?: "Tú") }
        } catch (_: Exception) {}
    }

    private suspend fun observeMensajes() {
        if (currentUserId.isBlank() || otroUsuarioId.isBlank()) {
            _state.update { it.copy(isLoading = false, error = "Datos de conversación inválidos") }
            return
        }
        try {
            mensajeRepository.getMensajesConversacion(conversacionId).collectLatest { mensajes ->
                _state.update { it.copy(mensajes = mensajes, isLoading = false) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Error al cargar mensajes") }
        }
    }

    fun marcarLeidos() {
        if (conversacionId.isBlank()) return
        viewModelScope.launch {
            try { mensajeRepository.marcarComoLeidos(conversacionId) }
            catch (_: Exception) {}
        }
    }

    fun onTextoChange(nuevo: String) {
        if (nuevo.length <= MensajeRepository.MAX_CARACTERES) {
            _state.update { it.copy(texto = nuevo) }
        }
    }

    fun enviarMensaje() {
        val texto = _state.value.texto.trim()
        if (texto.isBlank()) return
        if (_state.value.diasRestantes == -1) { toast("La conversación ha expirado"); return }
        if (otroUsuarioId.isBlank()) { toast("Error: destinatario desconocido"); return }

        _state.update { it.copy(isSending = true) }
        viewModelScope.launch {
            try {
                val result = mensajeRepository.enviarMensaje(
                    destinatarioId  = otroUsuarioId,
                    texto           = texto,
                    conversacionId  = conversacionId,
                    remitenteNombre = _state.value.currentUserNombre
                )
                if (result.isSuccess) {
                    _state.update { it.copy(texto = "", isSending = false) }
                } else {
                    _state.update { it.copy(isSending = false) }
                    toast("Error al enviar: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false) }
                toast("Error: ${e.message}")
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }

    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
