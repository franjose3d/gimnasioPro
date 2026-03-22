package com.example.gimnasiopro.presentation.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.MensajesSyncService
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.presentation.NotificacionLocalService
import com.example.gimnasiopro.utils.NotificationBadgeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class MainUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userId: String = "",
    val racha: Int = 0,
    val notifCount: Int = 0,
    val estaLogueado: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth          = FirebaseAuth.getInstance()
    private val notifRepo     = NotificacionRepository()
    private val app           = application as GimnasioproApplication
    private val db            = GymDatabase.getDatabase(application)
    private val mensajeRepo   = MensajeRepository(db.mensajeDao())

    private var mensajesSync: MensajesSyncService? = null
    private var rutinasSincronizadas = false

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _state.update { it.copy(isLoading = false, estaLogueado = false, userName = "", userId = "") }
            return
        }

        _state.update { it.copy(isLoading = true, estaLogueado = true, userId = currentUser.uid) }

        viewModelScope.launch {
            // 1. Load user name
            try {
                val info = UserHelper.getUserInfo(currentUser.uid)
                val nombre = info?.nombre?.uppercase() ?: currentUser.email?.substringBefore("@")?.uppercase() ?: "USUARIO"
                _state.update { it.copy(userName = nombre) }
            } catch (e: Exception) {
                Log.w(TAG, "Error cargando nombre: ${e.message}")
            }

            // 2. Load racha
            try {
                val racha = app.estadisticaRepository.getRachaActual()
                _state.update { it.copy(racha = racha) }
            } catch (e: Exception) {
                Log.w(TAG, "Error cargando racha: ${e.message}")
            }

            _state.update { it.copy(isLoading = false) }

            // 3. Observe notifications
            observarNotificaciones(currentUser.uid)

            // 4. Sync rutinas (throttled)
            sincronizarRutinas()

            // 5. Start message sync service
            iniciarSyncMensajes()

            // 6. Clean up expired conversations (older than 7 days)
            mensajeRepo.eliminarConversacionesExpiradas()

            // 7. Save FCM token
            guardarFCMToken(currentUser.uid)
        }
    }

    private fun observarNotificaciones(userId: String) {
        var primeraVez = true
        var notifAnteriores = emptyList<com.example.gimnasiopro.data.firestore.Notificacion>()

        viewModelScope.launch {
            try {
                notifRepo.getNotificacionesFlow(userId).collect { notifs ->
                    val count = notifs.count { !it.leida }
                    _state.update { it.copy(notifCount = count) }
                    NotificationBadgeManager.sincronizarConFirestore(getApplication(), count)

                    if (!primeraVez) {
                        val nuevas = notifs.filter { n ->
                            !n.leida && notifAnteriores.none { it.id == n.id }
                        }
                        nuevas.forEach { n ->
                            NotificacionLocalService.mostrarNotificacion(getApplication(), n)
                        }
                    }
                    notifAnteriores = notifs
                    primeraVez = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observando notificaciones: ${e.message}")
            }
        }
    }

    private suspend fun sincronizarRutinas() {
        if (rutinasSincronizadas) return
        try {
            val prefs = getApplication<Application>().getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
            val ultimaSync = prefs.getLong("ultima_sync_rutinas", 0L)
            val ahora = System.currentTimeMillis()
            if (ahora - ultimaSync > 50 * 60 * 1000L) {
                app.rutinaRepositoryHibrido.sincronizarRutinasConFirebase().onSuccess { n ->
                    prefs.edit().putLong("ultima_sync_rutinas", ahora).apply()
                    Log.d(TAG, "✅ $n rutinas sincronizadas")
                }.onFailure { e -> Log.w(TAG, "⚠️ Sync rutinas: ${e.message}") }
            }
            rutinasSincronizadas = true
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando rutinas: ${e.message}")
        }
    }

    private fun iniciarSyncMensajes() {
        if (mensajesSync == null) {
            mensajesSync = MensajesSyncService(mensajeRepo)
            mensajesSync?.iniciarEscucha()
            Log.d(TAG, "✅ MensajesSyncService iniciado")
        }
    }

    private fun guardarFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .set(hashMapOf("fcmToken" to token), SetOptions.merge())
                    .addOnFailureListener { e -> Log.e(TAG, "Error guardando FCM: ${e.message}") }
            }
        }
    }

    fun cerrarSesion() {
        mensajesSync?.detenerEscucha()
        mensajesSync = null
        rutinasSincronizadas = false
        auth.signOut()
        NotificationBadgeManager.limpiarTodo(getApplication())
        _state.update { MainUiState(isLoading = false, estaLogueado = false) }
    }

    override fun onCleared() {
        super.onCleared()
        mensajesSync?.detenerEscucha()
    }

    companion object { private const val TAG = "MainViewModel" }
}
