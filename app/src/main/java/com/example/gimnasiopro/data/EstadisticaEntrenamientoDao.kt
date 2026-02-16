package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar las estadísticas de entrenamiento.
 */
@Dao
interface EstadisticaEntrenamientoDao {

    /**
     * Inserta una nueva estadística.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstadistica(estadistica: EstadisticaEntrenamiento): Long

    /**
     * Actualiza una estadística existente.
     */
    @Update
    suspend fun updateEstadistica(estadistica: EstadisticaEntrenamiento)

    /**
     * Obtiene la estadística de un día específico.
     */
    @Query("SELECT * FROM estadisticas_entrenamiento WHERE fecha = :fecha LIMIT 1")
    suspend fun getEstadisticaPorFecha(fecha: Long): EstadisticaEntrenamiento?

    /**
     * Obtiene la estadística de hoy.
     */
    @Query("SELECT * FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes AND dia = :dia LIMIT 1")
    suspend fun getEstadisticaHoy(anio: Int, mes: Int, dia: Int): EstadisticaEntrenamiento?

    /**
     * Obtiene todas las estadísticas de un mes específico.
     */
    @Query("SELECT * FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes ORDER BY dia ASC")
    fun getEstadisticasMes(anio: Int, mes: Int): Flow<List<EstadisticaEntrenamiento>>

    /**
     * Obtiene todas las estadísticas de un año.
     */
    @Query("SELECT * FROM estadisticas_entrenamiento WHERE anio = :anio ORDER BY mes ASC, dia ASC")
    fun getEstadisticasAnio(anio: Int): Flow<List<EstadisticaEntrenamiento>>

    /**
     * Suma el tiempo total de entrenamiento de un día.
     */
    @Query("SELECT COALESCE(SUM(tiempoEntrenamientoMs), 0) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes AND dia = :dia")
    suspend fun getTiempoTotalDia(anio: Int, mes: Int, dia: Int): Long

    /**
     * Suma el tiempo total de entrenamiento del mes.
     */
    @Query("SELECT COALESCE(SUM(tiempoEntrenamientoMs), 0) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes")
    suspend fun getTiempoTotalMes(anio: Int, mes: Int): Long

    /**
     * Suma el tiempo total de entrenamiento del año.
     */
    @Query("SELECT COALESCE(SUM(tiempoEntrenamientoMs), 0) FROM estadisticas_entrenamiento WHERE anio = :anio")
    suspend fun getTiempoTotalAnio(anio: Int): Long

    /**
     * Cuenta el número de entrenamientos del mes.
     */
    @Query("SELECT COALESCE(SUM(numeroEntrenamientos), 0) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes")
    suspend fun getNumeroEntrenamientosMes(anio: Int, mes: Int): Int

    /**
     * Cuenta el número de días con entrenamiento del mes (para racha).
     */
    @Query("SELECT COUNT(*) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes AND numeroEntrenamientos > 0")
    suspend fun getDiasEntrenadosMes(anio: Int, mes: Int): Int

    /**
     * Obtiene los días entrenados ordenados por fecha (para calcular racha).
     */
    @Query("SELECT fecha FROM estadisticas_entrenamiento WHERE numeroEntrenamientos > 0 ORDER BY fecha DESC")
    suspend fun getFechasEntrenamiento(): List<Long>

    /**
     * Obtiene el volumen total del mes.
     */
    @Query("SELECT COALESCE(SUM(volumenTotal), 0) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes")
    suspend fun getVolumenTotalMes(anio: Int, mes: Int): Float

    /**
     * Obtiene el volumen total (peso movido) de un día específico.
     */
    @Query("SELECT COALESCE(SUM(volumenTotal), 0) FROM estadisticas_entrenamiento WHERE anio = :anio AND mes = :mes AND dia = :dia")
    suspend fun getVolumenTotalDia(anio: Int, mes: Int, dia: Int): Float

    /**
     * Obtiene el récord de volumen máximo en un solo día.
     */
    @Query("SELECT COALESCE(MAX(volumenTotal), 0) FROM estadisticas_entrenamiento")
    suspend fun getRecordVolumenDia(): Float

    /**
     * Obtiene el resumen mensual de todo un año (para gráficos anuales).
     */
    @Query("""
        SELECT mes, 
               SUM(tiempoEntrenamientoMs) as tiempoTotal, 
               SUM(numeroEntrenamientos) as entrenamientos,
               SUM(volumenTotal) as volumen
        FROM estadisticas_entrenamiento 
        WHERE anio = :anio 
        GROUP BY mes 
        ORDER BY mes ASC
    """)
    suspend fun getResumenAnual(anio: Int): List<ResumenMensual>
}

/**
 * Clase para el resumen mensual (usado en consultas de estadísticas anuales).
 */
data class ResumenMensual(
    val mes: Int,
    val tiempoTotal: Long,
    val entrenamientos: Int,
    val volumen: Float
)

