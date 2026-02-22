package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.example.gimnasiopro.data.EstadisticaEntrenamiento
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.sync.SyncManager
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
    private var lastUserId: String? = null // Para invalidar cache si cambia el usuario

    /**
     * Invalida el cache del tipo de usuario.
     * Útil si se crea el documento del usuario después de la primera verificación.
     */
    fun invalidarCache() {
        tipoUsuarioCache = null
        lastUserId = null
        Log.d(TAG, "🗑️ Cache de tipo de usuario invalidado")
    }

    /**
     * Determina si el usuario está registrado como trainer o cliente.
     * Usa UserHelper que automáticamente crea el documento de cliente si no existe.
     */
    private suspend fun determinarTipoUsuario(userId: String): String? {
        // Verificar si el userId cambió para invalidar cache
        if (lastUserId != null && lastUserId != userId) {
            Log.d(TAG, "🔄 Usuario cambió de $lastUserId a $userId - invalidando cache")
            tipoUsuarioCache = null
        }
        lastUserId = userId

        // Usar cache si ya se determinó
        tipoUsuarioCache?.let {
            Log.d(TAG, "📋 Usando tipo de usuario cacheado: $it para userId: $userId")
            return it
        }

        Log.d(TAG, "🔍 Determinando tipo de usuario para: $userId usando UserHelper")

        try {
            // UserHelper.getTipoUsuario CREA el documento de cliente si no existe
            val tipo = UserHelper.getTipoUsuario(userId)
            tipoUsuarioCache = tipo
            Log.d(TAG, "✅ Tipo de usuario determinado: $tipo para userId: $userId")
            return tipo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al determinar tipo de usuario: ${e.message}", e)
            return null
        }
    }

    private suspend fun getFirestoreRepository(): EstadisticaFirestoreRepository? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ No hay usuario autenticado en Firebase Auth")
            return null
        }

        Log.d(TAG, "🔐 Usuario autenticado: $userId")

        val tipoUsuario = determinarTipoUsuario(userId)
        if (tipoUsuario == null) {
            Log.w(TAG, "⚠️ No se pudo determinar tipo de usuario para: $userId")
            return null
        }

        Log.d(TAG, "✅ Creando EstadisticaFirestoreRepository para $tipoUsuario: $userId")
        return EstadisticaFirestoreRepository(userId, tipoUsuario)
    }

    /**
     * Registra un entrenamiento guardando en local Y en Firestore.
     *
     * IMPORTANTE: Respeta el SyncManager:
     * - SIEMPRE guarda en local (Room) primero
     * - Solo sincroniza con Firestore si SyncManager lo permite
     * - En modo entrenamiento, solo guarda localmente
     */
    suspend fun registrarEntrenamiento(
        tiempoMs: Long,
        ejerciciosCompletados: Int,
        volumenTotal: Float,
        rutinaId: Int
    ) {
        Log.d(TAG, "📊 Registrando entrenamiento: tiempo=${tiempoMs}ms, ejercicios=$ejerciciosCompletados, volumen=$volumenTotal, rutina=$rutinaId")

        // 1. SIEMPRE guardar en local primero (cache inmediato - funciona offline)
        try {
            localRepository.registrarEntrenamiento(tiempoMs, ejerciciosCompletados, volumenTotal, rutinaId)
            Log.d(TAG, "✅ Estadística guardada localmente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar estadística local: ${e.message}", e)
        }

        // 2. Solo sincronizar con Firestore si está habilitado
        if (SyncManager.isTrainingMode) {
            Log.d(TAG, "🏋️ Modo entrenamiento - sincronización con Firestore OMITIDA (se hará al finalizar)")
            return
        }

        // Verificar si es PENDING_SYNC o NORMAL para sincronizar
        val syncEnabled = SyncManager.isSyncEnabled
        Log.d(TAG, "🔄 Estado SyncManager: syncEnabled=$syncEnabled, isTrainingMode=${SyncManager.isTrainingMode}")

        if (!syncEnabled) {
            Log.d(TAG, "⏸️ Sincronización deshabilitada - estadística guardada solo localmente")
            return
        }

        // 3. Intentar sincronizar con Firestore
        Log.d(TAG, "☁️ Iniciando sincronización con Firestore...")
        sincronizarConFirestore(tiempoMs, ejerciciosCompletados, volumenTotal, rutinaId)
    }

    /**
     * Sincroniza estadísticas con Firestore.
     * Este método puede ser llamado directamente para forzar sincronización.
     */
    suspend fun sincronizarConFirestore(
        tiempoMs: Long,
        ejerciciosCompletados: Int,
        volumenTotal: Float,
        rutinaId: Int
    ) {
        Log.d(TAG, "☁️ sincronizarConFirestore iniciado - tiempo=$tiempoMs, ejercicios=$ejerciciosCompletados, volumen=$volumenTotal, rutina=$rutinaId")

        try {
            val firestoreRepo = getFirestoreRepository()
            if (firestoreRepo != null) {
                Log.d(TAG, "📤 Enviando estadísticas a Firestore...")
                val resultado = firestoreRepo.agregarEntrenamientoAEstadistica(
                    fecha = Date(),
                    tiempoMs = tiempoMs,
                    ejerciciosCompletados = ejerciciosCompletados,
                    volumenTotal = volumenTotal,
                    rutinaId = rutinaId.toString()
                )
                resultado.fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Estadística sincronizada con Firestore EXITOSAMENTE")
                        SyncManager.syncCompleted()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error al sincronizar con Firestore: ${error.message}")
                        Log.e(TAG, "❌ Tipo de error: ${error::class.simpleName}")
                    }
                )
            } else {
                Log.w(TAG, "⚠️ getFirestoreRepository() devolvió null - estadística NO guardada en Firebase")
                Log.w(TAG, "⚠️ Posibles causas: usuario no autenticado o no registrado en trainers/clientes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al sincronizar estadística con Firestore: ${e.message}", e)
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

