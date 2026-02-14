package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Serie de entrenamiento para Firestore
 */
data class SerieFirestore(
    val pesoKg: Float = 0f,
    val repeticiones: Int = 0,
    val completado: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "pesoKg" to pesoKg,
            "repeticiones" to repeticiones,
            "completado" to completado
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SerieFirestore? {
            return try {
                SerieFirestore(
                    pesoKg = (map["pesoKg"] as? Number)?.toFloat() ?: 0f,
                    repeticiones = (map["repeticiones"] as? Number)?.toInt() ?: 0,
                    completado = map["completado"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Modelo de Ejercicio en un entrenamiento para Firestore
 */
data class EjercicioEntrenamientoFirestore(
    val ejercicioId: String, // Referencia a ejercicios/{ejercicioId}
    val series: List<SerieFirestore> = emptyList(),
    val completado: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "ejercicioId" to ejercicioId,
            "series" to series.map { it.toMap() },
            "completado" to completado
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): EjercicioEntrenamientoFirestore? {
            return try {
                EjercicioEntrenamientoFirestore(
                    ejercicioId = map["ejercicioId"] as? String ?: "",
                    series = (map["series"] as? List<*>)?.mapNotNull { 
                        if (it is Map<*, *>) SerieFirestore.fromMap(it as Map<String, Any?>) else null
                    } ?: emptyList(),
                    completado = map["completado"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Modelo de Entrenamiento para Firestore.
 * Se guarda en users/{userId}/entrenamientos/{entrenamientoId}
 */
data class EntrenamientoFirestore(
    val entrenamientoId: String = "",
    val rutinaId: String? = null, // Referencia a users/{userId}/rutinas/{rutinaId}
    val fechaEntrenamiento: Date = Date(),
    val ejercicios: List<EjercicioEntrenamientoFirestore> = emptyList(),
    val tiempoEntrenamientoMs: Long = 0,
    val volumenTotal: Float = 0f
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "rutinaId" to (rutinaId ?: ""),
            "fechaEntrenamiento" to Timestamp(fechaEntrenamiento),
            "ejercicios" to ejercicios.map { it.toMap() },
            "tiempoEntrenamientoMs" to tiempoEntrenamientoMs,
            "volumenTotal" to volumenTotal
        )
    }

    companion object {
        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): EntrenamientoFirestore? {
            return try {
                val data = document.data ?: return null
                EntrenamientoFirestore(
                    entrenamientoId = document.id,
                    rutinaId = (data["rutinaId"] as? String)?.takeIf { it.isNotEmpty() },
                    fechaEntrenamiento = (data["fechaEntrenamiento"] as? Timestamp)?.toDate() ?: Date(),
                    ejercicios = (data["ejercicios"] as? List<*>)?.mapNotNull {
                        if (it is Map<*, *>) EjercicioEntrenamientoFirestore.fromMap(it as Map<String, Any?>) else null
                    } ?: emptyList(),
                    tiempoEntrenamientoMs = (data["tiempoEntrenamientoMs"] as? Number)?.toLong() ?: 0,
                    volumenTotal = (data["volumenTotal"] as? Number)?.toFloat() ?: 0f
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
