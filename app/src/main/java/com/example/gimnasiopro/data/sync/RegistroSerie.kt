package com.example.gimnasiopro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_series",
    foreignKeys = [
        ForeignKey(
            entity = RegistroEntrenamiento::class,
            parentColumns = ["id"],
            childColumns = ["registroEntrenamientoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("registroEntrenamientoId")]
)
data class RegistroSerie(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val registroEntrenamientoId: Long,
    val numeroSerie: Int,
    val pesoKg: Float,
    val repeticiones: Int
)