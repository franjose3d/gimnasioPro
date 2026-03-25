package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un ejercicio en la base de datos.
 *
 * @property id Identificador único del ejercicio (auto-generado)
 * @property grupoMuscular El grupo muscular principal al que pertenece el ejercicio (ej: "Pecho", "Espalda", "Piernas")
 * @property grupoMuscularSecundario Grupo muscular secundario que también trabaja el ejercicio (nullable)
 * @property nombre El nombre del ejercicio
 * @property descripcion Una breve descripción del ejercicio y cómo realizarlo
 * @property imagenUrl URL opcional de una imagen del ejercicio
 */
@Entity(tableName = "ejercicios")
data class Ejercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grupoMuscular: String,
    val grupoMuscularSecundario: String? = null,
    val nombre: String,
    val descripcion: String,
    val imagenUrl: String? = null
)

