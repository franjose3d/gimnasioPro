package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos relacionadas con la asignación de rutinas a días de la semana.
 */
@Dao
interface RutinaDiaSemanaDao {

    /**
     * Obtiene todas las asignaciones de rutinas a días de la semana.
     */
    @Query("SELECT * FROM rutina_dia_semana ORDER BY diaSemana ASC")
    fun getAllAsignaciones(): Flow<List<RutinaDiaSemana>>

    /**
     * Obtiene la rutina asignada a un día de la semana específico.
     */
    @Query("SELECT * FROM rutina_dia_semana WHERE diaSemana = :diaSemana")
    fun getAsignacionPorDia(diaSemana: Int): Flow<RutinaDiaSemana?>

    /**
     * Obtiene la rutina asignada a un día de la semana de forma síncrona.
     */
    @Query("SELECT * FROM rutina_dia_semana WHERE diaSemana = :diaSemana")
    suspend fun getAsignacionPorDiaSync(diaSemana: Int): RutinaDiaSemana?

    /**
     * Obtiene el número de rutina asignada a un día de la semana.
     */
    @Query("SELECT numeroRutina FROM rutina_dia_semana WHERE diaSemana = :diaSemana")
    suspend fun getNumeroRutinaPorDia(diaSemana: Int): Int?

    /**
     * Inserta o actualiza la asignación de una rutina a un día de la semana.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsignacion(asignacion: RutinaDiaSemana)

    /**
     * Elimina la asignación de rutina de un día de la semana.
     */
    @Query("DELETE FROM rutina_dia_semana WHERE diaSemana = :diaSemana")
    suspend fun deleteAsignacion(diaSemana: Int)

    /**
     * Elimina todas las asignaciones que tengan una rutina específica.
     * Útil cuando se elimina una rutina.
     */
    @Query("UPDATE rutina_dia_semana SET numeroRutina = NULL WHERE numeroRutina = :numeroRutina")
    suspend fun clearAsignacionesPorRutina(numeroRutina: Int)
}
