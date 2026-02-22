package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un registro de entrenamiento en la base de datos.
 * Guarda los datos de cada ejercicio realizado en una sesión de entrenamiento.
 */
@Entity(tableName = "registros_entrenamiento")
data class RegistroEntrenamiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rutinaId: Int,
    val ejercicioId: Long,
    val repeticiones: Int,
    val pesoKg: Float,
    val completado: Boolean = false,
    val fechaEntrenamiento: Long = System.currentTimeMillis()
)

