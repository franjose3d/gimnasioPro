package com.example.gimnasiopro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gimnasiopro.data.local.MensajeLocal
import com.example.gimnasiopro.data.local.MensajeDao

/**
 * Base de datos Room para la aplicación GimnasioPro.
 *
 * Version 7: Añadida columna numeroSeries a registros_entrenamiento
 */
@Database(
    entities = [
        Ejercicio::class,
        Rutina::class,
        RegistroEntrenamiento::class,
        RegistroSerie::class,
        EstadisticaEntrenamiento::class,
        RutinaDiaSemana::class,
        MensajeLocal::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(EjercicioIdsConverter::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun registroEntrenamientoDao(): RegistroEntrenamientoDao
    abstract fun registroSerieDao(): RegistroSerieDao  // ← NUEVO
    abstract fun estadisticaEntrenamientoDao(): EstadisticaEntrenamientoDao
    abstract fun rutinaDiaSemanaDao(): RutinaDiaSemanaDao
    abstract fun mensajeDao(): MensajeDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        /**
         * Migración de versión 7 a 8:
         * Añade tabla registros_series para guardar cada serie individualmente
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla de series
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS registros_series (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                registroEntrenamientoId INTEGER NOT NULL,
                numeroSerie INTEGER NOT NULL,
                pesoKg REAL NOT NULL,
                repeticiones INTEGER NOT NULL,
                FOREIGN KEY(registroEntrenamientoId) REFERENCES registros_entrenamiento(id) ON DELETE CASCADE
            )
        """)

                // Crear índice para mejorar rendimiento
                database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_registros_series_registroEntrenamientoId 
            ON registros_series(registroEntrenamientoId)
        """)
            }
        }

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
                    .addMigrations(MIGRATION_7_8)  // ← NUEVO: Usar migración
                    // ❌ ELIMINADO: .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}