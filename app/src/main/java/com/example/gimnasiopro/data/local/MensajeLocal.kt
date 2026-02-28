package com.example.gimnasiopro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar mensajes localmente.
 *
 * Firebase solo actúa como buzón temporal.
 * El historial completo se guarda aquí (Room).
 */
@Entity(tableName = "mensajes_locales")
data class MensajeLocal(
    @PrimaryKey
    val id: String,

    val conversacionId: String,      // clienteId_trainerId
    val remitenteId: String,
    val destinatarioId: String,
    val texto: String,
    val fechaEnvio: Long,            // Timestamp en millis
    val leido: Boolean = false,
    val entregado: Boolean = false,  // Confirmado por Firebase

    // Para UI: saber si es "yo" u "otro"
    val esRemitente: Boolean         // true si yo envié, false si recibí
)