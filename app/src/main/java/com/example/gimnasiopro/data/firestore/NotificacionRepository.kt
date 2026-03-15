package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repositorio para gestionar notificaciones y mensajes.
 *
 * Funcionalidades:
 * - Enviar/recibir mensajes (máx 200 chars, expiran en 7 días)
 * - Notificaciones de solicitudes de conexión
 * - Marcar como leídas
 * - Limpiar mensajes expirados
 */
class NotificacionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificacionesCollection = firestore.collection("notificaciones")

    companion object {
        private const val TAG = "NotificacionRepo"
    }

    // ==================== ENVIAR NOTIFICACIONES ====================

    /**
     * Enviar un mensaje breve (máx 200 caracteres, expira en 7 días)
     */
    suspend fun enviarMensaje(
        destinatarioId: String,
        remitenteId: String,
        remitenteNombre: String,
        remitenteTipo: String,
        mensaje: String
    ): Result<String> {
        return try {
            val notificacion = Notificacion(
                destinatarioId = destinatarioId,
                remitenteId = remitenteId,
                remitenteNombre = remitenteNombre,
                remitenteTipo = remitenteTipo,
                tipo = Notificacion.TIPO_MENSAJE,
                titulo = "Mensaje de $remitenteNombre",
                mensaje = mensaje.take(200),
                fechaCreacion = Date(),
                fechaExpiracion = Notificacion.calcularFechaExpiracion()
            )

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Log.d(TAG, "✅ Mensaje enviado: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Crear notificación de nuevo mensaje (para notificar al destinatario)
     */
    suspend fun crearNotificacionMensaje(
        destinatarioId: String,
        remitenteId: String,
        remitenteNombre: String,
        textoPreview: String
    ): Result<String> {
        return try {
            val notificacion = Notificacion(
                destinatarioId = destinatarioId,
                remitenteId = remitenteId,
                remitenteNombre = remitenteNombre,
                remitenteTipo = "usuario",
                tipo = Notificacion.TIPO_MENSAJE,
                titulo = "Nuevo mensaje de $remitenteNombre",
                mensaje = textoPreview,
                fechaCreacion = Date(),
                fechaExpiracion = Notificacion.calcularFechaExpiracion()
            )

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Log.d(TAG, "✅ Notificación de mensaje creada: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando notificación de mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Enviar notificación de solicitud de conexión (cliente → trainer)
     */
    suspend fun enviarSolicitudConexion(
        trainerId: String,
        clienteId: String,
        clienteNombre: String,
        mensaje: String = "",
        conexionId: String = ""
    ): Result<String> {
        return try {
            val extras = mutableMapOf("clienteId" to clienteId)
            if (conexionId.isNotEmpty()) {
                extras["conexionId"] = conexionId
            }

            val notificacion = Notificacion(
                destinatarioId = trainerId,
                remitenteId = clienteId,
                remitenteNombre = clienteNombre,
                remitenteTipo = "cliente",
                tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
                titulo = "Nueva solicitud de conexión",
                mensaje = if (mensaje.isNotEmpty()) mensaje.take(200) else "$clienteNombre quiere conectar contigo como trainer",
                fechaCreacion = Date(),
                datosExtra = extras,
                leida = false  // 🔔 EXPLÍCITO: marca como no leída
            )

            Log.d(TAG, "📤 Creando notificación de solicitud:")
            Log.d(TAG, "   Destinatario (trainer): $trainerId")
            Log.d(TAG, "   Remitente (cliente): $clienteId ($clienteNombre)")
            Log.d(TAG, "   Tipo: ${notificacion.tipo}")
            Log.d(TAG, "   Leída: ${notificacion.leida}")

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Log.d(TAG, "✅ Solicitud de conexión enviada: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando solicitud: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Enviar invitación de trainer a cliente
     */
    suspend fun enviarInvitacionTrainer(
        clienteId: String,
        trainerId: String,
        trainerNombre: String,
        mensaje: String = "",
        conexionId: String = ""
    ): Result<String> {
        return try {
            val extras = mutableMapOf("trainerId" to trainerId)
            if (conexionId.isNotEmpty()) {
                extras["conexionId"] = conexionId
            }

            val notificacion = Notificacion(
                destinatarioId = clienteId,
                remitenteId = trainerId,
                remitenteNombre = trainerNombre,
                remitenteTipo = "trainer",
                tipo = Notificacion.TIPO_INVITACION_TRAINER,
                titulo = "Invitación de trainer",
                mensaje = if (mensaje.isNotEmpty()) mensaje.take(200) else "$trainerNombre te invita a ser su cliente",
                fechaCreacion = Date(),
                datosExtra = extras,
                leida = false  // 🔔 EXPLÍCITO: marca como no leída
            )

            Log.d(TAG, "📤 Creando notificación de invitación:")
            Log.d(TAG, "   Destinatario (cliente): $clienteId")
            Log.d(TAG, "   Remitente (trainer): $trainerId ($trainerNombre)")
            Log.d(TAG, "   Tipo: ${notificacion.tipo}")
            Log.d(TAG, "   Leída: ${notificacion.leida}")

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Log.d(TAG, "✅ Invitación de trainer enviada: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando invitación: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Notificar que una conexión fue aceptada
     */
    suspend fun notificarConexionAceptada(
        destinatarioId: String,
        aceptadorId: String,
        aceptadorNombre: String,
        aceptadorTipo: String
    ): Result<String> {
        return try {
            val notificacion = Notificacion(
                destinatarioId = destinatarioId,
                remitenteId = aceptadorId,
                remitenteNombre = aceptadorNombre,
                remitenteTipo = aceptadorTipo,
                tipo = Notificacion.TIPO_CONEXION_ACEPTADA,
                titulo = "¡Conexión aceptada!",
                mensaje = "$aceptadorNombre ha aceptado tu solicitud de conexión",
                fechaCreacion = Date()
            )

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Notificar que una conexión fue rechazada
     */
    suspend fun notificarConexionRechazada(
        destinatarioId: String,
        rechazadorId: String,
        rechazadorNombre: String,
        rechazadorTipo: String
    ): Result<String> {
        return try {
            val notificacion = Notificacion(
                destinatarioId = destinatarioId,
                remitenteId = rechazadorId,
                remitenteNombre = rechazadorNombre,
                remitenteTipo = rechazadorTipo,
                tipo = Notificacion.TIPO_CONEXION_RECHAZADA,
                titulo = "Solicitud rechazada",
                mensaje = "$rechazadorNombre ha rechazado la solicitud de conexión",
                fechaCreacion = Date()
            )

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Notificar que una rutina fue actualizada por el trainer
     */
    suspend fun notificarRutinaActualizada(
        clienteId: String,
        trainerId: String,
        trainerNombre: String,
        rutinaNombre: String
    ): Result<String> {
        return try {
            val notificacion = Notificacion(
                destinatarioId = clienteId,
                remitenteId = trainerId,
                remitenteNombre = trainerNombre,
                remitenteTipo = "trainer",
                tipo = Notificacion.TIPO_RUTINA_ACTUALIZADA,
                titulo = "Rutina actualizada",
                mensaje = "$trainerNombre ha actualizado tu rutina: $rutinaNombre",
                fechaCreacion = Date()
            )

            val docRef = notificacionesCollection.add(notificacion.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CONSULTAR NOTIFICACIONES ====================

    /**
     * Obtener notificaciones de un usuario (en tiempo real)
     * No usa orderBy en la query para evitar necesidad de índice compuesto.
     * El ordenamiento se hace en memoria (las notificaciones son pocas por usuario).
     */
    fun getNotificacionesFlow(userId: String): Flow<List<Notificacion>> = callbackFlow {
        Log.d(TAG, "🔔 Iniciando listener de notificaciones para userId: $userId")

        val listenerRegistration = notificacionesCollection
            .whereEqualTo("destinatarioId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error escuchando notificaciones: ${error.message}", error)
                    // Propagar el error al colector para que la UI pueda informar al usuario
                    close(error)
                    return@addSnapshotListener
                }

                Log.d(TAG, "📬 Snapshot recibido: ${snapshot?.documents?.size ?: 0} documentos")

                val notificaciones = snapshot?.documents
                    ?.mapNotNull { doc ->
                        try {
                            Notificacion.fromDocument(doc)
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error parseando notificación ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    ?.filter { notif ->
                        // Filtrar mensajes expirados
                        if (notif.tipo == Notificacion.TIPO_MENSAJE && notif.fechaExpiracion != null) {
                            notif.fechaExpiracion.after(Date())
                        } else {
                            true
                        }
                    }
                    // Ordenar en memoria por fecha de creación (más reciente primero)
                    ?.sortedByDescending { it.fechaCreacion }
                    ?: emptyList()

                Log.d(TAG, "✅ Notificaciones procesadas: ${notificaciones.size}")
                trySend(notificaciones)
            }

        awaitClose {
            Log.d(TAG, "🔕 Cerrando listener de notificaciones")
            listenerRegistration.remove()
        }
    }

    /**
     * Obtener notificaciones una sola vez
     */
    fun getNotificaciones(userId: String): Flow<List<Notificacion>> = flow {
        val snapshot = notificacionesCollection
            .whereEqualTo("destinatarioId", userId)
            .get()
            .await()

        val notificaciones = snapshot.documents
            .mapNotNull { Notificacion.fromDocument(it) }
            .filter { notif ->
                // Filtrar mensajes expirados
                if (notif.tipo == Notificacion.TIPO_MENSAJE && notif.fechaExpiracion != null) {
                    notif.fechaExpiracion.after(Date())
                } else {
                    true
                }
            }
            .sortedByDescending { it.fechaCreacion }

        emit(notificaciones)
    }

    /**
     * Obtener solo notificaciones no leídas
     */
    fun getNotificacionesNoLeidas(userId: String): Flow<List<Notificacion>> = flow {
        val snapshot = notificacionesCollection
            .whereEqualTo("destinatarioId", userId)
            .whereEqualTo("leida", false)
            .get()
            .await()

        val notificaciones = snapshot.documents
            .mapNotNull { Notificacion.fromDocument(it) }
            .filter { notif ->
                if (notif.tipo == Notificacion.TIPO_MENSAJE && notif.fechaExpiracion != null) {
                    notif.fechaExpiracion.after(Date())
                } else {
                    true
                }
            }
            .sortedByDescending { it.fechaCreacion }

        emit(notificaciones)
    }

    /**
     * Contar notificaciones no leídas
     */
    suspend fun contarNoLeidas(userId: String): Int {
        return try {
            val snapshot = notificacionesCollection
                .whereEqualTo("destinatarioId", userId)
                .whereEqualTo("leida", false)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { Notificacion.fromDocument(it) }
                .filter { notif ->
                    if (notif.tipo == Notificacion.TIPO_MENSAJE && notif.fechaExpiracion != null) {
                        notif.fechaExpiracion.after(Date())
                    } else {
                        true
                    }
                }
                .size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Obtener conversación entre dos usuarios
     */
    fun getConversacion(userId1: String, userId2: String): Flow<List<Notificacion>> = flow {
        // Mensajes enviados por userId1 a userId2
        val enviados = notificacionesCollection
            .whereEqualTo("remitenteId", userId1)
            .whereEqualTo("destinatarioId", userId2)
            .whereEqualTo("tipo", Notificacion.TIPO_MENSAJE)
            .get()
            .await()
            .documents
            .mapNotNull { Notificacion.fromDocument(it) }

        // Mensajes recibidos de userId2
        val recibidos = notificacionesCollection
            .whereEqualTo("remitenteId", userId2)
            .whereEqualTo("destinatarioId", userId1)
            .whereEqualTo("tipo", Notificacion.TIPO_MENSAJE)
            .get()
            .await()
            .documents
            .mapNotNull { Notificacion.fromDocument(it) }

        // Combinar y ordenar por fecha
        val conversacion = (enviados + recibidos)
            .filter { it.fechaExpiracion == null || it.fechaExpiracion.after(Date()) }
            .sortedBy { it.fechaCreacion }

        emit(conversacion)
    }

    // ==================== ACCIONES ====================

    /**
     * Marcar notificación como leída
     */
    suspend fun marcarComoLeida(notificacionId: String): Result<Unit> {
        return try {
            notificacionesCollection.document(notificacionId)
                .update("leida", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar notificación como procesada (aceptada/rechazada) y leída
     */
    suspend fun marcarComoProcesada(notificacionId: String): Result<Unit> {
        return try {
            notificacionesCollection.document(notificacionId)
                .update(mapOf(
                    "procesada" to true,
                    "leida" to true
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar todas las notificaciones de un usuario como leídas
     */
    suspend fun marcarTodasComoLeidas(userId: String): Result<Unit> {
        return try {
            val snapshot = notificacionesCollection
                .whereEqualTo("destinatarioId", userId)
                .whereEqualTo("leida", false)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "leida", true)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una notificación
     */
    suspend fun eliminarNotificacion(notificacionId: String): Result<Unit> {
        return try {
            if (notificacionId.isEmpty()) {
                Log.e(TAG, "❌ ID de notificación vacío, no se puede eliminar")
                return Result.failure(IllegalArgumentException("ID de notificación vacío"))
            }
            Log.d(TAG, "🗑️ Eliminando notificación: $notificacionId")
            notificacionesCollection.document(notificacionId).delete().await()
            Log.d(TAG, "✅ Notificación eliminada: $notificacionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando notificación $notificacionId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Limpiar mensajes expirados (llamar periódicamente)
     */
    suspend fun limpiarMensajesExpirados(): Result<Int> {
        return try {
            val ahora = com.google.firebase.Timestamp(Date())
            val snapshot = notificacionesCollection
                .whereEqualTo("tipo", Notificacion.TIPO_MENSAJE)
                .whereLessThan("fechaExpiracion", ahora)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "🗑️ Limpiados ${snapshot.size()} mensajes expirados")
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando mensajes: ${e.message}")
            Result.failure(e)
        }
    }
}

