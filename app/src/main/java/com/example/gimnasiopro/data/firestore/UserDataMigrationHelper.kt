package com.example.gimnasiopro.data.firestore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.gimnasiopro.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

/**
 * Helper para migrar datos de usuario desde Room a Firestore.
 * Se ejecuta una vez por usuario cuando inicia sesión por primera vez.
 */
class UserDataMigrationHelper(
    private val context: Context,
    private val rutinaRepository: RutinaRepository,
    private val registroEntrenamientoRepository: RegistroEntrenamientoRepository,
    private val rutinaDiaSemanaRepository: RutinaDiaSemanaRepository,
    private val estadisticaRepository: EstadisticaRepository
) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("user_firestore_migration", Context.MODE_PRIVATE)
    
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UserDataMigration"
        private const val KEY_PREFIX = "user_migrated_"
        private const val KEY_VERSION_PREFIX = "user_version_"
        private const val CURRENT_VERSION = 1
    }

    /**
     * Migrar todos los datos del usuario actual desde Room a Firestore
     */
    suspend fun migrarDatosUsuario(): Result<FirestoreMigrationHelper.MigrationResult> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            // Verificar si ya se migraron los datos de este usuario
            val yaMigrado = prefs.getBoolean("$KEY_PREFIX$userId", false)
            val versionMigracion = prefs.getInt("$KEY_VERSION_PREFIX$userId", 0)

            if (yaMigrado && versionMigracion >= CURRENT_VERSION) {
                Log.d(TAG, "Datos del usuario $userId ya migrados")
                return Result.success(
                    FirestoreMigrationHelper.MigrationResult(
                        rutinasMigradas = 0,
                        calendarioMigrado = 0,
                        entrenamientosMigrados = 0,
                        estadisticasMigradas = 0
                    )
                )
            }

            Log.d(TAG, "Iniciando migración de datos para usuario: $userId")

            val migrationHelper = FirestoreMigrationHelper(
                ejercicioRepository = EjercicioRepository(
                    GymDatabase.getDatabase(context).ejercicioDao()
                ),
                rutinaRepository = rutinaRepository,
                registroEntrenamientoRepository = registroEntrenamientoRepository,
                rutinaDiaSemanaRepository = rutinaDiaSemanaRepository,
                estadisticaRepository = estadisticaRepository
            )

            val result = migrationHelper.migrarTodosLosDatosUsuario(userId)

            result.fold(
                onSuccess = { migrationResult ->
                    // Guardar que ya se migraron los datos
                    prefs.edit()
                        .putBoolean("$KEY_PREFIX$userId", true)
                        .putInt("$KEY_VERSION_PREFIX$userId", CURRENT_VERSION)
                        .apply()
                    
                    Log.d(TAG, "✅ Migración completada para usuario $userId:")
                    Log.d(TAG, "   - Rutinas: ${migrationResult.rutinasMigradas}")
                    Log.d(TAG, "   - Calendario: ${migrationResult.calendarioMigrado}")
                    Log.d(TAG, "   - Entrenamientos: ${migrationResult.entrenamientosMigrados}")
                    Log.d(TAG, "   - Estadísticas: ${migrationResult.estadisticasMigradas}")
                    
                    Result.success(migrationResult)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Error al migrar datos del usuario $userId: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado al migrar datos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si los datos del usuario ya están migrados
     */
    fun datosYaMigrados(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return prefs.getBoolean("$KEY_PREFIX$userId", false)
    }

    /**
     * Forzar re-migración (útil para desarrollo o si hay problemas)
     */
    suspend fun forzarRemigracion(): Result<FirestoreMigrationHelper.MigrationResult> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        
        prefs.edit()
            .putBoolean("$KEY_PREFIX$userId", false)
            .apply()
        
        return migrarDatosUsuario()
    }

    /**
     * Migrar solo un tipo de dato específico
     */
    suspend fun migrarRutinasUsuario(): Result<Int> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        
        val migrationHelper = FirestoreMigrationHelper(
            ejercicioRepository = EjercicioRepository(
                GymDatabase.getDatabase(context).ejercicioDao()
            ),
            rutinaRepository = rutinaRepository,
            registroEntrenamientoRepository = registroEntrenamientoRepository,
            rutinaDiaSemanaRepository = rutinaDiaSemanaRepository,
            estadisticaRepository = estadisticaRepository
        )
        
        return migrationHelper.migrarRutinasUsuario(userId)
    }

    suspend fun migrarCalendarioUsuario(): Result<Int> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        
        val migrationHelper = FirestoreMigrationHelper(
            ejercicioRepository = EjercicioRepository(
                GymDatabase.getDatabase(context).ejercicioDao()
            ),
            rutinaRepository = rutinaRepository,
            registroEntrenamientoRepository = registroEntrenamientoRepository,
            rutinaDiaSemanaRepository = rutinaDiaSemanaRepository,
            estadisticaRepository = estadisticaRepository
        )
        
        return migrationHelper.migrarCalendarioUsuario(userId)
    }

    suspend fun migrarEntrenamientosUsuario(): Result<Int> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        
        val migrationHelper = FirestoreMigrationHelper(
            ejercicioRepository = EjercicioRepository(
                GymDatabase.getDatabase(context).ejercicioDao()
            ),
            rutinaRepository = rutinaRepository,
            registroEntrenamientoRepository = registroEntrenamientoRepository,
            rutinaDiaSemanaRepository = rutinaDiaSemanaRepository,
            estadisticaRepository = estadisticaRepository
        )
        
        return migrationHelper.migrarEntrenamientosUsuario(userId)
    }

    suspend fun migrarEstadisticasUsuario(): Result<Int> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        
        val migrationHelper = FirestoreMigrationHelper(
            ejercicioRepository = EjercicioRepository(
                GymDatabase.getDatabase(context).ejercicioDao()
            ),
            rutinaRepository = rutinaRepository,
            registroEntrenamientoRepository = registroEntrenamientoRepository,
            rutinaDiaSemanaRepository = rutinaDiaSemanaRepository,
            estadisticaRepository = estadisticaRepository
        )
        
        return migrationHelper.migrarEstadisticasUsuario(userId)
    }
}
