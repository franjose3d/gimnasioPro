package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar los registros de entrenamiento.
 */
@Dao
interface RegistroEntrenamientoDao {

    /**
     * Inserta un nuevo registro de entrenamiento.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroEntrenamiento): Long

    /**
     * Inserta múltiples registros de entrenamiento.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistros(registros: List<RegistroEntrenamiento>)

    /**
     * Obtiene el último registro de un ejercicio específico en una rutina.
     * Útil para cargar los últimos valores usados.
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE rutinaId = :rutinaId AND ejercicioId = :ejercicioId 
        ORDER BY fechaEntrenamiento DESC 
        LIMIT 1
    """)
    suspend fun getUltimoRegistro(rutinaId: Int, ejercicioId: Long): RegistroEntrenamiento?

    /**
     * Obtiene los últimos registros de todos los ejercicios de una rutina.
     */
    @Query("""
        SELECT r1.* FROM registros_entrenamiento r1
        INNER JOIN (
            SELECT ejercicioId, MAX(fechaEntrenamiento) as maxFecha
            FROM registros_entrenamiento
            WHERE rutinaId = :rutinaId
            GROUP BY ejercicioId
        ) r2 ON r1.ejercicioId = r2.ejercicioId AND r1.fechaEntrenamiento = r2.maxFecha
        WHERE r1.rutinaId = :rutinaId
    """)
    suspend fun getUltimosRegistrosDeRutina(rutinaId: Int): List<RegistroEntrenamiento>

    /**
     * Obtiene todos los registros de un ejercicio (historial completo).
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE ejercicioId = :ejercicioId 
        ORDER BY fechaEntrenamiento DESC
    """)
    fun getHistorialEjercicio(ejercicioId: Long): Flow<List<RegistroEntrenamiento>>

    /**
     * Obtiene todos los registros de una rutina.
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE rutinaId = :rutinaId 
        ORDER BY fechaEntrenamiento DESC
    """)
    fun getHistorialRutina(rutinaId: Int): Flow<List<RegistroEntrenamiento>>

    /**
     * Obtiene registros en un rango de fechas (para estadísticas).
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE fechaEntrenamiento BETWEEN :fechaInicio AND :fechaFin 
        ORDER BY fechaEntrenamiento DESC
    """)
    suspend fun getRegistrosEntreFechas(fechaInicio: Long, fechaFin: Long): List<RegistroEntrenamiento>

    /**
     * Cuenta el total de entrenamientos completados.
     */
    @Query("SELECT COUNT(DISTINCT fechaEntrenamiento / 86400000) FROM registros_entrenamiento WHERE completado = 1")
    suspend fun getTotalDiasEntrenados(): Int

    /**
     * Obtiene el peso máximo levantado en un ejercicio.
     */
    @Query("SELECT MAX(pesoKg) FROM registros_entrenamiento WHERE ejercicioId = :ejercicioId")
    suspend fun getPesoMaximoEjercicio(ejercicioId: Long): Float?

    /**
     * Obtiene las repeticiones máximas en un ejercicio.
     */
    @Query("SELECT MAX(repeticiones) FROM registros_entrenamiento WHERE ejercicioId = :ejercicioId")
    suspend fun getRepeticionesMaximasEjercicio(ejercicioId: Long): Int?

    /**
     * Obtiene el primer registro de un ejercicio (para calcular progreso).
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE ejercicioId = :ejercicioId AND completado = 1 
        ORDER BY fechaEntrenamiento ASC 
        LIMIT 1
    """)
    suspend fun getPrimerRegistroEjercicio(ejercicioId: Long): RegistroEntrenamiento?

    /**
     * Obtiene el último registro de un ejercicio (para calcular progreso).
     */
    @Query("""
        SELECT * FROM registros_entrenamiento 
        WHERE ejercicioId = :ejercicioId AND completado = 1 
        ORDER BY fechaEntrenamiento DESC 
        LIMIT 1
    """)
    suspend fun getUltimoRegistroEjercicio(ejercicioId: Long): RegistroEntrenamiento?

    /**
     * Elimina todos los registros de entrenamiento.
     */
    @Query("DELETE FROM registros_entrenamiento")
    suspend fun deleteAllRegistros()

    /**
     * Obtiene todos los ejercicios completados en un rango de fechas.
     */
    @Query("""
        SELECT ejercicioId FROM registros_entrenamiento 
        WHERE completado = 1 AND fechaEntrenamiento BETWEEN :fechaInicio AND :fechaFin
    """)
    suspend fun getEjerciciosCompletadosEntreFechas(fechaInicio: Long, fechaFin: Long): List<Long>
}

