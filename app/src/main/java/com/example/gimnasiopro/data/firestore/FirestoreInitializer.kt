package com.example.gimnasiopro.data.firestore

import android.content.Context
import android.content.SharedPreferences
import com.example.gimnasiopro.data.EjercicioRepository

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
        // Versión 8: Limpieza forzada de duplicados en Firebase (mejorada)
        private const val CURRENT_VERSION = 8
    }

    /**
     * Inicializar ejercicios en Firestore.
     * Versión 6: Limpia ejercicios duplicados después de corregir bug de sincronización.
     */
    suspend fun inicializarEjercicios(): Result<Boolean> {
        return try {
            // Verificar si ya se migraron los ejercicios
            val yaMigrados = prefs.getBoolean(KEY_EJERCICIOS_MIGRADOS, false)
            val versionMigracion = prefs.getInt(KEY_VERSION_MIGRACION, 0)
            
            if (yaMigrados && versionMigracion >= CURRENT_VERSION) {
                return Result.success(false) // Ya migrados
            }

            // VERSIÓN 5: Limpiar duplicados que puedan existir
            android.util.Log.d("FirestoreInit", "Iniciando limpieza de duplicados...")
            val resultLimpieza = ejercicioFirestoreRepo.eliminarDuplicados()
            resultLimpieza.fold(
                onSuccess = { eliminados ->
                    android.util.Log.d("FirestoreInit", "Eliminados $eliminados ejercicios duplicados")
                },
                onFailure = { error ->
                    android.util.Log.e("FirestoreInit", "Error limpiando duplicados: ${error.message}")
                }
            )

            // Guardar que ya se ejecutó esta versión
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
            .apply()
        return inicializarEjercicios()
    }
}
