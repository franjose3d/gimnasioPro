package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa las estadísticas de un día de entrenamiento.
 * Se usa para calcular estadísticas diarias, mensuales y anuales.
 */
@Entity(tableName = "estadisticas_entrenamiento")
data class EstadisticaEntrenamiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Fecha del entrenamiento (solo día, sin hora - se usa para agrupar)
    val fecha: Long, // Timestamp del inicio del día (00:00:00)

    // Año y mes para facilitar consultas
    val anio: Int,
    val mes: Int,
    val dia: Int,

    // Tiempo de entrenamiento en milisegundos
    val tiempoEntrenamientoMs: Long,

    // Número de entrenamientos realizados ese día
    val numeroEntrenamientos: Int = 1,

    // Ejercicios completados ese día
    val ejerciciosCompletados: Int = 0,

    // Peso total levantado (suma de peso x repeticiones)
    val volumenTotal: Float = 0f,

    // Rutinas usadas ese día (IDs separados por coma)
    val rutinasUsadas: String = ""
)

