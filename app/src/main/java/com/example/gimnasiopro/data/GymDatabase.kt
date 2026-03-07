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
        EstadisticaEntrenamiento::class,
        RutinaDiaSemana::class,
        MensajeLocal::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(EjercicioIdsConverter::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun registroEntrenamientoDao(): RegistroEntrenamientoDao
    abstract fun estadisticaEntrenamientoDao(): EstadisticaEntrenamientoDao
    abstract fun rutinaDiaSemanaDao(): RutinaDiaSemanaDao
    abstract fun mensajeDao(): MensajeDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        /**
         * Migración de versión 6 a 7:
         * Añade columna numeroSeries a registros_entrenamiento
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añadir columna numeroSeries con valor por defecto 3
                database.execSQL("ALTER TABLE registros_entrenamiento ADD COLUMN numeroSeries INTEGER NOT NULL DEFAULT 3")
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
                    .addMigrations(MIGRATION_6_7)  // ← NUEVO: Usar migración
                    // ❌ ELIMINADO: .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}