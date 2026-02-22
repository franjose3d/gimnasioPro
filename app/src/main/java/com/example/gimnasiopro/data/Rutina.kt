package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Convertidor de tipos para guardar listas de IDs como String en Room.
 */
class EjercicioIdsConverter {
    @TypeConverter
    fun fromList(ejercicioIds: List<Int>): String {
        return ejercicioIds.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String): List<Int> {
        return if (data.isEmpty()) {
            emptyList()
        } else {
            data.split(",").mapNotNull { it.toIntOrNull() }
        }
    }
}

/**
 * Entidad que representa una rutina de ejercicios.
 * Cada rutina tiene un número del 1 al 10 y contiene una lista de IDs de ejercicios.
 */
@Entity(tableName = "rutinas")
@TypeConverters(EjercicioIdsConverter::class)
data class Rutina(
    @PrimaryKey
    val numeroRutina: Int,
    val nombre: String,
    val ejercicioIds: List<Int> = emptyList(),
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaModificacion: Long = System.currentTimeMillis()
) {
    /**
     * Obtiene la lista de IDs de ejercicios como Long.
     */
    fun getEjercicioIdsList(): List<Long> {
        return ejercicioIds.map { it.toLong() }
    }
}
