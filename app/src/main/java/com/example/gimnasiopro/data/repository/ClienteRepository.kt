package com.example.gimnasiopro.data.repository

import com.example.gimnasiopro.domain.model.Cliente
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository para Clientes.
 *
 * GUARDA SOLO EN:
 * clientes/{userId} → Datos completos del cliente + subcolecciones (rutinas, estadisticas, etc.)
 */
class ClienteRepository {

    private val firestore = FirebaseFirestore.getInstance()
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

            // GUARDAR EN clientes/ (todos los datos del cliente)
            val datosCliente = cliente.toClienteMap().toMutableMap()
            datosCliente["tipo"] = "cliente"

            clientesCollection
                .document(cliente.userId)
                .set(datosCliente)
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
            // Actualizar en clientes/ (incluye todos los datos)
            val datosCompletos = cliente.toClienteMap().toMutableMap()
            datosCompletos["tipo"] = "cliente"

            clientesCollection
                .document(cliente.userId)
                .set(datosCompletos, SetOptions.merge())
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
