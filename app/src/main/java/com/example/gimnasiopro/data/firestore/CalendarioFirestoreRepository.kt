package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para calendario (rutinas por día de semana) en Firestore.
 * Se guarda en users/{userId}/calendario/{diaSemana}
 */
class CalendarioFirestoreRepository(private val userId: String) {

    private val firestore = FirebaseFirestore.getInstance()
    private val calendarioCollection = firestore
        .collection("users")
        .document(userId)
        .collection("calendario")

    /**
     * Obtener todo el calendario del usuario
     */
    fun getCalendario(): Flow<List<CalendarioFirestore>> = flow {
        val snapshot = calendarioCollection
            .orderBy("diaSemana")
            .get()
            .await()
        
        val calendario = snapshot.documents.mapNotNull { doc ->
            CalendarioFirestore.fromDocument(doc)
        }
        emit(calendario)
    }

    /**
     * Obtener la rutina asignada a un día específico
     */
    suspend fun getRutinaPorDia(diaSemana: Int): CalendarioFirestore? {
        return try {
            val document = calendarioCollection.document(diaSemana.toString()).get().await()
            if (document.exists()) {
                CalendarioFirestore.fromDocument(document)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Asignar una rutina a un día de la semana
     */
    suspend fun asignarRutinaADia(
        diaSemana: Int,
        rutinaId: String?
    ): Result<Unit> {
        return try {
            val calendario = CalendarioFirestore(
                diaSemana = diaSemana,
                rutinaId = rutinaId,
                activo = true,
                fechaModificacion = java.util.Date()
            )
            calendarioCollection
                .document(diaSemana.toString())
                .set(calendario.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar la rutina asignada a un día
     */
    suspend fun eliminarRutinaDeDia(diaSemana: Int): Result<Unit> {
        return try {
            calendarioCollection.document(diaSemana.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activar/desactivar un día del calendario
     */
    suspend fun setDiaActivo(diaSemana: Int, activo: Boolean): Result<Unit> {
        return try {
            calendarioCollection
                .document(diaSemana.toString())
                .update("activo", activo)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
