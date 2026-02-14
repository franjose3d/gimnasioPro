package com.example.gimnasiopro.data.repository

import com.example.gimnasiopro.domain.model.Cliente
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository para Clientes.
 *
 * GUARDA EN 2 COLECCIONES:
 * 1. users/ → Datos comunes (para búsqueda rápida por tipo)
 * 2. clientes/ → Datos específicos del cliente
 */
class ClienteRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val clientesCollection = firestore.collection("clientes")

    /**
     * Registrar un nuevo cliente
     *
     * Validaciones:
     * - Email único
     * - Teléfono único
     *
     * Guarda en:
     * - users/ (datos comunes)
     * - clientes/ (datos específicos)
     */
    suspend fun registerCliente(cliente: Cliente): Result<Unit> {
        return try {
            // VALIDACIÓN: Teléfono único
            val telefonoExistente = clientesCollection
                .whereEqualTo("telefono", cliente.telefono)
                .get()
                .await()

            if (!telefonoExistente.isEmpty) {
                return Result.failure(
                    IllegalArgumentException("Ya existe un cliente con el teléfono ${cliente.telefono}")
                )
            }

            // GUARDAR EN users/ (colección común)
            usersCollection
                .document(cliente.userId)
                .set(cliente.toUserMap(), SetOptions.merge())
                .await()

            // GUARDAR EN clientes/ (colección específica)
            clientesCollection
                .document(cliente.userId)
                .set(cliente.toClienteMap())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener cliente por ID
     */
    suspend fun getCliente(userId: String): Result<Cliente?> {
        return try {
            val document = clientesCollection
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val cliente = Cliente.fromMap(document.data!!)
                Result.success(cliente)
            } else {
                Result.success(null)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar cliente
     */
    suspend fun updateCliente(cliente: Cliente): Result<Unit> {
        return try {
            // Actualizar en users/
            usersCollection
                .document(cliente.userId)
                .set(cliente.toUserMap(), SetOptions.merge())
                .await()

            // Actualizar en clientes/
            clientesCollection
                .document(cliente.userId)
                .set(cliente.toClienteMap(), SetOptions.merge())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Asignar trainer a cliente
     */
    suspend fun assignTrainer(clienteId: String, trainerId: String): Result<Unit> {
        return try {
            clientesCollection
                .document(clienteId)
                .update("trainerId", trainerId)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener todos los clientes de un trainer
     */
    suspend fun getClientesByTrainer(trainerId: String): Result<List<Cliente>> {
        return try {
            val documents = clientesCollection
                .whereEqualTo("trainerId", trainerId)
                .whereEqualTo("activo", true)
                .get()
                .await()

            val clientes = documents.mapNotNull { doc ->
                try {
                    Cliente.fromMap(doc.data)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(clientes)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar verificación de email
     */
    suspend fun updateEmailVerified(userId: String, verified: Boolean): Result<Unit> {
        return try {
            // Actualizar en users/
            usersCollection
                .document(userId)
                .update("emailVerificado", verified)
                .await()

            // Actualizar en clientes/
            clientesCollection
                .document(userId)
                .update("emailVerificado", verified)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
