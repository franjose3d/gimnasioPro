package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos relacionadas con las rutinas.
 */
@Dao
interface RutinaDao {

    /**
     * Obtiene todas las rutinas.
     */
    @Query("SELECT * FROM rutinas ORDER BY numeroRutina ASC")
    fun getAllRutinas(): Flow<List<Rutina>>

    /**
     * Obtiene una rutina específica por su número.
     */
    @Query("SELECT * FROM rutinas WHERE numeroRutina = :numeroRutina")
    fun getRutinaByNumero(numeroRutina: Int): Flow<Rutina?>

    /**
     * Obtiene una rutina específica por su número (suspending).
     */
    @Query("SELECT * FROM rutinas WHERE numeroRutina = :numeroRutina")
    suspend fun getRutinaByNumeroSync(numeroRutina: Int): Rutina?

    /**
     * Inserta una nueva rutina.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRutina(rutina: Rutina)

    /**
     * Inserta múltiples rutinas.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRutinas(rutinas: List<Rutina>)

    /**
     * Actualiza una rutina existente.
     */
    @Update
    suspend fun updateRutina(rutina: Rutina)

    /**
     * Actualiza los ejercicios de una rutina específica.
     */
    @Query("UPDATE rutinas SET ejercicioIds = :ejercicioIds, fechaModificacion = :fechaModificacion WHERE numeroRutina = :numeroRutina")
    suspend fun updateEjerciciosDeRutina(numeroRutina: Int, ejercicioIds: String, fechaModificacion: Long)

    /**
     * Obtiene el número de rutinas.
     */
    @Query("SELECT COUNT(*) FROM rutinas")
    suspend fun getCountRutinas(): Int

    /**
     * Elimina todos los ejercicios de una rutina.
     */
    @Query("UPDATE rutinas SET ejercicioIds = '', fechaModificacion = :fechaModificacion WHERE numeroRutina = :numeroRutina")
    suspend fun clearEjerciciosDeRutina(numeroRutina: Int, fechaModificacion: Long = System.currentTimeMillis())

    /**
     * Actualiza el nombre de una rutina.
     */
    @Query("UPDATE rutinas SET nombre = :nombre, fechaModificacion = :fechaModificacion WHERE numeroRutina = :numeroRutina")
    suspend fun updateNombreRutina(numeroRutina: Int, nombre: String, fechaModificacion: Long = System.currentTimeMillis())

    /**
     * Elimina una rutina por su número.
     */
    @Query("DELETE FROM rutinas WHERE numeroRutina = :numeroRutina")
    suspend fun deleteRutinaByNumero(numeroRutina: Int)

    /**
     * Obtiene el número máximo de rutina existente.
     */
    @Query("SELECT MAX(numeroRutina) FROM rutinas")
    suspend fun getMaxNumeroRutina(): Int?
}

