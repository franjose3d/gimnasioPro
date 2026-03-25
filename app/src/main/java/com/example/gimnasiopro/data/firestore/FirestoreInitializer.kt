package com.example.gimnasiopro.data.firestore

import android.content.Context
import android.content.SharedPreferences
import com.example.gimnasiopro.data.EjercicioRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Helper para inicializar Firestore con datos iniciales.
 * Se ejecuta una sola vez cuando la app se instala o actualiza.
 *
 * Versión 3: Migra ejercicios desde colección antigua 'ejercicios'
 * a la nueva estructura jerárquica 'gruposMusculares/[grupo]/ejercicios'
 */
class FirestoreInitializer(
    private val context: Context,
    private val ejercicioRepository: EjercicioRepository
) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("firestore_migration", Context.MODE_PRIVATE)
    
    private val ejercicioFirestoreRepo = EjercicioFirestoreRepository()

    companion object {
        private const val KEY_EJERCICIOS_MIGRADOS = "ejercicios_migrados"
        private const val KEY_VERSION_MIGRACION = "version_migracion"
        private const val KEY_SECUNDARIOS_MIGRADOS = "grupos_secundarios_migrados"
        private const val CURRENT_VERSION = 12 // v12: añade ejercicios 161-164 (cinta, bicicleta estática, elíptica, escalera)
    }

    /**
     * Inicializar y migrar ejercicios en Firestore.
     * Marca versión completada solo si la migración principal se ejecuta sin errores.
     */
    suspend fun inicializarEjercicios(): Result<Boolean> {
        return try {
            val yaMigrados = prefs.getBoolean(KEY_EJERCICIOS_MIGRADOS, false)
            val versionMigracion = prefs.getInt(KEY_VERSION_MIGRACION, 0)

            // Si aún no hay sesión no podemos pasar reglas de Firestore (request.auth != null)
            if (FirebaseAuth.getInstance().currentUser == null) {
                android.util.Log.w("FirestoreInit", "Migración pospuesta: no hay usuario autenticado")
                return Result.success(false)
            }

            if (yaMigrados && versionMigracion >= CURRENT_VERSION) {
                return Result.success(false)
            }

            android.util.Log.d("FirestoreInit", "v12: Migrando nuevos ejercicios a Firestore...")
            val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
            val resultadoMigracion = ejercicioFirestoreRepo.migrarEjerciciosIniciales(ejercicios)
            if (resultadoMigracion.isFailure) {
                val error = resultadoMigracion.exceptionOrNull() ?: Exception("Error desconocido migrando ejercicios")
                android.util.Log.e("FirestoreInit", "Error migrando nuevos ejercicios: ${error.message}", error)
                return Result.failure(error)
            }
            val nuevos = resultadoMigracion.getOrNull() ?: 0
            android.util.Log.d("FirestoreInit", "v12: $nuevos nuevos ejercicios subidos a Firestore")

            android.util.Log.d("FirestoreInit", "Iniciando limpieza de duplicados...")
            val resultadoDuplicados = ejercicioFirestoreRepo.eliminarDuplicados()
            if (resultadoDuplicados.isFailure) {
                val error = resultadoDuplicados.exceptionOrNull() ?: Exception("Error desconocido limpiando duplicados")
                android.util.Log.e("FirestoreInit", "Error limpiando duplicados: ${error.message}", error)
                return Result.failure(error)
            }
            val eliminados = resultadoDuplicados.getOrNull() ?: 0
            android.util.Log.d("FirestoreInit", "Eliminados $eliminados duplicados")

            prefs.edit()
                .putBoolean(KEY_EJERCICIOS_MIGRADOS, true)
                .putInt(KEY_VERSION_MIGRACION, CURRENT_VERSION)
                .apply()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verificar si los ejercicios ya están migrados
     */
    fun ejerciciosYaMigrados(): Boolean {
        return prefs.getBoolean(KEY_EJERCICIOS_MIGRADOS, false)
    }

    /**
     * Forzar re-migración (útil para desarrollo)
     */
    suspend fun forzarRemigracionEjercicios(): Result<Boolean> {
        prefs.edit()
            .putBoolean(KEY_EJERCICIOS_MIGRADOS, false)
            .putBoolean(KEY_SECUNDARIOS_MIGRADOS, false)
            .putInt(KEY_VERSION_MIGRACION, 0)
            .apply()
        return inicializarEjercicios()
    }
}
