package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repositorio para estadísticas en Firestore.
 * Se guarda en clientes/{userId}/estadisticas/{fecha} o trainers/{userId}/estadisticas/{fecha}
 */
class EstadisticaFirestoreRepository(
    private val userId: String,
    private val tipoUsuario: String = "cliente"
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val coleccionBase = if (tipoUsuario == "trainer") "trainers" else "clientes"
    private val estadisticasCollection = firestore
        .collection(coleccionBase)
        .document(userId)
        .collection("estadisticas")

    /**
     * Obtener estadística de un día específico
     */
    suspend fun getEstadisticaPorFecha(fecha: Date): EstadisticaFirestore? {
        return try {
            val fechaStr = EstadisticaFirestore.formatFecha(fecha)
            val document = estadisticasCollection.document(fechaStr).get().await()
            if (document.exists()) {
                EstadisticaFirestore.fromDocument(document)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtener estadísticas de un mes
     */
    fun getEstadisticasPorMes(anio: Int, mes: Int): Flow<List<EstadisticaFirestore>> = flow {
        val snapshot = estadisticasCollection
            .whereEqualTo("anio", anio)
            .whereEqualTo("mes", mes)
            .orderBy("dia")
            .get()
            .await()
        
        val estadisticas = snapshot.documents.mapNotNull { doc ->
            EstadisticaFirestore.fromDocument(doc)
        }
        emit(estadisticas)
    }

    /**
     * Obtener estadísticas de un año
     */
    fun getEstadisticasPorAnio(anio: Int): Flow<List<EstadisticaFirestore>> = flow {
        val snapshot = estadisticasCollection
            .whereEqualTo("anio", anio)
            .orderBy("mes")
            .orderBy("dia")
            .get()
            .await()
        
        val estadisticas = snapshot.documents.mapNotNull { doc ->
            EstadisticaFirestore.fromDocument(doc)
        }
        emit(estadisticas)
    }

    /**
     * Obtener estadísticas en un rango de fechas
     */
    fun getEstadisticasPorRango(
        fechaInicio: Date,
        fechaFin: Date
    ): Flow<List<EstadisticaFirestore>> = flow {
        val inicioTimestamp = com.google.firebase.Timestamp(
            EstadisticaFirestore.getInicioDia(fechaInicio)
        )
        val finTimestamp = com.google.firebase.Timestamp(
            EstadisticaFirestore.getInicioDia(fechaFin)
        )
        
        val snapshot = estadisticasCollection
            .whereGreaterThanOrEqualTo("fechaTimestamp", inicioTimestamp)
            .whereLessThanOrEqualTo("fechaTimestamp", finTimestamp)
            .orderBy("fechaTimestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        
        val estadisticas = snapshot.documents.mapNotNull { doc ->
            EstadisticaFirestore.fromDocument(doc)
        }
        emit(estadisticas)
    }

    /**
     * Guardar o actualizar estadística de un día
     */
    suspend fun guardarEstadistica(estadistica: EstadisticaFirestore): Result<Unit> {
        return try {
            estadisticasCollection
                .document(estadistica.fecha)
                .set(estadistica.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar estadística agregando datos de un entrenamiento
     */
    suspend fun agregarEntrenamientoAEstadistica(
        fecha: Date,
        tiempoMs: Long,
        ejerciciosCompletados: Int,
        volumenTotal: Float,
        rutinaId: String?
    ): Result<Unit> {
        return try {
            val fechaStr = EstadisticaFirestore.formatFecha(fecha)
            val fechaTimestamp = EstadisticaFirestore.getInicioDia(fecha)
            val calendar = java.util.Calendar.getInstance().apply { time = fecha }
            
            val estadisticaRef = estadisticasCollection.document(fechaStr)
            val estadisticaDoc = estadisticaRef.get().await()
            
            if (estadisticaDoc.exists()) {
                // Actualizar estadística existente
                estadisticaRef.update(
                    mapOf(
                        "numeroEntrenamientos" to com.google.firebase.firestore.FieldValue.increment(1),
                        "tiempoEntrenamientoMs" to com.google.firebase.firestore.FieldValue.increment(tiempoMs),
                        "ejerciciosCompletados" to com.google.firebase.firestore.FieldValue.increment(ejerciciosCompletados.toLong()),
                        "volumenTotal" to com.google.firebase.firestore.FieldValue.increment(volumenTotal.toDouble()),
                        "rutinasUsadas" to com.google.firebase.firestore.FieldValue.arrayUnion(rutinaId ?: "")
                    )
                ).await()
            } else {
                // Crear nueva estadística
                val nuevaEstadistica = EstadisticaFirestore(
                    fecha = fechaStr,
                    fechaTimestamp = fechaTimestamp,
                    anio = calendar.get(java.util.Calendar.YEAR),
                    mes = calendar.get(java.util.Calendar.MONTH) + 1,
                    dia = calendar.get(java.util.Calendar.DAY_OF_MONTH),
                    tiempoEntrenamientoMs = tiempoMs,
                    numeroEntrenamientos = 1,
                    ejerciciosCompletados = ejerciciosCompletados,
                    volumenTotal = volumenTotal,
                    rutinasUsadas = if (rutinaId != null) listOf(rutinaId) else emptyList()
                )
                estadisticasCollection.document(fechaStr).set(nuevaEstadistica.toMap()).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
