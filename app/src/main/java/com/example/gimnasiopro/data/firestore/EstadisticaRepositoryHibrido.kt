package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.example.gimnasiopro.data.EstadisticaEntrenamiento
import com.example.gimnasiopro.data.EstadisticaRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Repositorio híbrido para estadísticas que sincroniza datos locales y de Firestore.
 * Usa Room como cache local y sincroniza con Firestore cuando hay conexión.
 */
class EstadisticaRepositoryHibrido(
    private val localRepository: EstadisticaRepository
) {
    companion object {
        private const val TAG = "EstadisticaHibrido"
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    // Cache del tipo de usuario para no verificar cada vez
    private var tipoUsuarioCache: String? = null

    /**
     * Determina si el usuario está registrado como trainer o cliente.
     * Retorna null si no existe en ninguna colección.
     */
    private suspend fun determinarTipoUsuario(userId: String): String? {
        // Usar cache si ya se determinó
        tipoUsuarioCache?.let { return it }

        try {
            // Verificar primero si es trainer
            val trainerDoc = firestore.collection("trainers").document(userId).get().await()
            if (trainerDoc.exists()) {
                tipoUsuarioCache = "trainer"
                Log.d(TAG, "✅ Usuario $userId identificado como TRAINER")
                return "trainer"
            }

            // Verificar si es cliente
            val clienteDoc = firestore.collection("clientes").document(userId).get().await()
            if (clienteDoc.exists()) {
                tipoUsuarioCache = "cliente"
                Log.d(TAG, "✅ Usuario $userId identificado como CLIENTE")
                return "cliente"
            }

            Log.w(TAG, "⚠️ Usuario $userId no encontrado en trainers ni clientes")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al determinar tipo de usuario: ${e.message}")
            return null
        }
    }

    private suspend fun getFirestoreRepository(): EstadisticaFirestoreRepository? {
        val userId = auth.currentUser?.uid ?: return null
        val tipoUsuario = determinarTipoUsuario(userId) ?: return null
        return EstadisticaFirestoreRepository(userId, tipoUsuario)
    }

    /**
     * Registra un entrenamiento guardando en local Y en Firestore
     */
    suspend fun registrarEntrenamiento(
        tiempoMs: Long,
        ejerciciosCompletados: Int,
        volumenTotal: Float,
        rutinaId: Int
    ) {
        Log.d(TAG, "📊 Registrando entrenamiento: tiempo=${tiempoMs}ms, ejercicios=$ejerciciosCompletados, volumen=$volumenTotal, rutina=$rutinaId")

        // 1. Siempre guardar en local primero (cache inmediato)
        try {
            localRepository.registrarEntrenamiento(tiempoMs, ejerciciosCompletados, volumenTotal, rutinaId)
            Log.d(TAG, "✅ Estadística guardada localmente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar estadística local: ${e.message}", e)
        }

        // 2. Intentar sincronizar con Firestore
        try {
            val firestoreRepo = getFirestoreRepository()
            if (firestoreRepo != null) {
                val resultado = firestoreRepo.agregarEntrenamientoAEstadistica(
                    fecha = Date(),
                    tiempoMs = tiempoMs,
                    ejerciciosCompletados = ejerciciosCompletados,
                    volumenTotal = volumenTotal,
                    rutinaId = rutinaId.toString()
                )
                resultado.fold(
                    onSuccess = { Log.d(TAG, "✅ Estadística sincronizada con Firestore") },
                    onFailure = { Log.e(TAG, "❌ Error al sincronizar con Firestore: ${it.message}") }
                )
            } else {
                Log.w(TAG, "⚠️ Usuario no autenticado, estadística solo guardada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al sincronizar estadística con Firestore: ${e.message}")
            // No lanzar excepción, los datos están guardados localmente
        }
    }

    // ==================== Métodos de consulta (usan local) ====================

    /**
     * Obtiene el tiempo de entrenamiento de hoy.
     */
    suspend fun getTiempoHoy(): Long {
        val tiempo = localRepository.getTiempoHoy()
        Log.d(TAG, "📈 getTiempoHoy: ${tiempo}ms")
        return tiempo
    }

    /**
     * Obtiene el tiempo total del mes actual.
     */
    suspend fun getTiempoMesActual(): Long {
        val tiempo = localRepository.getTiempoMesActual()
        Log.d(TAG, "📈 getTiempoMesActual: ${tiempo}ms")
        return tiempo
    }

    /**
     * Obtiene el tiempo total del año actual.
     */
    suspend fun getTiempoAnioActual(): Long {
        val tiempo = localRepository.getTiempoAnioActual()
        Log.d(TAG, "📈 getTiempoAnioActual: ${tiempo}ms")
        return tiempo
    }

    /**
     * Obtiene el número de entrenamientos del mes actual.
     */
    suspend fun getEntrenamientosMesActual(): Int {
        val entrenamientos = localRepository.getEntrenamientosMesActual()
        Log.d(TAG, "📈 getEntrenamientosMesActual: $entrenamientos")
        return entrenamientos
    }

    /**
     * Calcula la racha actual de días consecutivos entrenando.
     */
    suspend fun getRachaActual(): Int {
        val racha = localRepository.getRachaActual()
        Log.d(TAG, "📈 getRachaActual: $racha días")
        return racha
    }

    /**
     * Obtiene el volumen total (peso movido) de hoy.
     */
    suspend fun getVolumenHoy(): Float {
        val volumen = localRepository.getVolumenHoy()
        Log.d(TAG, "📈 getVolumenHoy: ${volumen} kg")
        return volumen
    }

    /**
     * Obtiene el récord de volumen máximo en un solo día.
     */
    suspend fun getRecordVolumen(): Float {
        val record = localRepository.getRecordVolumen()
        Log.d(TAG, "📈 getRecordVolumen: ${record} kg")
        return record
    }


    /**
     * Obtiene las estadísticas del año actual como Flow.
     */
    fun getEstadisticasAnioActual(): Flow<List<EstadisticaEntrenamiento>> {
        return localRepository.getEstadisticasAnioActual()
    }

    /**
     * Obtiene las estadísticas del mes actual como Flow.
     */
    fun getEstadisticasMesActual(): Flow<List<EstadisticaEntrenamiento>> {
        return localRepository.getEstadisticasMesActual()
    }

    /**
     * Sincroniza estadísticas desde Firestore hacia local (para nuevos dispositivos)
     */
    suspend fun sincronizarDesdeFirestore(): Result<Int> {
        return try {
            val firestoreRepo = getFirestoreRepository()
            if (firestoreRepo == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val calendar = Calendar.getInstance()
            val anioActual = calendar.get(Calendar.YEAR)

            // Obtener estadísticas del año actual desde Firestore
            val estadisticasFirestore = firestoreRepo.getEstadisticasPorAnio(anioActual).first()

            var contadorSincronizados = 0
            estadisticasFirestore.forEach { estadisticaFirestore ->
                try {
                    // Convertir a modelo local y guardar
                    val estadisticaLocal = EstadisticaEntrenamiento(
                        fecha = estadisticaFirestore.fechaTimestamp.time,
                        anio = estadisticaFirestore.anio,
                        mes = estadisticaFirestore.mes,
                        dia = estadisticaFirestore.dia,
                        tiempoEntrenamientoMs = estadisticaFirestore.tiempoEntrenamientoMs,
                        numeroEntrenamientos = estadisticaFirestore.numeroEntrenamientos,
                        ejerciciosCompletados = estadisticaFirestore.ejerciciosCompletados,
                        volumenTotal = estadisticaFirestore.volumenTotal,
                        rutinasUsadas = estadisticaFirestore.rutinasUsadas.joinToString(",")
                    )
                    localRepository.insertOrUpdateEstadistica(estadisticaLocal)
                    contadorSincronizados++
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando estadística: ${e.message}")
                }
            }

            Log.d(TAG, "✅ Sincronizadas $contadorSincronizados estadísticas desde Firestore")
            Result.success(contadorSincronizados)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando desde Firestore: ${e.message}")
            Result.failure(e)
        }
    }
}

