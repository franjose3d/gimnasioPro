package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar los registros de entrenamiento.
 * Proporciona métodos para persistir y recuperar datos de entrenamientos.
 */
class RegistroEntrenamientoRepository(private val registroDao: RegistroEntrenamientoDao) {

    /**
     * Guarda un registro de entrenamiento.
     */
    suspend fun guardarRegistro(registro: RegistroEntrenamiento): Long {
        return registroDao.insertRegistro(registro)
    }

    /**
     * Guarda múltiples registros de entrenamiento (al finalizar una sesión).
     */
    suspend fun guardarRegistros(registros: List<RegistroEntrenamiento>) {
        registroDao.insertRegistros(registros)
    }

    /**
     * Obtiene el último registro de un ejercicio en una rutina.
     * Útil para precargar los valores anteriores.
     */
    suspend fun getUltimoRegistro(rutinaId: Int, ejercicioId: Long): RegistroEntrenamiento? {
        return registroDao.getUltimoRegistro(rutinaId, ejercicioId)
    }

    /**
     * Obtiene los últimos registros de todos los ejercicios de una rutina.
     * Devuelve un mapa de ejercicioId -> RegistroEntrenamiento para fácil acceso.
     */
    suspend fun getUltimosRegistrosDeRutina(rutinaId: Int): Map<Long, RegistroEntrenamiento> {
        return registroDao.getUltimosRegistrosDeRutina(rutinaId)
            .associateBy { it.ejercicioId }
    }

    /**
     * Obtiene el historial completo de un ejercicio.
     */
    fun getHistorialEjercicio(ejercicioId: Long): Flow<List<RegistroEntrenamiento>> {
        return registroDao.getHistorialEjercicio(ejercicioId)
    }

    /**
     * Obtiene el historial de una rutina.
     */
    fun getHistorialRutina(rutinaId: Int): Flow<List<RegistroEntrenamiento>> {
        return registroDao.getHistorialRutina(rutinaId)
    }

    /**
     * Obtiene registros entre dos fechas (para gráficos de progreso).
     */
    suspend fun getRegistrosEntreFechas(fechaInicio: Long, fechaFin: Long): List<RegistroEntrenamiento> {
        return registroDao.getRegistrosEntreFechas(fechaInicio, fechaFin)
    }

    /**
     * Obtiene los IDs de ejercicios completados en un rango de fechas.
     */
    suspend fun getEjerciciosCompletadosEntreFechas(fechaInicio: Long, fechaFin: Long): List<Long> {
        return registroDao.getEjerciciosCompletadosEntreFechas(fechaInicio, fechaFin)
    }

    // ==================== Métodos para Estadísticas/Progreso ====================

    /**
     * Obtiene el total de días entrenados.
     */
    suspend fun getTotalDiasEntrenados(): Int {
        return registroDao.getTotalDiasEntrenados()
    }

    /**
     * Obtiene el peso máximo levantado en un ejercicio.
     */
    suspend fun getPesoMaximoEjercicio(ejercicioId: Long): Float {
        return registroDao.getPesoMaximoEjercicio(ejercicioId) ?: 0f
    }

    /**
     * Obtiene las repeticiones máximas en un ejercicio.
     */
    suspend fun getRepeticionesMaximasEjercicio(ejercicioId: Long): Int {
        return registroDao.getRepeticionesMaximasEjercicio(ejercicioId) ?: 0
    }

    /**
     * Calcula el volumen total de un ejercicio (peso x repeticiones) en un período.
     */
    suspend fun calcularVolumenEjercicio(ejercicioId: Long, fechaInicio: Long, fechaFin: Long): Float {
        val registros = registroDao.getRegistrosEntreFechas(fechaInicio, fechaFin)
            .filter { it.ejercicioId == ejercicioId && it.completado }

        return registros.sumOf { (it.pesoKg * it.repeticiones).toDouble() }.toFloat()
    }

    /**
     * Obtiene el progreso de peso en un ejercicio (comparando primer y último registro).
     */
    suspend fun calcularProgresoEjercicio(ejercicioId: Long): ProgresoEjercicio {
        // Obtener el primer y último registro completado del ejercicio
        val primerRegistro = registroDao.getPrimerRegistroEjercicio(ejercicioId)
        val ultimoRegistro = registroDao.getUltimoRegistroEjercicio(ejercicioId)

        if (primerRegistro == null || ultimoRegistro == null) {
            return ProgresoEjercicio(
                ejercicioId = ejercicioId,
                pesoInicial = 0f,
                pesoActual = 0f,
                repeticionesIniciales = 0,
                repeticionesActuales = 0,
                porcentajeMejora = 0f
            )
        }

        val pesoInicial = primerRegistro.pesoKg
        val pesoActual = ultimoRegistro.pesoKg

        // Calcular porcentaje de mejora en peso
        val porcentajeMejora = if (pesoInicial > 0f) {
            ((pesoActual - pesoInicial) / pesoInicial) * 100f
        } else {
            0f
        }

        return ProgresoEjercicio(
            ejercicioId = ejercicioId,
            pesoInicial = pesoInicial,
            pesoActual = pesoActual,
            repeticionesIniciales = primerRegistro.repeticiones,
            repeticionesActuales = ultimoRegistro.repeticiones,
            porcentajeMejora = porcentajeMejora
        )
    }
}

/**
 * Clase para representar el progreso de un ejercicio.
 */
data class ProgresoEjercicio(
    val ejercicioId: Long,
    val pesoInicial: Float,
    val pesoActual: Float,
    val repeticionesIniciales: Int,
    val repeticionesActuales: Int,
    val porcentajeMejora: Float
)

