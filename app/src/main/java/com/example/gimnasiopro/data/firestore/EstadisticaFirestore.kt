package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo de Estadística de entrenamiento para Firestore.
 * Se guarda en users/{userId}/estadisticas/{fecha}
 * La fecha se guarda como string en formato "YYYY-MM-DD" para facilitar consultas
 */
data class EstadisticaFirestore(
    val fecha: String, // Formato "YYYY-MM-DD"
    val fechaTimestamp: Date, // Timestamp del inicio del día (00:00:00)
    val anio: Int,
    val mes: Int,
    val dia: Int,
    val tiempoEntrenamientoMs: Long = 0,
    val numeroEntrenamientos: Int = 1,
    val ejerciciosCompletados: Int = 0,
    val volumenTotal: Float = 0f,
    val rutinasUsadas: List<String> = emptyList()
) {
    /**
     * Convertir a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "fecha" to fecha,
            "fechaTimestamp" to Timestamp(fechaTimestamp),
            "anio" to anio,
            "mes" to mes,
            "dia" to dia,
            "tiempoEntrenamientoMs" to tiempoEntrenamientoMs,
            "numeroEntrenamientos" to numeroEntrenamientos,
            "ejerciciosCompletados" to ejerciciosCompletados,
            "volumenTotal" to volumenTotal,
            "rutinasUsadas" to rutinasUsadas
        )
    }

    companion object {
        /**
         * Crear desde DocumentSnapshot de Firestore
         */
        fun fromDocument(document: DocumentSnapshot): EstadisticaFirestore? {
            return try {
                val data = document.data ?: return null
                EstadisticaFirestore(
                    fecha = document.id, // El ID del documento es la fecha
                    fechaTimestamp = (data["fechaTimestamp"] as? Timestamp)?.toDate() ?: Date(),
                    anio = (data["anio"] as? Number)?.toInt() ?: 0,
                    mes = (data["mes"] as? Number)?.toInt() ?: 0,
                    dia = (data["dia"] as? Number)?.toInt() ?: 0,
                    tiempoEntrenamientoMs = (data["tiempoEntrenamientoMs"] as? Number)?.toLong() ?: 0,
                    numeroEntrenamientos = (data["numeroEntrenamientos"] as? Number)?.toInt() ?: 1,
                    ejerciciosCompletados = (data["ejerciciosCompletados"] as? Number)?.toInt() ?: 0,
                    volumenTotal = (data["volumenTotal"] as? Number)?.toFloat() ?: 0f,
                    rutinasUsadas = (data["rutinasUsadas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Formatear fecha a string "YYYY-MM-DD"
         */
        fun formatFecha(date: Date): String {
            val calendar = java.util.Calendar.getInstance().apply {
                time = date
            }
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            return String.format("%04d-%02d-%02d", year, month, day)
        }

        /**
         * Obtener timestamp del inicio del día (00:00:00)
         */
        fun getInicioDia(date: Date): Date {
            val calendar = java.util.Calendar.getInstance().apply {
                time = date
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return calendar.time
        }
    }
}
