package com.example.gimnasiopro.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeDao {

    /**
     * Obtener mensajes de una conversación en tiempo real.
     * Ordenados por fecha ascendente (más antiguos primero).
     */
    @Query("SELECT * FROM mensajes_locales WHERE conversacionId = :conversacionId ORDER BY fechaEnvio ASC")
    fun getMensajesPorConversacion(conversacionId: String): Flow<List<MensajeLocal>>

    /**
     * Insertar o actualizar mensaje.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMensaje(mensaje: MensajeLocal)

    /**
     * Insertar múltiples mensajes.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMensajes(mensajes: List<MensajeLocal>)

    /**
     * Marcar mensaje como leído.
     */
    @Query("UPDATE mensajes_locales SET leido = 1 WHERE id = :mensajeId")
    suspend fun marcarComoLeido(mensajeId: String)

    /**
     * Marcar todos los mensajes de una conversación como leídos.
     */
    @Query("UPDATE mensajes_locales SET leido = 1 WHERE conversacionId = :conversacionId AND esRemitente = 0")
    suspend fun marcarTodosComoLeidos(conversacionId: String)

    /**
     * Marcar TODOS los mensajes recibidos (de todas las conversaciones) como leídos.
     * Para usar al pulsar "marcar todas como leídas" en NotificacionesActivity.
     */
    @Query("UPDATE mensajes_locales SET leido = 1 WHERE esRemitente = 0")
    suspend fun marcarTodosLosMensajesComoLeidos()

    /**
     * Marcar mensaje como entregado.
     */
    @Query("UPDATE mensajes_locales SET entregado = 1 WHERE id = :mensajeId")
    suspend fun marcarComoEntregado(mensajeId: String)

    /**
     * Contar mensajes no leídos de una conversación.
     */
    @Query("SELECT COUNT(*) FROM mensajes_locales WHERE conversacionId = :conversacionId AND leido = 0 AND esRemitente = 0")
    fun contarNoLeidos(conversacionId: String): Flow<Int>

    /**
     * Contar TOTAL de mensajes no leídos (todas las conversaciones).
     */
    @Query("SELECT COUNT(*) FROM mensajes_locales WHERE leido = 0 AND esRemitente = 0")
    fun contarTotalNoLeidos(): Flow<Int>

    /**
     * Obtener último mensaje de una conversación.
     */
    @Query("SELECT * FROM mensajes_locales WHERE conversacionId = :conversacionId ORDER BY fechaEnvio DESC LIMIT 1")
    suspend fun getUltimoMensaje(conversacionId: String): MensajeLocal?

    /**
     * Eliminar todos los mensajes de una conversación.
     */
    @Query("DELETE FROM mensajes_locales WHERE conversacionId = :conversacionId")
    suspend fun eliminarConversacion(conversacionId: String)

    /**
     * Fecha del primer mensaje de una conversación (para calcular expiración).
     */
    @Query("SELECT MIN(fechaEnvio) FROM mensajes_locales WHERE conversacionId = :conversacionId")
    suspend fun getFechaPrimerMensaje(conversacionId: String): Long?

    /**
     * Eliminar mensajes de conversaciones cuyo primer mensaje supera los 7 días.
     */
    @Query("DELETE FROM mensajes_locales WHERE conversacionId IN (SELECT conversacionId FROM mensajes_locales GROUP BY conversacionId HAVING MIN(fechaEnvio) < :fechaLimite)")
    suspend fun eliminarConversacionesExpiradas(fechaLimite: Long)
}