package com.example.gimnasiopro.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Modelo de mensaje entre usuarios.
 *
 * Características:
 * - Máximo 200 caracteres
 * - Expira después de 7 días
 */
data class Mensaje(
    val id: String = "",
    val remitenteId: String = "",
    val remitenteNombre: String = "",
    val destinatarioId: String = "",
    val texto: String = "",
    val fechaEnvio: Date = Date(),
    val leido: Boolean = false,
    val conversacionId: String = "" // ID ordenado de ambos usuarios
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "remitenteId" to remitenteId,
            "remitenteNombre" to remitenteNombre,
            "destinatarioId" to destinatarioId,
            "texto" to texto,
            "fechaEnvio" to Timestamp(fechaEnvio),
            "leido" to leido,
            "conversacionId" to conversacionId
        )
    }

    companion object {
        fun fromDocument(document: DocumentSnapshot): Mensaje? {
            val data = document.data ?: return null
            return Mensaje(
                id = document.id,
                remitenteId = data["remitenteId"] as? String ?: "",
                remitenteNombre = data["remitenteNombre"] as? String ?: "",
                destinatarioId = data["destinatarioId"] as? String ?: "",
                texto = data["texto"] as? String ?: "",
                fechaEnvio = (data["fechaEnvio"] as? Timestamp)?.toDate() ?: Date(),
                leido = data["leido"] as? Boolean ?: false,
                conversacionId = data["conversacionId"] as? String ?: ""
            )
        }

        /**
         * Genera un ID de conversación ordenando los IDs de los dos usuarios.
         * Esto asegura que ambos usuarios vean la misma conversación.
         */
        fun generarConversacionId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) {
                "${userId1}_${userId2}"
            } else {
                "${userId2}_${userId1}"
            }
        }
    }
}

