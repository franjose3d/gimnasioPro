package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar las asignaciones de rutinas a días de la semana.
 */
class RutinaDiaSemanaRepository(private val dao: RutinaDiaSemanaDao) {

    /**
     * Obtiene todas las asignaciones de rutinas a días de la semana.
     */
    fun getAllAsignaciones(): Flow<List<RutinaDiaSemana>> = dao.getAllAsignaciones()

    /**
     * Obtiene la rutina asignada a un día de la semana específico.
     */
    fun getAsignacionPorDia(diaSemana: Int): Flow<RutinaDiaSemana?> =
        dao.getAsignacionPorDia(diaSemana)

    /**
     * Obtiene la rutina asignada a un día de la semana de forma síncrona.
     */
    suspend fun getAsignacionPorDiaSync(diaSemana: Int): RutinaDiaSemana? =
        dao.getAsignacionPorDiaSync(diaSemana)

    /**
     * Obtiene el número de rutina asignada a un día de la semana.
     */
    suspend fun getNumeroRutinaPorDia(diaSemana: Int): Int? =
        dao.getNumeroRutinaPorDia(diaSemana)

    /**
     * Asigna una rutina a un día de la semana.
     * @param diaSemana Día de la semana (1=Lunes, 7=Domingo)
     * @param numeroRutina Número de la rutina a asignar
     */
    suspend fun asignarRutinaADia(diaSemana: Int, numeroRutina: Int) {
        dao.insertAsignacion(
            RutinaDiaSemana(
                diaSemana = diaSemana,
                numeroRutina = numeroRutina,
                fechaModificacion = System.currentTimeMillis()
            )
        )
    }

    /**
     * Elimina la asignación de rutina de un día de la semana (long press).
     */
    suspend fun eliminarRutinaDia(diaSemana: Int) {
        dao.deleteAsignacion(diaSemana)
    }

    /**
     * Elimina la asignación de rutina de un día de la semana.
     */
    suspend fun eliminarAsignacion(diaSemana: Int) {
        dao.deleteAsignacion(diaSemana)
    }

    /**
     * Elimina todas las asignaciones de una rutina específica.
     * Útil cuando se elimina una rutina.
     */
    suspend fun clearAsignacionesPorRutina(numeroRutina: Int) {
        dao.clearAsignacionesPorRutina(numeroRutina)
    }
}