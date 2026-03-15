package com.example.gimnasiopro.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Inicializador de la base de datos.
 * Se encarga de poblar la base de datos con los ejercicios iniciales
 * la primera vez que se ejecuta la aplicación.
 */
object DatabaseInitializer {

    @Volatile
    private var isInitialized = false

    // Scope persistente para operaciones de reinicialización
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reinitializeJob: Job? = null

    /**
     * Inicializa la base de datos con los ejercicios predefinidos
     * si no ha sido inicializada previamente o si no hay ejercicios.
     */
    fun initializeIfNeeded(context: Context, repository: EjercicioRepository, rutinaRepository: RutinaRepository) {
        if (isInitialized) return

        // Usar runBlocking para asegurar que la inicialización se complete
        runBlocking(Dispatchers.IO) {
            try {
                // Siempre verificar si hay ejercicios en la base de datos
                val count = repository.getEjerciciosCount()
                android.util.Log.d("DatabaseInitializer", "Ejercicios en la base de datos: $count")

                if (count == 0) {
                    // Insertar ejercicios iniciales
                    val ejercicios = EjerciciosIniciales.getEjerciciosIniciales()
                    android.util.Log.d("DatabaseInitializer", "Insertando ${ejercicios.size} ejercicios")
                    repository.insertAllEjercicios(ejercicios)
                    android.util.Log.d("DatabaseInitializer", "Ejercicios insertados correctamente")

                    // Verificar que se insertaron
                    val newCount = repository.getEjerciciosCount()
                    android.util.Log.d("DatabaseInitializer", "Verificación: $newCount ejercicios después de insertar")
                } else {
                    // Verificar si necesita migración de "Piernas y Glúteos" a grupos separados
                    // Esto actualiza los ejercicios existentes con los nuevos datos
                    val ejercicios = EjerciciosIniciales.getEjerciciosIniciales()
                    repository.insertAllEjercicios(ejercicios)
                    android.util.Log.d("DatabaseInitializer", "Ejercicios actualizados con grupos separados")
                }

                // Inicializar las rutinas vacías
                rutinaRepository.initializeRutinasIfNeeded()

                isInitialized = true
                android.util.Log.d("DatabaseInitializer", "Inicialización completada")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseInitializer", "Error inicializando base de datos", e)
            }
        }
    }

    /**
     * Fuerza la reinicialización de la base de datos.
     * Útil para actualizar los ejercicios cuando se actualiza la app.
     */
    fun forceReinitialize(repository: EjercicioRepository) {
        reinitializeJob?.cancel() // Cancelar reinicialización previa si aún está en curso
        reinitializeJob = initScope.launch {
            try {
                // Eliminar todos los ejercicios existentes
                repository.deleteAllEjercicios()

                // Insertar los nuevos ejercicios
                val ejercicios = EjerciciosIniciales.getEjerciciosIniciales()
                repository.insertAllEjercicios(ejercicios)

                android.util.Log.d("DatabaseInitializer", "Base de datos reinicializada con ${ejercicios.size} ejercicios")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseInitializer", "Error reinicializando base de datos", e)
            }
        }
    }
}

