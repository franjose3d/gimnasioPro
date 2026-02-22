package com.example.gimnasiopro.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Modelo de Notificación para solicitudes, mensajes y alertas.
 *
 * Tipos de notificación:
 * - SOLICITUD_CONEXION: Cliente solicita trainer o trainer invita cliente
 * - MENSAJE: Mensaje breve (máx 200 caracteres)
 * - RUTINA_ACTUALIZADA: Trainer actualizó una rutina del cliente
 * - SISTEMA: Notificaciones del sistema
 */
data class Notificacion(
    val id: String = "",
    val destinatarioId: String = "",       // A quién va dirigida
    val remitenteId: String = "",          // Quién la envía
    val remitenteNombre: String = "",      // Nombre del remitente (para mostrar)
    val remitenteTipo: String = "",        // "cliente" o "trainer"
    val tipo: String = TIPO_MENSAJE,       // Tipo de notificación
    val titulo: String = "",               // Título breve
    val mensaje: String = "",              // Contenido (máx 200 caracteres para mensajes)
    val leida: Boolean = false,            // Si ya fue leída
    val procesada: Boolean = false,        // Si la solicitud ya fue aceptada/rechazada
    val fechaCreacion: Date = Date(),      // Cuándo se creó
    val fechaExpiracion: Date? = null,     // Cuándo expira (para mensajes: 7 días)
    val datosExtra: Map<String, String> = emptyMap()  // Datos adicionales (conexionId, rutinaId, etc)
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "destinatarioId" to destinatarioId,
            "remitenteId" to remitenteId,
            "remitenteNombre" to remitenteNombre,
            "remitenteTipo" to remitenteTipo,
            "tipo" to tipo,
            "titulo" to titulo,
            "mensaje" to mensaje.take(200), // Limitar a 200 caracteres
            "leida" to leida,
            "procesada" to procesada,
            "fechaCreacion" to Timestamp(fechaCreacion),
            "fechaExpiracion" to fechaExpiracion?.let { Timestamp(it) },
            "datosExtra" to datosExtra
        )
    }

    companion object {
        // Tipos de notificación
        const val TIPO_SOLICITUD_CONEXION = "solicitud_conexion"
        const val TIPO_INVITACION_TRAINER = "invitacion_trainer"
        const val TIPO_MENSAJE = "mensaje"
        const val TIPO_RUTINA_ACTUALIZADA = "rutina_actualizada"
        const val TIPO_CONEXION_ACEPTADA = "conexion_aceptada"
        const val TIPO_CONEXION_RECHAZADA = "conexion_rechazada"
        const val TIPO_SISTEMA = "sistema"

        // Duración de mensajes en milisegundos (7 días)
        const val DURACION_MENSAJE_MS = 7 * 24 * 60 * 60 * 1000L

        fun fromDocument(document: DocumentSnapshot): Notificacion? {
            val data = document.data ?: return null
            return Notificacion(
                id = document.id,
                destinatarioId = data["destinatarioId"] as? String ?: "",
                remitenteId = data["remitenteId"] as? String ?: "",
                remitenteNombre = data["remitenteNombre"] as? String ?: "",
                remitenteTipo = data["remitenteTipo"] as? String ?: "",
                tipo = data["tipo"] as? String ?: TIPO_MENSAJE,
                titulo = data["titulo"] as? String ?: "",
                mensaje = data["mensaje"] as? String ?: "",
                leida = data["leida"] as? Boolean ?: false,
                procesada = data["procesada"] as? Boolean ?: false,
                fechaCreacion = (data["fechaCreacion"] as? Timestamp)?.toDate() ?: Date(),
                fechaExpiracion = (data["fechaExpiracion"] as? Timestamp)?.toDate(),
                datosExtra = @Suppress("UNCHECKED_CAST") (data["datosExtra"] as? Map<String, String>) ?: emptyMap()
            )
        }

        /**
         * Crear fecha de expiración para mensajes (7 días desde ahora)
         */
        fun calcularFechaExpiracion(): Date {
            return Date(System.currentTimeMillis() + DURACION_MENSAJE_MS)
        }
    }
}

