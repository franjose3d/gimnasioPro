package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para ejercicios en Firestore.
 * Los ejercicios se guardan en la colección global "ejercicios".
 */
class EjercicioFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val ejerciciosCollection = firestore.collection("ejercicios")

    /**
     * Obtener todos los ejercicios
     */
    fun getAllEjercicios(): Flow<List<EjercicioFirestore>> = flow {
        val snapshot = ejerciciosCollection
            .orderBy("grupoMuscular")
            .orderBy("nombre")
            .get()
            .await()
        
        val ejercicios = snapshot.documents.mapNotNull { doc ->
            EjercicioFirestore.fromDocument(doc)
        }
        emit(ejercicios)
    }

    /**
     * Obtener ejercicios por grupo muscular
     */
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<EjercicioFirestore>> = flow {
        val snapshot = ejerciciosCollection
            .whereEqualTo("grupoMuscular", grupoMuscular)
            .orderBy("nombre")
            .get()
            .await()
        
        val ejercicios = snapshot.documents.mapNotNull { doc ->
            EjercicioFirestore.fromDocument(doc)
        }
        emit(ejercicios)
    }

    /**
     * Obtener un ejercicio por ID
     */
    suspend fun getEjercicioById(ejercicioId: String): EjercicioFirestore? {
        return try {
            val document = ejerciciosCollection.document(ejercicioId).get().await()
            EjercicioFirestore.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Buscar ejercicios por nombre
     */
    fun searchEjercicios(query: String): Flow<List<EjercicioFirestore>> = flow {
        // Firestore no soporta búsqueda de texto completo nativa
        // Obtenemos todos y filtramos en memoria (para pocos ejercicios es aceptable)
        // Para producción, considera usar Algolia o similar
        val snapshot = ejerciciosCollection
            .orderBy("nombre")
            .get()
            .await()
        
        val queryLower = query.lowercase()
        val ejercicios = snapshot.documents
            .mapNotNull { EjercicioFirestore.fromDocument(it) }
            .filter { 
                it.nombre.lowercase().contains(queryLower) ||
                it.descripcion.lowercase().contains(queryLower)
            }
        emit(ejercicios)
    }

    /**
     * Obtener todos los grupos musculares únicos
     */
    fun getAllGruposMusculares(): Flow<List<String>> = flow {
        val snapshot = ejerciciosCollection
            .orderBy("grupoMuscular")
            .get()
            .await()
        
        val grupos = snapshot.documents
            .mapNotNull { it.getString("grupoMuscular") }
            .distinct()
            .sorted()
        emit(grupos)
    }

    /**
     * Crear un nuevo ejercicio
     */
    suspend fun crearEjercicio(ejercicio: EjercicioFirestore): Result<String> {
        return try {
            val docRef = ejerciciosCollection.add(ejercicio.toMap()).await()
            // Actualizar el documento con su ID
            docRef.update("id", docRef.id).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar un ejercicio existente
     */
    suspend fun actualizarEjercicio(ejercicio: EjercicioFirestore): Result<Unit> {
        return try {
            if (ejercicio.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("El ejercicio debe tener un ID"))
            }
            ejerciciosCollection
                .document(ejercicio.id)
                .update(ejercicio.toMap() as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar un ejercicio
     */
    suspend fun eliminarEjercicio(ejercicioId: String): Result<Unit> {
        return try {
            ejerciciosCollection.document(ejercicioId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar ejercicios iniciales desde Room
     * Solo se debe llamar una vez para poblar la base de datos
     */
    suspend fun migrarEjerciciosIniciales(ejercicios: List<com.example.gimnasiopro.data.Ejercicio>): Result<Int> {
        return try {
            var count = 0
            ejercicios.forEach { ejercicio ->
                val ejercicioFirestore = EjercicioFirestore(
                    id = ejercicio.id.toString(),
                    grupoMuscular = ejercicio.grupoMuscular,
                    nombre = ejercicio.nombre,
                    descripcion = ejercicio.descripcion,
                    imagenUrl = ejercicio.imagenUrl,
                    esPredefinido = true,
                    fechaCreacion = java.util.Date()
                )
                val docRef = ejerciciosCollection.document(ejercicioFirestore.id)
                docRef.set(ejercicioFirestore.toMap()).await()
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
