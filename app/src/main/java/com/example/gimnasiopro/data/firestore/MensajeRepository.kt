package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.example.gimnasiopro.data.local.MensajeDao
import com.example.gimnasiopro.data.local.MensajeLocal
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Repositorio de mensajería con arquitectura híbrida:
 * - Firebase: Buzón temporal (solo mensajes en tránsito)
 * - Room: Almacenamiento permanente (historial completo)
 *
 * Características:
 * - Mensajes se eliminan de Firebase al ser recibidos
 * - Historial completo en Room (offline-first)
 * - Límite de 200 caracteres por mensaje
 */
class MensajeRepository(
    private val mensajeDao: MensajeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        const val MAX_CARACTERES = 200
        const val TAG = "MensajeRepository"
    }

    /**
     * Enviar mensaje a otro usuario.
     *
     * Flujo:
     * 1. Guarda en Room local (del remitente)
     * 2. Envía a Firebase temporal (buzón del destinatario)
     * 3. Crea notificación
     */
    suspend fun enviarMensaje(
        destinatarioId: String,
        texto: String,
        conversacionId: String,
        remitenteNombre: String = ""
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Validar y truncar texto
            val textoTruncado = texto.take(MAX_CARACTERES)
            val mensajeId = UUID.randomUUID().toString()

            // 1. Guardar en Room local (historial del remitente)
            val mensajeLocal = MensajeLocal(
                id = mensajeId,
                conversacionId = conversacionId,
                remitenteId = userId,
                destinatarioId = destinatarioId,
                texto = textoTruncado,
                fechaEnvio = System.currentTimeMillis(),
                leido = true,  // Mis mensajes están "leídos" por mí
                entregado = false,
                esRemitente = true  // Yo lo envié
            )
            mensajeDao.insertarMensaje(mensajeLocal)

            // 2. Enviar a Firebase temporal (buzón del destinatario)
            val ttl = Timestamp(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))

            firestore.collection("mensajes_pendientes")
                .document(destinatarioId)
                .collection("mensajes")
                .document(mensajeId)
                .set(hashMapOf(
                    "de" to userId,
                    "texto" to textoTruncado,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "ttl" to ttl,
                    "conversacionId" to conversacionId,
                    "remitenteNombre" to remitenteNombre
                ))
                .await()

            // 3. Marcar como entregado localmente
            mensajeDao.marcarComoEntregado(mensajeId)

            // 4. Crear notificación para el destinatario
            try {
                val notificacionRepository = NotificacionRepository()
                notificacionRepository.crearNotificacionMensaje(
                    destinatarioId = destinatarioId,
                    remitenteId = userId,
                    remitenteNombre = remitenteNombre,
                    textoPreview = textoTruncado.take(50) + if (textoTruncado.length > 50) "..." else ""
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error creando notificación: ${e.message}")
                // No fallar si falla la notificación
            }

            Result.success(mensajeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener mensajes de una conversación desde Room.
     *
     * IMPORTANTE: Esto NO consulta Firebase, solo Room local.
     * El historial completo está en Room.
     */
    fun getMensajesConversacion(conversacionId: String): Flow<List<MensajeLocal>> {
        return mensajeDao.getMensajesPorConversacion(conversacionId)
    }

    /**
     * Marcar todos los mensajes de una conversación como leídos.
     */
    suspend fun marcarComoLeidos(conversacionId: String): Result<Unit> {
        return try {
            mensajeDao.marcarTodosComoLeidos(conversacionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando como leídos: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Marcar TODOS los mensajes recibidos (de todas las conversaciones) como leídos.
     * Se usa al pulsar "Marcar todas como leídas" en NotificacionesActivity.
     */
    suspend fun marcarTodosLosMensajesComoLeidos(): Result<Unit> {
        return try {
            mensajeDao.marcarTodosLosMensajesComoLeidos()
            Log.d(TAG, "✅ Todos los mensajes marcados como leídos")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marcando todos los mensajes como leídos: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Contar mensajes no leídos de una conversación.
     */
    fun contarNoLeidos(conversacionId: String): Flow<Int> {
        return mensajeDao.contarNoLeidos(conversacionId)
    }

    /**
     * Contar TOTAL de mensajes no leídos (todas las conversaciones).
     * Útil para el badge de notificaciones.
     */
    fun contarTotalNoLeidos(): Flow<Int> {
        return mensajeDao.contarTotalNoLeidos()
    }

    /**
     * Obtener último mensaje de una conversación.
     * Útil para mostrar preview en lista de conversaciones.
     */
    suspend fun getUltimoMensaje(conversacionId: String): MensajeLocal? {
        return try {
            mensajeDao.getUltimoMensaje(conversacionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo último mensaje: ${e.message}")
            null
        }
    }

    /**
     * Eliminar conversación completa (de Room local).
     * Firebase temporal ya estará vacío.
     */
    suspend fun eliminarConversacion(conversacionId: String): Result<Unit> {
        return try {
            mensajeDao.eliminarConversacion(conversacionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando conversación: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener fecha del primer mensaje de una conversación.
     * Usado para calcular cuántos días le quedan antes de expirar (7 días desde el primer mensaje).
     */
    suspend fun getFechaPrimerMensaje(conversacionId: String): Long? {
        return try {
            mensajeDao.getFechaPrimerMensaje(conversacionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo fecha primer mensaje: ${e.message}")
            null
        }
    }

    /**
     * Eliminar conversaciones cuyo primer mensaje tiene más de 7 días (expiradas).
     * Llamar al iniciar la app para mantener Room limpio.
     */
    suspend fun eliminarConversacionesExpiradas(): Result<Unit> {
        return try {
            val hace7Dias = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            mensajeDao.eliminarConversacionesExpiradas(hace7Dias)
            Log.d(TAG, "✅ Conversaciones expiradas eliminadas")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando conversaciones expiradas: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Procesar mensaje recibido de Firebase.
     *
     * NOTA: Este método lo llamará MensajesSyncService automáticamente.
     * NO necesitas llamarlo manualmente.
     */
    suspend fun procesarMensajeRecibido(
        mensajeId: String,
        remitenteId: String,
        texto: String,
        timestamp: Long,
        conversacionId: String
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Guardar en Room local
            val mensajeLocal = MensajeLocal(
                id = mensajeId,
                conversacionId = conversacionId,
                remitenteId = remitenteId,
                destinatarioId = userId,
                texto = texto,
                fechaEnvio = timestamp,
                leido = false,      // Mensaje nuevo no leído
                entregado = true,   // Ya fue entregado
                esRemitente = false // Lo recibí, no lo envié
            )
            mensajeDao.insertarMensaje(mensajeLocal)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje recibido: ${e.message}")
            Result.failure(e)
        }
    }
}
