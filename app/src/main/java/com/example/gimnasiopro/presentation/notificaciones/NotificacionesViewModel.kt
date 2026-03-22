package com.example.gimnasiopro.presentation.notificaciones

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.Notificacion
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.presentation.NotificacionLocalService
import com.example.gimnasiopro.utils.NotificationBadgeManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

data class NotificacionesUiState(
    val notificaciones: List<Notificacion> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val toast: String? = null
)

class NotificacionesViewModel(application: Application) : AndroidViewModel(application) {

    private val auth                 = FirebaseAuth.getInstance()
    private val notifRepo            = NotificacionRepository()
    private val conexionRepo         = ConexionRepository()
    private val currentUserId        = auth.currentUser?.uid ?: ""

    private var currentUserTipo: String? = null
    private var currentUserNombre: String? = null

    private val _state = MutableStateFlow(NotificacionesUiState())
    val state: StateFlow<NotificacionesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadUserInfo()
            observeNotificaciones()
        }
    }

    private suspend fun loadUserInfo() {
        try {
            val info = UserHelper.getUserInfo(currentUserId)
            currentUserTipo   = info?.tipo
            currentUserNombre = info?.nombre
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando user info: ${e.message}")
        }
    }

    private suspend fun observeNotificaciones() {
        if (currentUserId.isBlank()) {
            _state.update { it.copy(isLoading = false, error = "Sin sesión activa") }
            return
        }
        try {
            notifRepo.getNotificacionesFlow(currentUserId).collectLatest { list ->
                val ahora = Date()
                val vigentes  = list.filter { it.fechaExpiracion?.before(ahora) != true }
                val expiradas = list.filter { it.fechaExpiracion?.before(ahora) == true }
                if (expiradas.isNotEmpty()) {
                    viewModelScope.launch { expiradas.forEach { notifRepo.eliminarNotificacion(it.id) } }
                }
                _state.update { it.copy(notificaciones = vigentes, isLoading = false) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Error al cargar notificaciones") }
        }
    }

    fun marcarComoLeida(notificacion: Notificacion) {
        val esSolicitudPendiente = !notificacion.leida &&
            (notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
             notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER)
        if (!esSolicitudPendiente) {
            viewModelScope.launch { notifRepo.marcarComoLeida(notificacion.id) }
        }
    }

    fun aceptar(notificacion: Notificacion) {
        viewModelScope.launch {
            try {
                when (notificacion.tipo) {
                    Notificacion.TIPO_SOLICITUD_CONEXION -> aceptarSolicitudCliente(notificacion)
                    Notificacion.TIPO_INVITACION_TRAINER -> aceptarInvitacionTrainer(notificacion)
                    else -> toast("Tipo no soportado")
                }
            } catch (e: Exception) { toast("Error: ${e.message}") }
        }
    }

    fun rechazar(notificacion: Notificacion) {
        viewModelScope.launch {
            try {
                when (notificacion.tipo) {
                    Notificacion.TIPO_SOLICITUD_CONEXION -> rechazarSolicitudCliente(notificacion)
                    Notificacion.TIPO_INVITACION_TRAINER -> rechazarInvitacionTrainer(notificacion)
                    else -> toast("Tipo no soportado")
                }
            } catch (e: Exception) { toast("Error: ${e.message}") }
        }
    }

    fun eliminar(notificacion: Notificacion) {
        viewModelScope.launch {
            try {
                val result = notifRepo.eliminarNotificacion(notificacion.id)
                if (result.isSuccess) {
                    NotificacionLocalService.cancelarNotificacion(getApplication(), notificacion.id)
                    if (!notificacion.leida) NotificationBadgeManager.decrementarBadge(getApplication())
                    toast("Notificación eliminada")
                } else toast("Error al eliminar")
            } catch (e: Exception) { toast("Error: ${e.message}") }
        }
    }

    fun marcarTodasLeidas() {
        viewModelScope.launch {
            try {
                notifRepo.marcarTodasComoLeidas(currentUserId)
                val db = GymDatabase.getDatabase(getApplication())
                MensajeRepository(db.mensajeDao()).marcarTodosLosMensajesComoLeidos()
                NotificacionLocalService.cancelarTodas(getApplication())
                NotificationBadgeManager.limpiarBadge(getApplication())
                toast("Todas marcadas como leídas")
            } catch (e: Exception) { toast("Error: ${e.message}") }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }

    // ── Private connection helpers ───────────────────────────────────────────

    private suspend fun aceptarSolicitudCliente(n: Notificacion) {
        val clienteId = n.datosExtra["clienteId"]?.takeIf { it.isNotEmpty() } ?: n.remitenteId
        val conexionId = n.datosExtra["conexionId"]?.takeIf { it.isNotEmpty() }
            ?: conexionRepo.getSolicitudesPendientes(currentUserId).first()
                .find { it.clienteId == clienteId || it.clienteId == n.remitenteId }?.id

        if (conexionId == null) { toast("Solicitud no encontrada o expirada"); return }
        val result = conexionRepo.aceptarSolicitud(conexionId)
        if (result.isSuccess) {
            notifRepo.notificarConexionAceptada(n.remitenteId, currentUserId, currentUserNombre ?: "Usuario", currentUserTipo ?: "trainer")
            notifRepo.marcarComoProcesada(n.id)
            toast("¡Conexión aceptada!")
        } else toast("Error al aceptar: ${result.exceptionOrNull()?.message}")
    }

    private suspend fun aceptarInvitacionTrainer(n: Notificacion) {
        val trainerId      = n.datosExtra["trainerId"] ?: n.remitenteId
        val conexionDirect = n.datosExtra["conexionId"]?.takeIf { it.isNotEmpty() }
        val conexionId = when {
            conexionDirect != null -> conexionDirect
            else -> conexionRepo.getConexionPendienteEntreUsuarios(currentUserId, trainerId)?.id
        }
        val result = if (conexionId != null) {
            conexionRepo.aceptarSolicitud(conexionId)
        } else {
            conexionRepo.crearYAceptarConexion(currentUserId, trainerId)
        }
        if (result.isSuccess) {
            notifRepo.notificarConexionAceptada(trainerId, currentUserId, currentUserNombre ?: "Usuario", "cliente")
            notifRepo.marcarComoProcesada(n.id)
            toast("¡Invitación aceptada!")
        } else toast("Error: ${result.exceptionOrNull()?.message}")
    }

    private suspend fun rechazarSolicitudCliente(n: Notificacion) {
        val clienteId  = n.datosExtra["clienteId"]?.takeIf { it.isNotEmpty() } ?: n.remitenteId
        val conexionId = n.datosExtra["conexionId"]?.takeIf { it.isNotEmpty() }
            ?: conexionRepo.getSolicitudesPendientes(currentUserId).first()
                .find { it.clienteId == clienteId || it.clienteId == n.remitenteId }?.id
        if (conexionId != null) conexionRepo.rechazarSolicitud(conexionId)
        notifRepo.notificarConexionRechazada(clienteId, currentUserId, currentUserNombre ?: "Usuario", currentUserTipo ?: "trainer")
        notifRepo.marcarComoProcesada(n.id)
        toast("Solicitud rechazada")
    }

    private suspend fun rechazarInvitacionTrainer(n: Notificacion) {
        val trainerId = n.datosExtra["trainerId"] ?: n.remitenteId
        val conexion  = conexionRepo.getConexionPendienteEntreUsuarios(currentUserId, trainerId)
        if (conexion != null) conexionRepo.rechazarSolicitud(conexion.id)
        notifRepo.notificarConexionRechazada(trainerId, currentUserId, currentUserNombre ?: "Usuario", "cliente")
        notifRepo.marcarComoProcesada(n.id)
        toast("Invitación rechazada")
    }

    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }

    companion object { private const val TAG = "NotificacionesVM" }
}
