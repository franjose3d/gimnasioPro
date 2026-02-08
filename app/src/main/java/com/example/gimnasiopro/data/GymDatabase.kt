package com.example.gimnasiopro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room para la aplicación GimnasioPro.
 * Contiene las tablas de ejercicios y rutinas.
 */
@Database(
    entities = [Ejercicio::class, Rutina::class, RegistroEntrenamiento::class, EstadisticaEntrenamiento::class, RutinaDiaSemana::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(EjercicioIdsConverter::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun registroEntrenamientoDao(): RegistroEntrenamientoDao
    abstract fun estadisticaEntrenamientoDao(): EstadisticaEntrenamientoDao
    abstract fun rutinaDiaSemanaDao(): RutinaDiaSemanaDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        /**
         * Obtiene la instancia de la base de datos (Singleton).
         */
        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gimnasio_pro_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

