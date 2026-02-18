package com.example.gimnasiopro.data.firestore

import com.example.gimnasiopro.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

/**
 * Helper para migrar datos desde Room a Firestore
 */
class FirestoreMigrationHelper(
    private val ejercicioRepository: EjercicioRepository,
    private val rutinaRepository: RutinaRepository,
    private val registroEntrenamientoRepository: RegistroEntrenamientoRepository,
    private val rutinaDiaSemanaRepository: RutinaDiaSemanaRepository,
    private val estadisticaRepository: EstadisticaRepository
) {

    private val ejercicioFirestoreRepo = EjercicioFirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Migrar todos los ejercicios desde Room a Firestore
     * Solo se debe ejecutar una vez
     */
    suspend fun migrarEjercicios(): Result<Int> {
        return try {
            val ejercicios = ejercicioRepository.allEjercicios.first()
            ejercicioFirestoreRepo.migrarEjerciciosIniciales(ejercicios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar rutinas de un usuario desde Room a Firestore
     */
    suspend fun migrarRutinasUsuario(userId: String): Result<Int> {
        return try {
            val rutinas = rutinaRepository.getAllRutinas().first()
            val tipoUsuario = UserHelper.getTipoUsuario(userId)
            val rutinaFirestoreRepo = RutinaFirestoreRepository(userId, tipoUsuario)

            var count = 0
            rutinas.forEach { rutina ->
                val rutinaFirestore = rutina.toFirestore()
                rutinaFirestoreRepo.crearRutina(rutinaFirestore).fold(
                    onSuccess = { count++ },
                    onFailure = { /* Log error */ }
                )
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar calendario de un usuario desde Room a Firestore
     */
    suspend fun migrarCalendarioUsuario(userId: String): Result<Int> {
        return try {
            val calendario = rutinaDiaSemanaRepository.getAllAsignaciones().first()
            val calendarioFirestoreRepo = CalendarioFirestoreRepository(userId)
            
            var count = 0
            calendario.forEach { rutinaDia ->
                val calendarioFirestore = CalendarioFirestore(
                    diaSemana = rutinaDia.diaSemana,
                    rutinaId = rutinaDia.numeroRutina?.toString(), // Convertir a String
                    activo = true,
                    fechaModificacion = java.util.Date(rutinaDia.fechaModificacion)
                )
                calendarioFirestoreRepo.asignarRutinaADia(
                    rutinaDia.diaSemana,
                    calendarioFirestore.rutinaId
                ).fold(
                    onSuccess = { count++ },
                    onFailure = { /* Log error */ }
                )
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar entrenamientos de un usuario desde Room a Firestore
     */
    suspend fun migrarEntrenamientosUsuario(userId: String): Result<Int> {
        return try {
            // Obtener todos los registros usando el DAO directamente o un método que devuelva List
            // Por ahora, obtenemos registros de los últimos 10 años
            val fechaInicio = System.currentTimeMillis() - (10L * 365 * 24 * 60 * 60 * 1000)
            val fechaFin = System.currentTimeMillis()
            val registros = registroEntrenamientoRepository.getRegistrosEntreFechas(fechaInicio, fechaFin)
            val tipoUsuario = UserHelper.getTipoUsuario(userId)
            val entrenamientoFirestoreRepo = EntrenamientoFirestoreRepository(userId)
            val estadisticaFirestoreRepo = EstadisticaFirestoreRepository(userId, tipoUsuario)

            // Agrupar registros por fecha de entrenamiento
            val entrenamientosPorFecha = registros.groupBy { 
                java.util.Date(it.fechaEntrenamiento)
            }
            
            var count = 0
            entrenamientosPorFecha.forEach { (fecha, registrosDelDia) ->
                // Agrupar por ejercicio
                val ejerciciosPorId = registrosDelDia.groupBy { it.ejercicioId.toString() }
                
                val ejerciciosEntrenamiento = ejerciciosPorId.map { (ejercicioId, registros) ->
                    val series = registros.map { registro ->
                        SerieFirestore(
                            pesoKg = registro.pesoKg,
                            repeticiones = registro.repeticiones,
                            completado = registro.completado
                        )
                    }
                    EjercicioEntrenamientoFirestore(
                        ejercicioId = ejercicioId,
                        series = series,
                        completado = registros.any { it.completado }
                    )
                }
                
                val volumenTotal = registros.sumOf { 
                    (it.pesoKg * it.repeticiones).toDouble() 
                }.toFloat()
                
                val entrenamiento = EntrenamientoFirestore(
                    entrenamientoId = "",
                    rutinaId = registrosDelDia.firstOrNull()?.rutinaId?.toString(),
                    fechaEntrenamiento = fecha,
                    ejercicios = ejerciciosEntrenamiento,
                    tiempoEntrenamientoMs = 0, // No disponible en Room
                    volumenTotal = volumenTotal
                )
                
                entrenamientoFirestoreRepo.guardarEntrenamiento(entrenamiento).fold(
                    onSuccess = { 
                        count++
                        // Actualizar estadísticas
                        estadisticaFirestoreRepo.agregarEntrenamientoAEstadistica(
                            fecha = fecha,
                            tiempoMs = 0,
                            ejerciciosCompletados = ejerciciosEntrenamiento.count { it.completado },
                            volumenTotal = volumenTotal,
                            rutinaId = entrenamiento.rutinaId
                        )
                    },
                    onFailure = { /* Log error */ }
                )
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar todas las estadísticas de un usuario desde Room a Firestore
     */
    suspend fun migrarEstadisticasUsuario(userId: String): Result<Int> {
        return try {
            // Obtener estadísticas del año actual y anteriores
            val calendar = java.util.Calendar.getInstance()
            val anioActual = calendar.get(java.util.Calendar.YEAR)
            val estadisticas = estadisticaRepository.getEstadisticasAnioActual().first()
            val tipoUsuario = UserHelper.getTipoUsuario(userId)
            val estadisticaFirestoreRepo = EstadisticaFirestoreRepository(userId, tipoUsuario)

            var count = 0
            estadisticas.forEach { estadistica ->
                val fecha = java.util.Date(estadistica.fecha)
                val estadisticaFirestore = EstadisticaFirestore(
                    fecha = EstadisticaFirestore.formatFecha(fecha),
                    fechaTimestamp = fecha,
                    anio = estadistica.anio,
                    mes = estadistica.mes,
                    dia = estadistica.dia,
                    tiempoEntrenamientoMs = estadistica.tiempoEntrenamientoMs,
                    numeroEntrenamientos = estadistica.numeroEntrenamientos,
                    ejerciciosCompletados = estadistica.ejerciciosCompletados,
                    volumenTotal = estadistica.volumenTotal,
                    rutinasUsadas = estadistica.rutinasUsadas.split(",").filter { it.isNotEmpty() }
                )
                estadisticaFirestoreRepo.guardarEstadistica(estadisticaFirestore).fold(
                    onSuccess = { count++ },
                    onFailure = { /* Log error */ }
                )
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar todos los datos de un usuario desde Room a Firestore
     */
    suspend fun migrarTodosLosDatosUsuario(userId: String): Result<MigrationResult> {
        return try {
            val rutinasResult = migrarRutinasUsuario(userId)
            val calendarioResult = migrarCalendarioUsuario(userId)
            val entrenamientosResult = migrarEntrenamientosUsuario(userId)
            val estadisticasResult = migrarEstadisticasUsuario(userId)
            
            Result.success(
                MigrationResult(
                    rutinasMigradas = rutinasResult.getOrNull() ?: 0,
                    calendarioMigrado = calendarioResult.getOrNull() ?: 0,
                    entrenamientosMigrados = entrenamientosResult.getOrNull() ?: 0,
                    estadisticasMigradas = estadisticasResult.getOrNull() ?: 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class MigrationResult(
        val rutinasMigradas: Int,
        val calendarioMigrado: Int,
        val entrenamientosMigrados: Int,
        val estadisticasMigradas: Int
    )
}

/**
 * Extension functions para convertir modelos de Room a Firestore
 */
fun Rutina.toFirestore(): RutinaFirestore {
    return RutinaFirestore(
        rutinaId = "",
        nombre = this.nombre,
        ejercicioIds = this.ejercicioIds.map { it.toString() },
        fechaCreacion = java.util.Date(this.fechaCreacion),
        fechaModificacion = java.util.Date(this.fechaModificacion),
        creadoPorId = "",
        creadoPorTipo = "cliente"
    )
}

fun Ejercicio.toFirestore(): EjercicioFirestore {
    return EjercicioFirestore(
        id = this.id.toString(),
        grupoMuscular = this.grupoMuscular,
        nombre = this.nombre,
        descripcion = this.descripcion,
        imagenUrl = this.imagenUrl,
        esPredefinido = true,
        fechaCreacion = java.util.Date()
    )
}
