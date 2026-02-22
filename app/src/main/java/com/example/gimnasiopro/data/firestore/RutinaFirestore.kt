package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Rutina para Firestore.
 *
 * SISTEMA DE IDs FIJOS:
 * - Cada rutina tiene un numeroRutina (1-20) que es INMUTABLE
 * - El ID del documento en Firebase es siempre "rutina_{numeroRutina}"
 * - Esto evita duplicaciones: cuando se actualiza una rutina, se sobreescribe el mismo documento
 *
 * Estructura en Firebase:
 * clientes/{userId}/rutinas/
 *   └── rutina_1/                    ← ID FIJO basado en numeroRutina
 *         ├── numeroRutina: 1        ← Número inmutable
 *         ├── nombre: "Mi Rutina"    ← Nombre puede cambiar
 *         ├── propietarioId: "userId"
 *         ├── creadoPorId: "userId"
 *         ├── creadoPorTipo: "cliente|trainer"
 *         ├── compartidaConTrainer: true/false
 *         ├── trainerId: "trainerId"
 *         ├── ejercicioIds: [...]
 *         ├── fechaCreacion: Timestamp
 *         └── fechaModificacion: Timestamp
 */
data class RutinaFirestore(
    val rutinaId: String = "",
    val numeroRutina: Int = 0,                // Número fijo de rutina (1-20) - CLAVE ÚNICA
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
            "numeroRutina" to numeroRutina,
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
         * Máximo número de rutinas permitidas por usuario
         */
        const val MAX_RUTINAS = 20

        /**
         * Genera el ID fijo del documento en Firebase basado en el número de rutina.
         * Ejemplo: rutina_1, rutina_2, ... rutina_20
         * Este ID NUNCA cambia, evitando duplicaciones.
         */
        fun generarDocumentId(numeroRutina: Int): String = "rutina_$numeroRutina"

        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): RutinaFirestore? {
            return try {
                val data = document.data ?: return null
                RutinaFirestore(
                    rutinaId = document.id,
                    numeroRutina = (data["numeroRutina"] as? Long)?.toInt() ?: 0,
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
