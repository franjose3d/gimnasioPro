package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repositorio para gestionar las estadísticas de entrenamiento.
 */
class EstadisticaRepository(private val estadisticaDao: EstadisticaEntrenamientoDao) {

    /**
     * Registra o actualiza las estadísticas de un entrenamiento.
     */
    suspend fun registrarEntrenamiento(
        tiempoMs: Long,
        ejerciciosCompletados: Int,
        volumenTotal: Float,
        rutinaId: Int
    ) {
        val calendar = Calendar.getInstance()
        val anio = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH es 0-based
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        // Obtener timestamp del inicio del día
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val fechaInicioDia = calendar.timeInMillis

        // Verificar si ya existe una estadística para hoy
        val estadisticaExistente = estadisticaDao.getEstadisticaHoy(anio, mes, dia)

        if (estadisticaExistente != null) {
            // Actualizar la existente
            val rutinasActualizadas = actualizarRutinasUsadas(estadisticaExistente.rutinasUsadas, rutinaId)
            val estadisticaActualizada = estadisticaExistente.copy(
                tiempoEntrenamientoMs = estadisticaExistente.tiempoEntrenamientoMs + tiempoMs,
                numeroEntrenamientos = estadisticaExistente.numeroEntrenamientos + 1,
                ejerciciosCompletados = estadisticaExistente.ejerciciosCompletados + ejerciciosCompletados,
                volumenTotal = estadisticaExistente.volumenTotal + volumenTotal,
                rutinasUsadas = rutinasActualizadas
            )
            estadisticaDao.updateEstadistica(estadisticaActualizada)
        } else {
            // Crear nueva
            val nuevaEstadistica = EstadisticaEntrenamiento(
                fecha = fechaInicioDia,
                anio = anio,
                mes = mes,
                dia = dia,
                tiempoEntrenamientoMs = tiempoMs,
                numeroEntrenamientos = 1,
                ejerciciosCompletados = ejerciciosCompletados,
                volumenTotal = volumenTotal,
                rutinasUsadas = rutinaId.toString()
            )
            estadisticaDao.insertEstadistica(nuevaEstadistica)
        }
    }

    private fun actualizarRutinasUsadas(rutinasActuales: String, nuevaRutina: Int): String {
        val rutinas = if (rutinasActuales.isBlank()) {
            mutableSetOf()
        } else {
            rutinasActuales.split(",").map { it.trim() }.toMutableSet()
        }
        rutinas.add(nuevaRutina.toString())
        return rutinas.joinToString(",")
    }

    // ==================== Métodos de consulta ====================

    /**
     * Obtiene el tiempo de entrenamiento de hoy.
     */
    suspend fun getTiempoHoy(): Long {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getTiempoTotalDia(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Obtiene el tiempo total del mes actual.
     */
    suspend fun getTiempoMesActual(): Long {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getTiempoTotalMes(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Obtiene el tiempo total del año actual.
     */
    suspend fun getTiempoAnioActual(): Long {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getTiempoTotalAnio(calendar.get(Calendar.YEAR))
    }

    /**
     * Obtiene el número de entrenamientos del mes actual.
     */
    suspend fun getEntrenamientosMesActual(): Int {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getNumeroEntrenamientosMes(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Calcula la racha actual de días consecutivos entrenando.
     */
    suspend fun getRachaActual(): Int {
        val fechasEntrenamiento = estadisticaDao.getFechasEntrenamiento()
        if (fechasEntrenamiento.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        // Establecer a inicio del día actual
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val hoy = calendar.timeInMillis
        val ayer = hoy - 86400000L // 24 horas en milisegundos

        // Verificar si el último entrenamiento fue hoy o ayer
        val ultimaFecha = fechasEntrenamiento.firstOrNull() ?: return 0
        if (ultimaFecha != hoy && ultimaFecha != ayer) {
            return 0 // La racha se rompió
        }

        var racha = 0
        var fechaEsperada = if (ultimaFecha == hoy) hoy else ayer

        for (fecha in fechasEntrenamiento) {
            if (fecha == fechaEsperada) {
                racha++
                fechaEsperada -= 86400000L // Ir al día anterior
            } else if (fecha < fechaEsperada) {
                // Hubo un hueco, la racha termina
                break
            }
        }

        return racha
    }

    /**
     * Obtiene las estadísticas del mes como Flow.
     */
    fun getEstadisticasMesActual(): Flow<List<EstadisticaEntrenamiento>> {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getEstadisticasMes(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Obtiene las estadísticas del año como Flow.
     */
    fun getEstadisticasAnioActual(): Flow<List<EstadisticaEntrenamiento>> {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getEstadisticasAnio(calendar.get(Calendar.YEAR))
    }

    /**
     * Obtiene el volumen total (peso movido) de hoy.
     */
    suspend fun getVolumenHoy(): Float {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getVolumenTotalDia(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Obtiene el récord de volumen máximo en un solo día.
     */
    suspend fun getRecordVolumen(): Float {
        return estadisticaDao.getRecordVolumenDia()
    }

    /**
     * Obtiene el resumen mensual del año (para gráficos).
     */
    suspend fun getResumenAnual(): List<ResumenMensual> {
        val calendar = Calendar.getInstance()
        return estadisticaDao.getResumenAnual(calendar.get(Calendar.YEAR))
    }

    /**
     * Inserta o actualiza una estadística (usado para sincronización desde Firestore).
     */
    suspend fun insertOrUpdateEstadistica(estadistica: EstadisticaEntrenamiento) {
        val existente = estadisticaDao.getEstadisticaHoy(
            estadistica.anio,
            estadistica.mes,
            estadistica.dia
        )
        if (existente != null) {
            // Solo actualizar si los datos de Firestore son más recientes o completos
            if (estadistica.numeroEntrenamientos >= existente.numeroEntrenamientos) {
                estadisticaDao.updateEstadistica(estadistica.copy(id = existente.id))
            }
        } else {
            estadisticaDao.insertEstadistica(estadistica)
        }
    }

    // ==================== Utilidades de formato ====================

    companion object {
        /**
         * Formatea milisegundos a formato "Xh Xmin".
         */
        fun formatearTiempo(tiempoMs: Long): String {
            val totalMinutos = tiempoMs / 60000
            val horas = totalMinutos / 60
            val minutos = totalMinutos % 60
            return "${horas}h ${minutos}min"
        }
    }
}

