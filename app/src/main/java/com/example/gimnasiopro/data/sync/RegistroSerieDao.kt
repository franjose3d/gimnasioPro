package com.example.gimnasiopro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RegistroSerieDao {

    @Insert
    suspend fun insertAll(series: List<RegistroSerie>)

    @Query("""
        SELECT * FROM registros_series 
        WHERE registroEntrenamientoId = :registroId 
        ORDER BY numeroSerie ASC
    """)
    suspend fun getSeriesByRegistroId(registroId: Long): List<RegistroSerie>

    @Query("""
        SELECT rs.* FROM registros_series rs
        INNER JOIN registros_entrenamiento re ON rs.registroEntrenamientoId = re.id
        WHERE re.ejercicioId = :ejercicioId 
          AND re.rutinaId = :rutinaId
          AND re.id = (
              SELECT id FROM registros_entrenamiento
              WHERE ejercicioId = :ejercicioId 
                AND rutinaId = :rutinaId
              ORDER BY fechaEntrenamiento DESC
              LIMIT 1
          )
        ORDER BY rs.numeroSerie ASC
        LIMIT :numeroSeries
    """)
    suspend fun getUltimasSeriesDeEjercicio(
        ejercicioId: Long,
        rutinaId: Int,
        numeroSeries: Int
    ): List<RegistroSerie>

    @Query("""
        SELECT rs.* FROM registros_series rs
        INNER JOIN registros_entrenamiento re ON rs.registroEntrenamientoId = re.id
        WHERE re.ejercicioId = :ejercicioId
          AND re.id = (
              SELECT id FROM registros_entrenamiento
              WHERE ejercicioId = :ejercicioId
              ORDER BY fechaEntrenamiento DESC
              LIMIT 1
          )
        ORDER BY rs.numeroSerie ASC
        LIMIT :numeroSeries
    """)
    suspend fun getUltimasSeriesDeEjercicioGlobal(
        ejercicioId: Long,
        numeroSeries: Int
    ): List<RegistroSerie>

    @Query("""
        SELECT MAX(rs.pesoKg) FROM registros_series rs
        INNER JOIN registros_entrenamiento re ON rs.registroEntrenamientoId = re.id
        WHERE re.ejercicioId = :ejercicioId
    """)
    suspend fun getPesoMaximoSerie(ejercicioId: Long): Float?

    @Query("""
        SELECT MAX(rs.repeticiones) FROM registros_series rs
        INNER JOIN registros_entrenamiento re ON rs.registroEntrenamientoId = re.id
        WHERE re.ejercicioId = :ejercicioId
    """)
    suspend fun getRepeticionesMaximasSerie(ejercicioId: Long): Int?

    @Query("""
        DELETE FROM registros_series
        WHERE registroEntrenamientoId IN (
            SELECT id FROM registros_entrenamiento
            WHERE ejercicioId = :ejercicioId
        )
    """)
    suspend fun deleteSeriesByEjercicioId(ejercicioId: Long)
}