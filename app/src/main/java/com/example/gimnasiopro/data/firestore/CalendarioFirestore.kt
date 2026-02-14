package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Calendario (Rutina por día de semana) para Firestore.
 * Se guarda en users/{userId}/calendario/{diaSemana}
 */
data class CalendarioFirestore(
    val diaSemana: Int, // 1 = Lunes, 2 = Martes, ..., 7 = Domingo
    val rutinaId: String? = null, // Referencia a users/{userId}/rutinas/{rutinaId}
    val activo: Boolean = true,
    val fechaModificacion: Date = Date()
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "diaSemana" to diaSemana,
            "rutinaId" to (rutinaId ?: ""),
            "activo" to activo,
            "fechaModificacion" to Timestamp(fechaModificacion)
        )
    }

    companion object {
        const val LUNES = 1
        const val MARTES = 2
        const val MIERCOLES = 3
        const val JUEVES = 4
        const val VIERNES = 5
        const val SABADO = 6
        const val DOMINGO = 7

        fun getNombreDia(diaSemana: Int): String {
            return when (diaSemana) {
                LUNES -> "Lunes"
                MARTES -> "Martes"
                MIERCOLES -> "Miércoles"
                JUEVES -> "Jueves"
                VIERNES -> "Viernes"
                SABADO -> "Sábado"
                DOMINGO -> "Domingo"
                else -> "Desconocido"
            }
        }

        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): CalendarioFirestore? {
            return try {
                val data = document.data ?: return null
                CalendarioFirestore(
                    diaSemana = document.id.toIntOrNull() ?: return null,
                    rutinaId = (data["rutinaId"] as? String)?.takeIf { it.isNotEmpty() },
                    activo = data["activo"] as? Boolean ?: true,
                    fechaModificacion = (data["fechaModificacion"] as? Timestamp)?.toDate() ?: Date()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
