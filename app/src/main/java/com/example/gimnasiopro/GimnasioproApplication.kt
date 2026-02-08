package com.example.gimnasiopro

import android.app.Application
import android.util.Log
import com.example.gimnasiopro.data.DatabaseInitializer
import com.example.gimnasiopro.data.EjercicioRepository
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RegistroEntrenamientoRepository
import com.example.gimnasiopro.data.RutinaRepository

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

    // Lazy initialization del repositorio de ejercicios
    val ejercicioRepository: EjercicioRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de ejercicios...")
        EjercicioRepository(database.ejercicioDao())
    }

    // Lazy initialization del repositorio de rutinas
    val rutinaRepository: RutinaRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de rutinas...")
        RutinaRepository(database.rutinaDao())
    }

    // Lazy initialization del repositorio de registros de entrenamiento
    val registroEntrenamientoRepository: RegistroEntrenamientoRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de registros de entrenamiento...")
        RegistroEntrenamientoRepository(database.registroEntrenamientoDao())
    }

    // Lazy initialization del repositorio de estadísticas
    val estadisticaRepository: EstadisticaRepository by lazy {
        Log.d(TAG, "Inicializando repositorio de estadísticas...")
        EstadisticaRepository(database.estadisticaEntrenamientoDao())
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - iniciando inicialización de BD")
        // Inicializar la base de datos con los ejercicios y rutinas predefinidas
        DatabaseInitializer.initializeIfNeeded(this, ejercicioRepository, rutinaRepository)
    }
}

