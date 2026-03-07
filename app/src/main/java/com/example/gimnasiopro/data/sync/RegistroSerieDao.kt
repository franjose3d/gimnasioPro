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
        ORDER BY re.fechaEntrenamiento DESC
        LIMIT :numeroSeries
    """)
    suspend fun getUltimasSeriesDeEjercicio(
        ejercicioId: Long,
        rutinaId: Int,
        numeroSeries: Int
    ): List<RegistroSerie>
}