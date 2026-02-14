package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Rutina para Firestore.
 * Se guarda en users/{userId}/rutinas/{rutinaId}
 */
data class RutinaFirestore(
    val rutinaId: String = "",
    val nombre: String,
    val ejercicioIds: List<String> = emptyList(), // IDs de ejercicios (String en Firestore)
    val fechaCreacion: Date = Date(),
    val fechaModificacion: Date = Date(),
    val creadoPor: String = "" // userId del usuario que la creó
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "nombre" to nombre,
            "ejercicioIds" to ejercicioIds,
            "fechaCreacion" to Timestamp(fechaCreacion),
            "fechaModificacion" to Timestamp(fechaModificacion),
            "creadoPor" to creadoPor
        )
    }

    companion object {
        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): RutinaFirestore? {
            return try {
                val data = document.data ?: return null
                RutinaFirestore(
                    rutinaId = document.id,
                    nombre = data["nombre"] as? String ?: "",
                    ejercicioIds = (data["ejercicioIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    fechaCreacion = (data["fechaCreacion"] as? Timestamp)?.toDate() ?: Date(),
                    fechaModificacion = (data["fechaModificacion"] as? Timestamp)?.toDate() ?: Date(),
                    creadoPor = data["creadoPor"] as? String ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
