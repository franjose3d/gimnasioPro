package com.example.gimnasiopro.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Modelo de Conexión entre Trainer y Cliente.
 */
data class ConexionTrainerCliente(
    val id: String = "",
    val trainerId: String = "",
    val clienteId: String = "",
    val estado: String = ESTADO_PENDIENTE,
    val solicitadoPor: String = "cliente",
    val fechaSolicitud: Date = Date(),
    val fechaRespuesta: Date? = null,
    val mensaje: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "trainerId" to trainerId,
            "clienteId" to clienteId,
            "estado" to estado,
            "solicitadoPor" to solicitadoPor,
            "fechaSolicitud" to Timestamp(fechaSolicitud),
            "fechaRespuesta" to fechaRespuesta?.let { Timestamp(it) },
            "mensaje" to mensaje
        )
    }

    companion object {
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_ACTIVA = "activa"
        const val ESTADO_RECHAZADA = "rechazada"
        const val ESTADO_FINALIZADA = "finalizada"

        fun fromDocument(document: DocumentSnapshot): ConexionTrainerCliente? {
            val data = document.data ?: return null
            return ConexionTrainerCliente(
                id = document.id,
                trainerId = data["trainerId"] as? String ?: "",
                clienteId = data["clienteId"] as? String ?: "",
                estado = data["estado"] as? String ?: ESTADO_PENDIENTE,
                solicitadoPor = data["solicitadoPor"] as? String ?: "cliente",
                fechaSolicitud = (data["fechaSolicitud"] as? Timestamp)?.toDate() ?: Date(),
                fechaRespuesta = (data["fechaRespuesta"] as? Timestamp)?.toDate(),
                mensaje = data["mensaje"] as? String ?: ""
            )
        }
    }
}
