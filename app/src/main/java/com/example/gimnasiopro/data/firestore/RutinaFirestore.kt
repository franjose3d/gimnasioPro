package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Rutina para Firestore.
 *
 * Estructura en Firebase:
 * rutinas/
 *   └── [rutinaId]/
 *         ├── nombre: "Mi Rutina"
 *         ├── propietarioId: "userId"          ← Dueño de la rutina
 *         ├── creadoPorId: "userId"            ← Quién la creó (puede ser trainer)
 *         ├── creadoPorTipo: "cliente|trainer"
 *         ├── compartidaConTrainer: true/false ← Si el trainer puede verla
 *         ├── trainerId: "trainerId"           ← Trainer asignado (si aplica)
 *         ├── ejercicioIds: [...]              ← Lista de IDs de ejercicios
 *         ├── fechaCreacion: Timestamp
 *         └── fechaModificacion: Timestamp
 */
data class RutinaFirestore(
    val rutinaId: String = "",
    val nombre: String = "",
    val propietarioId: String = "",           // userId del dueño de la rutina
    val creadoPorId: String = "",             // userId de quien la creó (puede ser trainer)
    val creadoPorTipo: String = "cliente",    // "cliente" o "trainer"
    val compartidaConTrainer: Boolean = true, // Si el trainer conectado puede verla
    val trainerId: String? = null,            // ID del trainer asignado (si tiene)
    val ejercicioIds: List<String> = emptyList(), // IDs de ejercicios
    val activa: Boolean = true,
    val fechaCreacion: Date = Date(),
    val fechaModificacion: Date = Date()
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "nombre" to nombre,
            "propietarioId" to propietarioId,
            "creadoPorId" to creadoPorId,
            "creadoPorTipo" to creadoPorTipo,
            "compartidaConTrainer" to compartidaConTrainer,
            "trainerId" to trainerId,
            "ejercicioIds" to ejercicioIds,
            "activa" to activa,
            "fechaCreacion" to Timestamp(fechaCreacion),
            "fechaModificacion" to Timestamp(fechaModificacion)
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
                    propietarioId = data["propietarioId"] as? String ?: data["creadoPor"] as? String ?: "",
                    creadoPorId = data["creadoPorId"] as? String ?: data["creadoPor"] as? String ?: "",
                    creadoPorTipo = data["creadoPorTipo"] as? String ?: "cliente",
                    compartidaConTrainer = data["compartidaConTrainer"] as? Boolean ?: true,
                    trainerId = data["trainerId"] as? String,
                    ejercicioIds = (data["ejercicioIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    activa = data["activa"] as? Boolean ?: true,
                    fechaCreacion = (data["fechaCreacion"] as? Timestamp)?.toDate() ?: Date(),
                    fechaModificacion = (data["fechaModificacion"] as? Timestamp)?.toDate() ?: Date()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
