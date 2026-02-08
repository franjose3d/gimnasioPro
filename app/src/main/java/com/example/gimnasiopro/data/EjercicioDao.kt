package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para la entidad Ejercicio.
 * Proporciona métodos para acceder y manipular los ejercicios en la base de datos.
 */
@Dao
interface EjercicioDao {

    /**
     * Obtiene todos los ejercicios ordenados por grupo muscular y nombre.
     */
    @Query("SELECT * FROM ejercicios ORDER BY grupoMuscular, nombre")
    fun getAllEjercicios(): Flow<List<Ejercicio>>

    /**
     * Obtiene todos los ejercicios de un grupo muscular específico.
     */
    @Query("SELECT * FROM ejercicios WHERE grupoMuscular = :grupoMuscular ORDER BY nombre")
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>>

    /**
     * Obtiene un ejercicio por su ID.
     */
    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun getEjercicioById(id: Long): Ejercicio?

    /**
     * Busca ejercicios por nombre (búsqueda parcial).
     */
    @Query("SELECT * FROM ejercicios WHERE nombre LIKE '%' || :searchQuery || '%' ORDER BY grupoMuscular, nombre")
    fun searchEjercicios(searchQuery: String): Flow<List<Ejercicio>>

    /**
     * Obtiene todos los grupos musculares únicos.
     */
    @Query("SELECT DISTINCT grupoMuscular FROM ejercicios ORDER BY grupoMuscular")
    fun getAllGruposMusculares(): Flow<List<String>>

    /**
     * Inserta un nuevo ejercicio.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEjercicio(ejercicio: Ejercicio): Long

    /**
     * Inserta una lista de ejercicios.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEjercicios(ejercicios: List<Ejercicio>)

    /**
     * Actualiza un ejercicio existente.
     */
    @Update
    suspend fun updateEjercicio(ejercicio: Ejercicio)

    /**
     * Elimina un ejercicio.
     */
    @Delete
    suspend fun deleteEjercicio(ejercicio: Ejercicio)

    /**
     * Elimina todos los ejercicios.
     */
    @Query("DELETE FROM ejercicios")
    suspend fun deleteAllEjercicios()

    /**
     * Cuenta el número total de ejercicios.
     */
    @Query("SELECT COUNT(*) FROM ejercicios")
    suspend fun getEjerciciosCount(): Int

    /**
     * Obtiene ejercicios por una lista de IDs.
     */
    @Query("SELECT * FROM ejercicios WHERE id IN (:ids)")
    suspend fun getEjerciciosByIds(ids: List<Long>): List<Ejercicio>

    /**
     * Obtiene ejercicios por una lista de IDs como Flow.
     */
    @Query("SELECT * FROM ejercicios WHERE id IN (:ids)")
    fun getEjerciciosByIdsFlow(ids: List<Long>): Flow<List<Ejercicio>>
}
