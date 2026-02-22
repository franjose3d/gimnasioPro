package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Repositorio para gestionar mensajes entre usuarios.
 *
 * Características:
 * - Mensajes limitados a 200 caracteres
 * - Expiración automática a los 7 días
 * - Notificación al destinatario
 */
class MensajeRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val mensajesCollection = firestore.collection("mensajes")

    companion object {
        const val MAX_CARACTERES = 200
        const val DIAS_EXPIRACION = 7
    }

    /**
     * Enviar un mensaje a otro usuario.
     */
    suspend fun enviarMensaje(
        remitenteId: String,
        remitenteNombre: String,
        destinatarioId: String,
        texto: String
    ): Result<String> {
        return try {
            // Validar longitud
            val textoTruncado = texto.take(MAX_CARACTERES)

            val conversacionId = Mensaje.generarConversacionId(remitenteId, destinatarioId)

            val mensaje = Mensaje(
                remitenteId = remitenteId,
                remitenteNombre = remitenteNombre,
                destinatarioId = destinatarioId,
                texto = textoTruncado,
                fechaEnvio = Date(),
                conversacionId = conversacionId
            )

            val docRef = mensajesCollection.add(mensaje.toMap()).await()

            // Crear notificación para el destinatario
            val notificacionRepository = NotificacionRepository()
            notificacionRepository.crearNotificacionMensaje(
                destinatarioId = destinatarioId,
                remitenteId = remitenteId,
                remitenteNombre = remitenteNombre,
                textoPreview = textoTruncado.take(50) + if (textoTruncado.length > 50) "..." else ""
            )

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener mensajes de una conversación en tiempo real.
     */
    fun getMensajesConversacion(userId1: String, userId2: String): Flow<List<Mensaje>> = callbackFlow {
        val conversacionId = Mensaje.generarConversacionId(userId1, userId2)

        // Calcular fecha límite (7 días atrás)
        val fechaLimite = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -DIAS_EXPIRACION)
        }.time

        val listener = mensajesCollection
            .whereEqualTo("conversacionId", conversacionId)
            .whereGreaterThan("fechaEnvio", fechaLimite)
            .orderBy("fechaEnvio", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val mensajes = snapshot?.documents
                    ?.mapNotNull { Mensaje.fromDocument(it) }
                    ?: emptyList()

                trySend(mensajes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Marcar mensajes como leídos.
     */
    suspend fun marcarComoLeidos(conversacionId: String, usuarioId: String): Result<Unit> {
        return try {
            val mensajesNoLeidos = mensajesCollection
                .whereEqualTo("conversacionId", conversacionId)
                .whereEqualTo("destinatarioId", usuarioId)
                .whereEqualTo("leido", false)
                .get()
                .await()

            val batch = firestore.batch()
            mensajesNoLeidos.documents.forEach { doc ->
                batch.update(doc.reference, "leido", true)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar mensajes expirados (más de 7 días).
     * Se puede llamar periódicamente o mediante Cloud Functions.
     */
    suspend fun eliminarMensajesExpirados(): Result<Int> {
        return try {
            val fechaLimite = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -DIAS_EXPIRACION)
            }.time

            val mensajesExpirados = mensajesCollection
                .whereLessThan("fechaEnvio", fechaLimite)
                .get()
                .await()

            val batch = firestore.batch()
            mensajesExpirados.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Result.success(mensajesExpirados.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener número de mensajes no leídos para un usuario.
     * Optimizado: usa solo 2 campos en query (destinatarioId + leido)
     * y filtra la expiración localmente para evitar índice compuesto triple.
     */
    suspend fun getMensajesNoLeidos(usuarioId: String): Int {
        return try {
            val fechaLimite = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -DIAS_EXPIRACION)
            }.time

            val snapshot = mensajesCollection
                .whereEqualTo("destinatarioId", usuarioId)
                .whereEqualTo("leido", false)
                .get()
                .await()

            // Filtrar expiración localmente para ahorrar un índice compuesto
            snapshot.documents.count { doc ->
                val fechaEnvio = (doc.getTimestamp("fechaEnvio"))?.toDate()
                fechaEnvio != null && fechaEnvio.after(fechaLimite)
            }
        } catch (e: Exception) {
            0
        }
    }
}

