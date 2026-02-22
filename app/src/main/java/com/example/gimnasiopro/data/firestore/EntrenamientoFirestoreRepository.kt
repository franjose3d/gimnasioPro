package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repositorio para entrenamientos en Firestore.
 * Se guarda en clientes/{userId}/entrenamientos/{entrenamientoId}
 * o trainers/{userId}/entrenamientos/{entrenamientoId}
 */
class EntrenamientoFirestoreRepository(
    private val userId: String,
    private val tipoUsuario: String = "cliente"
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val coleccionBase = if (tipoUsuario == "trainer") "trainers" else "clientes"
    private val entrenamientosCollection = firestore
        .collection(coleccionBase)
        .document(userId)
        .collection("entrenamientos")

    /**
     * Obtener todos los entrenamientos del usuario
     */
    fun getAllEntrenamientos(): Flow<List<EntrenamientoFirestore>> = flow {
        val snapshot = entrenamientosCollection
            .orderBy("fechaEntrenamiento", Query.Direction.DESCENDING)
            .get()
            .await()
        
        val entrenamientos = snapshot.documents.mapNotNull { doc ->
            EntrenamientoFirestore.fromDocument(doc)
        }
        emit(entrenamientos)
    }

    /**
     * Obtener entrenamientos en un rango de fechas
     */
    fun getEntrenamientosPorFecha(
        fechaInicio: Date,
        fechaFin: Date
    ): Flow<List<EntrenamientoFirestore>> = flow {
        val snapshot = entrenamientosCollection
            .whereGreaterThanOrEqualTo("fechaEntrenamiento", com.google.firebase.Timestamp(fechaInicio))
            .whereLessThanOrEqualTo("fechaEntrenamiento", com.google.firebase.Timestamp(fechaFin))
            .orderBy("fechaEntrenamiento", Query.Direction.DESCENDING)
            .get()
            .await()
        
        val entrenamientos = snapshot.documents.mapNotNull { doc ->
            EntrenamientoFirestore.fromDocument(doc)
        }
        emit(entrenamientos)
    }

    /**
     * Obtener un entrenamiento por ID
     */
    suspend fun getEntrenamientoById(entrenamientoId: String): EntrenamientoFirestore? {
        return try {
            val document = entrenamientosCollection.document(entrenamientoId).get().await()
            EntrenamientoFirestore.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guardar un nuevo entrenamiento
     */
    suspend fun guardarEntrenamiento(entrenamiento: EntrenamientoFirestore): Result<String> {
        return try {
            val docRef = entrenamientosCollection.add(entrenamiento.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar un entrenamiento existente
     */
    suspend fun actualizarEntrenamiento(entrenamiento: EntrenamientoFirestore): Result<Unit> {
        return try {
            if (entrenamiento.entrenamientoId.isEmpty()) {
                return Result.failure(IllegalArgumentException("El entrenamiento debe tener un ID"))
            }
            entrenamientosCollection
                .document(entrenamiento.entrenamientoId)
                .update(entrenamiento.toMap() as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar un entrenamiento
     */
    suspend fun eliminarEntrenamiento(entrenamientoId: String): Result<Unit> {
        return try {
            entrenamientosCollection.document(entrenamientoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener el último entrenamiento del usuario
     */
    suspend fun getUltimoEntrenamiento(): EntrenamientoFirestore? {
        return try {
            val snapshot = entrenamientosCollection
                .orderBy("fechaEntrenamiento", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                EntrenamientoFirestore.fromDocument(doc)
            }
        } catch (e: Exception) {
            null
        }
    }
}
