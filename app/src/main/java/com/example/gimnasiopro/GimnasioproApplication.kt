package com.example.gimnasiopro

import android.app.Application
import android.util.Log
import com.example.gimnasiopro.data.DatabaseInitializer
import com.example.gimnasiopro.data.EjercicioRepository
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RegistroEntrenamientoRepository
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.RutinaDiaSemanaRepository
import com.example.gimnasiopro.data.firestore.EjercicioFirestoreRepository
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.FirestoreInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Clase Application personalizada para inicializar componentes globales.
 */
class GimnasioproApplication : Application() {

    companion object {
        private const val TAG = "GimnasioproApp"
    }

    // Lazy initialization de la base de datos
    val database: GymDatabase by lazy {
        Log.d(TAG, "Inicializando base de datos...")
        GymDatabase.getDatabase(this)
    }

    // Repositorio local (Room) - solo para cache y debugging
    val localEjercicioRepository: EjercicioRepository by lazy {
        Log.d(TAG, "Inicializando repositorio local de ejercicios...")
        EjercicioRepository(database.ejercicioDao())
    }

    // Repositorio remoto (Firestore) - público para acceso desde Activities
    val ejercicioFirestoreRepository: EjercicioFirestoreRepository by lazy {
        Log.d(TAG, "Inicializando repositorio remoto de ejercicios...")
        EjercicioFirestoreRepository()
    }

    // Repositorio híbrido (combina Room + Firestore)
    val ejercicioRepository: EjercicioRepositoryHibrido by lazy {
        Log.d(TAG, "Inicializando repositorio híbrido de ejercicios...")
        EjercicioRepositoryHibrido(localEjercicioRepository, ejercicioFirestoreRepository)
    }

    // Lazy initialization del repositorio de rutinas (local)
    val rutinaRepository: RutinaRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de rutinas...")
        RutinaRepository(database.rutinaDao())
    }

    // Repositorio híbrido de rutinas (Room + Firebase)
    val rutinaRepositoryHibrido: com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido by lazy {
        Log.d(TAG, "Inicializando repositorio híbrido de rutinas...")
        com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido(rutinaRepository)
    }

    // Repositorio de conexiones trainer-cliente
    val conexionRepository: com.example.gimnasiopro.data.firestore.ConexionRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de conexiones...")
        com.example.gimnasiopro.data.firestore.ConexionRepository()
    }

    // Lazy initialization del repositorio de registros de entrenamiento
    val registroEntrenamientoRepository: RegistroEntrenamientoRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de registros de entrenamiento...")
        RegistroEntrenamientoRepository(
            database.registroEntrenamientoDao(),
            database.registroSerieDao()  // ← NUEVO: Añadido DAO de series
        )
    }

    // Lazy initialization del repositorio de estadísticas (local)
    val estadisticaRepository: EstadisticaRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de estadísticas...")
        EstadisticaRepository(database.estadisticaEntrenamientoDao())
    }

    // Repositorio híbrido de estadísticas (sincroniza local + Firestore)
    val estadisticaRepositoryHibrido: com.example.gimnasiopro.data.firestore.EstadisticaRepositoryHibrido by lazy {
        Log.d(TAG, "Inicializando repositorio híbrido de estadísticas...")
        com.example.gimnasiopro.data.firestore.EstadisticaRepositoryHibrido(estadisticaRepository)
    }

    // Lazy initialization del repositorio de rutinas por día
    val rutinaDiaSemanaRepository: RutinaDiaSemanaRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de rutinas por día...")
        RutinaDiaSemanaRepository(database.rutinaDiaSemanaDao())
    }

    // Scope para operaciones en background
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Firestore Initializer (usa el repo local para inicialización)
    val firestoreInitializer: FirestoreInitializer by lazy {
        FirestoreInitializer(this, localEjercicioRepository)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - iniciando inicialización híbrida")

        // 0. Inicializar canales de notificación (debe hacerse antes de cualquier notificación)
        try {
            com.example.gimnasiopro.presentation.NotificacionLocalService.inicializarCanales(this)
            Log.d(TAG, "✅ Canales de notificación inicializados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando canales: ${e.message}")
        }

        // 1. Restaurar badge desde SharedPreferences al iniciar la app
        try {
            val badgeCount = com.example.gimnasiopro.utils.NotificationBadgeManager.obtenerContador(this)
            if (badgeCount > 0) {
                // Reaplica el badge que teníamos guardado
                com.example.gimnasiopro.utils.NotificationBadgeManager.setBadgeCount(this, badgeCount)
                Log.d(TAG, "🔔 Badge restaurado al iniciar: $badgeCount notificaciones pendientes")
            }

            // Diagnóstico: Verificar compatibilidad con el launcher
            com.example.gimnasiopro.utils.NotificationBadgeManager.diagnosticar(this)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restaurando badge: ${e.message}")
        }

        // 2. Inicializar la base de datos local PRIMERO (para cache inmediato)
        DatabaseInitializer.initializeIfNeeded(this, localEjercicioRepository, rutinaRepository)

        // 3. Inicializar Firestore en background (no bloquea el inicio de la app)
        applicationScope.launch {
            try {
                val result = firestoreInitializer.inicializarEjercicios()
                result.fold(
                    onSuccess = { migrado ->
                        if (migrado) {
                            Log.d(TAG, "✅ Ejercicios migrados a Firestore exitosamente")
                        } else {
                            Log.d(TAG, "ℹ️ Ejercicios ya estaban migrados a Firestore")
                        }
                        // Forzar sincronización después de la migración
                        Log.d(TAG, "🔄 Iniciando sincronización automática...")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error al migrar ejercicios a Firestore: ${error.message}", error)
                        Log.i(TAG, "📱 Modo offline activado - usando solo datos locales")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al inicializar Firestore: ${e.message}", e)
                Log.i(TAG, "📱 Modo offline activado - usando solo datos locales")
            }
        }
    }
}
