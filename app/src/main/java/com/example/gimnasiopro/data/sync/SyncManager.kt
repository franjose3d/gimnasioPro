package com.example.gimnasiopro.data.sync

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de sincronización para la arquitectura offline-first.
 *
 * Controla cuándo se debe sincronizar con Firebase:
 * - Durante el entrenamiento: SOLO se usa Room (local)
 * - Al finalizar entrenamiento: Se sincroniza con Firebase
 * - En operaciones CRUD de rutinas: Sincronización inmediata
 *
 * La app debe funcionar completamente sin conexión a internet.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    /**
     * Estado de sincronización actual
     */
    enum class SyncState {
        /** Sincronización normal - se sincroniza inmediatamente */
        NORMAL,
        /** En entrenamiento - solo se usa Room, no se sincroniza con Firebase */
        TRAINING_MODE,
        /** Sincronización pendiente - hay cambios locales sin subir */
        PENDING_SYNC
    }

    private val _syncState = MutableStateFlow(SyncState.NORMAL)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * Indica si la sincronización con Firebase está habilitada.
     * Durante el entrenamiento, esto será false.
     * Cuando hay sincronización pendiente (PENDING_SYNC), esto será true para que se sincronice.
     */
    val isSyncEnabled: Boolean
        get() = (_syncState.value == SyncState.NORMAL || _syncState.value == SyncState.PENDING_SYNC) && _isOnline.value

    /**
     * Indica si estamos en modo entrenamiento (solo local).
     */
    val isTrainingMode: Boolean
        get() = _syncState.value == SyncState.TRAINING_MODE

    /**
     * Inicia el modo entrenamiento.
     * Durante este modo, todas las operaciones son SOLO locales.
     */
    fun startTrainingMode() {
        Log.d(TAG, "🏋️ Iniciando modo entrenamiento - sincronización con Firebase DESACTIVADA")
        _syncState.value = SyncState.TRAINING_MODE
    }

    /**
     * Finaliza el modo entrenamiento.
     * Cambia el estado a PENDING_SYNC para indicar que hay que sincronizar.
     */
    fun endTrainingMode() {
        Log.d(TAG, "✅ Finalizando modo entrenamiento - sincronización PENDIENTE")
        _syncState.value = SyncState.PENDING_SYNC
    }

    /**
     * Marca que la sincronización se completó.
     */
    fun syncCompleted() {
        Log.d(TAG, "☁️ Sincronización completada")
        _syncState.value = SyncState.NORMAL
    }

    /**
     * Actualiza el estado de conexión.
     */
    fun setOnlineStatus(online: Boolean) {
        Log.d(TAG, "🌐 Estado de conexión: ${if (online) "ONLINE" else "OFFLINE"}")
        _isOnline.value = online
    }

    /**
     * Restablece al estado normal (para uso en pruebas o reinicio).
     */
    fun reset() {
        _syncState.value = SyncState.NORMAL
        _isOnline.value = true
    }
}

