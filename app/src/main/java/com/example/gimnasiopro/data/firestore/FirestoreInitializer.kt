package com.example.gimnasiopro.data.firestore

import android.content.Context
import android.content.SharedPreferences
import com.example.gimnasiopro.data.EjercicioRepository
import com.example.gimnasiopro.data.EjerciciosIniciales
import kotlinx.coroutines.flow.first

/**
 * Helper para inicializar Firestore con datos iniciales.
 * Se ejecuta una sola vez cuando la app se instala o actualiza.
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
        private const val CURRENT_VERSION = 1
    }

    /**
     * Inicializar ejercicios en Firestore.
     * Solo se ejecuta una vez.
     */
    suspend fun inicializarEjercicios(): Result<Boolean> {
        return try {
            // Verificar si ya se migraron los ejercicios
            val yaMigrados = prefs.getBoolean(KEY_EJERCICIOS_MIGRADOS, false)
            val versionMigracion = prefs.getInt(KEY_VERSION_MIGRACION, 0)
            
            if (yaMigrados && versionMigracion >= CURRENT_VERSION) {
                return Result.success(false) // Ya migrados
            }

            // Obtener ejercicios desde Room o desde EjerciciosIniciales
            val ejercicios = if (ejercicioRepository.allEjercicios.first().isEmpty()) {
                // Si Room está vacío, usar ejercicios iniciales
                EjerciciosIniciales.getEjerciciosIniciales()
            } else {
                // Si Room tiene datos, usarlos
                ejercicioRepository.allEjercicios.first()
            }

            // Migrar a Firestore
            val result = ejercicioFirestoreRepo.migrarEjerciciosIniciales(ejercicios)
            
            result.fold(
                onSuccess = { count ->
                    // Guardar que ya se migraron
                    prefs.edit()
                        .putBoolean(KEY_EJERCICIOS_MIGRADOS, true)
                        .putInt(KEY_VERSION_MIGRACION, CURRENT_VERSION)
                        .apply()
                    Result.success(true)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
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
