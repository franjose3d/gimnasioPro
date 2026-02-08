package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa la asignación de una rutina a un día de la semana.
 * Permite que cada día de la semana tenga una rutina asignada.
 */
@Entity(tableName = "rutina_dia_semana")
data class RutinaDiaSemana(
    /**
     * Día de la semana (1 = Lunes, 2 = Martes, ..., 7 = Domingo)
     */
    @PrimaryKey
    val diaSemana: Int,

    /**
     * Número de la rutina asignada a este día (null si no hay rutina asignada)
     */
    val numeroRutina: Int?,

    /**
     * Fecha de última modificación
     */
    val fechaModificacion: Long = System.currentTimeMillis()
) {
    companion object {
        const val LUNES = 1
        const val MARTES = 2
        const val MIERCOLES = 3
        const val JUEVES = 4
        const val VIERNES = 5
        const val SABADO = 6
        const val DOMINGO = 7

        /**
         * Obtiene el nombre del día de la semana en español.
         */
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
    }
}
