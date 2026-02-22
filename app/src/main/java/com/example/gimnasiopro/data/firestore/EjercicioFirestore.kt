package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Modelo de Ejercicio para Firestore.
 * Representa un ejercicio en la colección global "ejercicios".
 */
data class EjercicioFirestore(
    val id: String = "",
    val grupoMuscular: String,
    val nombre: String,
    val descripcion: String,
    val imagenUrl: String? = null,
    val creadoPor: String? = null, // userId del admin/trainer que lo creó
    val esPredefinido: Boolean = true, // true para ejercicios del sistema
    val fechaCreacion: Date = Date()
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "grupoMuscular" to grupoMuscular,
            "nombre" to nombre,
            "descripcion" to descripcion,
            "imagenUrl" to imagenUrl,
            "creadoPor" to creadoPor,
            "esPredefinido" to esPredefinido,
            "fechaCreacion" to fechaCreacion
        )
    }

    companion object {
        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): EjercicioFirestore? {
            return try {
                val data = document.data ?: return null
                EjercicioFirestore(
                    id = document.id,
                    grupoMuscular = data["grupoMuscular"] as? String ?: "",
                    nombre = data["nombre"] as? String ?: "",
                    descripcion = data["descripcion"] as? String ?: "",
                    imagenUrl = data["imagenUrl"] as? String,
                    creadoPor = data["creadoPor"] as? String,
                    esPredefinido = data["esPredefinido"] as? Boolean ?: true,
                    fechaCreacion = (data["fechaCreacion"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Crear desde Map de Firestore
         */
        fun fromMap(id: String, map: Map<String, Any?>): EjercicioFirestore? {
            return try {
                EjercicioFirestore(
                    id = id,
                    grupoMuscular = map["grupoMuscular"] as? String ?: "",
                    nombre = map["nombre"] as? String ?: "",
                    descripcion = map["descripcion"] as? String ?: "",
                    imagenUrl = map["imagenUrl"] as? String,
                    creadoPor = map["creadoPor"] as? String,
                    esPredefinido = map["esPredefinido"] as? Boolean ?: true,
                    fechaCreacion = (map["fechaCreacion"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
