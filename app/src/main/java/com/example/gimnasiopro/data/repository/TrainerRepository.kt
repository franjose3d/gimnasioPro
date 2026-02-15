package com.example.gimnasiopro.data.repository

import com.example.gimnasiopro.domain.model.Trainer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository para Trainers.
 *
 * GUARDA SOLO EN:
 * trainers/{userId} → Datos completos del trainer + subcolecciones (rutinas, estadisticas, etc.)
 */
class TrainerRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val trainersCollection = firestore.collection("trainers")

    /**
     * Registrar un nuevo trainer
     *
     * Validaciones:
     * - DNI único
     * - Teléfono único
     *
     * Guarda en:
     * - users/ (datos comunes)
     * - trainers/ (datos específicos)
     */
    suspend fun registerTrainer(trainer: Trainer): Result<Unit> {
        return try {
            // VALIDACIÓN 1: DNI único
            val dniExistente = trainersCollection
                .whereEqualTo("dni", trainer.dni)
                .get()
                .await()

            if (!dniExistente.isEmpty) {
                return Result.failure(
                    IllegalArgumentException("Ya existe un entrenador con el DNI ${trainer.dni}")
                )
            }

            // VALIDACIÓN 2: Teléfono único
            val telefonoExistente = trainersCollection
                .whereEqualTo("telefono", trainer.telefono)
                .get()
                .await()

            if (!telefonoExistente.isEmpty) {
                return Result.failure(
                    IllegalArgumentException("Ya existe un entrenador con el teléfono ${trainer.telefono}")
                )
            }

            // GUARDAR EN trainers/ (todos los datos del trainer)
            val datosTrainer = trainer.toTrainerMap().toMutableMap()
            datosTrainer["tipo"] = "trainer"

            trainersCollection
                .document(trainer.userId)
                .set(datosTrainer)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener trainer por ID
     */
    suspend fun getTrainer(userId: String): Result<Trainer?> {
        return try {
            val document = trainersCollection
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val trainer = Trainer.fromMap(document.data!!)
                Result.success(trainer)
            } else {
                Result.success(null)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar trainer
     */
    suspend fun updateTrainer(trainer: Trainer): Result<Unit> {
        return try {
            // Actualizar en trainers/ (incluye todos los datos)
            val datosCompletos = trainer.toTrainerMap().toMutableMap()
            datosCompletos["tipo"] = "trainer"

            trainersCollection
                .document(trainer.userId)
                .set(datosCompletos, SetOptions.merge())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar solo el estado de verificación de email
     */
    suspend fun updateEmailVerified(userId: String, verified: Boolean): Result<Unit> {
        return try {
            // Actualizar solo en trainers/
            trainersCollection
                .document(userId)
                .update("emailVerificado", verified)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Añadir cliente a la lista de clientes activos del trainer
     */
    suspend fun addClienteToTrainer(trainerId: String, clienteId: String): Result<Unit> {
        return try {
            val trainerDoc = trainersCollection.document(trainerId).get().await()
            val trainer = Trainer.fromMap(trainerDoc.data!!)

            val nuevaLista = trainer.clientesActivos.toMutableList()
            if (!nuevaLista.contains(clienteId)) {
                nuevaLista.add(clienteId)

                trainersCollection
                    .document(trainerId)
                    .update(
                        mapOf(
                            "clientesActivos" to nuevaLista,
                            "numeroClientes" to nuevaLista.size
                        )
                    )
                    .await()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener todos los trainers verificados (para que clientes los vean)
     */
    suspend fun getVerifiedTrainers(): Result<List<Trainer>> {
        return try {
            val documents = trainersCollection
                .whereEqualTo("verificado", true)
                .get()
                .await()

            val trainers = documents.mapNotNull { doc ->
                try {
                    Trainer.fromMap(doc.data)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(trainers)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
