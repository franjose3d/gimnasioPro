package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio de sincronización de mensajes.
 *
 * Escucha mensajes nuevos en Firebase y los guarda en Room.
 * Después elimina el mensaje de Firebase (confirma recepción).
 */
class MensajesSyncService(
    private val mensajeRepository: MensajeRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private var listenerRegistration: ListenerRegistration? = null

    companion object {
        private const val TAG = "MensajesSync"
    }

    /**
     * Iniciar escucha de mensajes pendientes.
     *
     * Llamar esto en MainActivity.onCreate() si el usuario está autenticado.
     */
    fun iniciarEscucha() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "No se puede iniciar escucha: usuario no autenticado")
            return
        }

        // Detener listener anterior si existe
        detenerEscucha()

        Log.d(TAG, "Iniciando escucha de mensajes para usuario: $userId")

        // Escuchar buzón personal en Firebase
        listenerRegistration = firestore
            .collection("mensajes_pendientes")
            .document(userId)
            .collection("mensajes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando mensajes: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            // Nuevo mensaje recibido
                            CoroutineScope(Dispatchers.IO).launch {
                                procesarMensajeNuevo(change.document.id, change.document.data)
                            }
                        }
                        else -> {
                            // Ignorar modificaciones y eliminaciones
                        }
                    }
                }
            }
    }

    /**
     * Procesar mensaje nuevo recibido de Firebase.
     */
    private suspend fun procesarMensajeNuevo(
        mensajeId: String,
        data: Map<String, Any>
    ) {
        try {
            val remitenteId = data["de"] as? String ?: return
            val texto = data["texto"] as? String ?: return
            val timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
            val conversacionId = data["conversacionId"] as? String ?: return

            Log.d(TAG, "Procesando mensaje nuevo: $mensajeId de $remitenteId")

            // 1. Guardar en Room local
            mensajeRepository.procesarMensajeRecibido(
                mensajeId = mensajeId,
                remitenteId = remitenteId,
                texto = texto,
                timestamp = timestamp,
                conversacionId = conversacionId
            )

            // 2. ELIMINAR de Firebase (confirmar recepción)
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("mensajes_pendientes")
                .document(userId)
                .collection("mensajes")
                .document(mensajeId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Mensaje $mensajeId eliminado de Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error eliminando mensaje de Firebase: ${e.message}")
                }

            Log.d(TAG, "✅ Mensaje procesado y guardado en Room")

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje: ${e.message}")
        }
    }

    /**
     * Detener escucha de mensajes.
     *
     * Llamar esto en MainActivity.onDestroy().
     */
    fun detenerEscucha() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Escucha de mensajes detenida")
    }
}
