package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para rutinas en Firestore.
 * Las rutinas se guardan en users/{userId}/rutinas/{rutinaId}
 */
class RutinaFirestoreRepository(private val userId: String) {

    private val firestore = FirebaseFirestore.getInstance()
    private val rutinasCollection = firestore
        .collection("users")
        .document(userId)
        .collection("rutinas")

    /**
     * Obtener todas las rutinas del usuario
     */
    fun getAllRutinas(): Flow<List<RutinaFirestore>> = flow {
        val snapshot = rutinasCollection
            .orderBy("fechaCreacion", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        
        val rutinas = snapshot.documents.mapNotNull { doc ->
            RutinaFirestore.fromDocument(doc)
        }
        emit(rutinas)
    }

    /**
     * Obtener una rutina por ID
     */
    suspend fun getRutinaById(rutinaId: String): RutinaFirestore? {
        return try {
            val document = rutinasCollection.document(rutinaId).get().await()
            RutinaFirestore.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crear una nueva rutina
     */
    suspend fun crearRutina(rutina: RutinaFirestore): Result<String> {
        return try {
            val rutinaConUsuario = rutina.copy(creadoPor = userId)
            val docRef = rutinasCollection.add(rutinaConUsuario.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar una rutina existente
     */
    suspend fun actualizarRutina(rutina: RutinaFirestore): Result<Unit> {
        return try {
            if (rutina.rutinaId.isEmpty()) {
                return Result.failure(IllegalArgumentException("La rutina debe tener un ID"))
            }
            val rutinaActualizada = rutina.copy(
                fechaModificacion = java.util.Date()
            )
            rutinasCollection
                .document(rutina.rutinaId)
                .update(rutinaActualizada.toMap() as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar solo los ejercicios de una rutina
     */
    suspend fun actualizarEjerciciosDeRutina(
        rutinaId: String,
        ejercicioIds: List<String>
    ): Result<Unit> {
        return try {
            rutinasCollection
                .document(rutinaId)
                .update(
                    mapOf(
                        "ejercicioIds" to ejercicioIds,
                        "fechaModificacion" to com.google.firebase.Timestamp(java.util.Date())
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Añadir ejercicios a una rutina existente
     */
    suspend fun agregarEjerciciosARutina(
        rutinaId: String,
        nuevosEjercicioIds: List<String>
    ): Result<Int> {
        return try {
            val rutina = getRutinaById(rutinaId)
                ?: return Result.failure(IllegalArgumentException("Rutina no encontrada"))
            
            val ejerciciosExistentes = rutina.ejercicioIds.toMutableList()
            val ejerciciosNuevos = nuevosEjercicioIds.filter { it !in ejerciciosExistentes }
            
            if (ejerciciosNuevos.isEmpty()) {
                return Result.success(0)
            }
            
            ejerciciosExistentes.addAll(ejerciciosNuevos)
            actualizarEjerciciosDeRutina(rutinaId, ejerciciosExistentes)
            
            Result.success(ejerciciosNuevos.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una rutina
     */
    suspend fun eliminarRutina(rutinaId: String): Result<Unit> {
        return try {
            rutinasCollection.document(rutinaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
