package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repositorio para gestionar conexiones entre Trainers y Clientes.
 *
 * Permite:
 * - Clientes solicitar conexión con trainers
 * - Trainers aceptar/rechazar solicitudes
 * - Ver clientes conectados (para trainers)
 * - Ver trainer asignado (para clientes)
 */
class ConexionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val conexionesCollection = firestore.collection("conexiones")

    // ==================== SOLICITUDES ====================

    /**
     * Cliente solicita conexión con un trainer
     */
    suspend fun solicitarConexion(
        clienteId: String,
        trainerId: String,
        mensaje: String = ""
    ): Result<String> {
        return try {
            // Verificar si ya existe una conexión activa o pendiente
            val existente = getConexionEntreUsuarios(clienteId, trainerId)
            if (existente != null && existente.estado in listOf(
                ConexionTrainerCliente.ESTADO_PENDIENTE,
                ConexionTrainerCliente.ESTADO_ACTIVA
            )) {
                return Result.failure(IllegalStateException("Ya existe una conexión con este trainer"))
            }

            val conexion = ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = clienteId,
                estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
                solicitadoPor = "cliente",
                fechaSolicitud = Date(),
                mensaje = mensaje
            )

            val docRef = conexionesCollection.add(conexion.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trainer acepta una solicitud de conexión
     */
    suspend fun aceptarSolicitud(conexionId: String): Result<Unit> {
        return try {
            conexionesCollection.document(conexionId)
                .update(mapOf(
                    "estado" to ConexionTrainerCliente.ESTADO_ACTIVA,
                    "fechaRespuesta" to com.google.firebase.Timestamp(Date())
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trainer rechaza una solicitud de conexión
     */
    suspend fun rechazarSolicitud(conexionId: String): Result<Unit> {
        return try {
            conexionesCollection.document(conexionId)
                .update(mapOf(
                    "estado" to ConexionTrainerCliente.ESTADO_RECHAZADA,
                    "fechaRespuesta" to com.google.firebase.Timestamp(Date())
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finalizar una conexión activa (cliente o trainer)
     */
    suspend fun finalizarConexion(conexionId: String): Result<Unit> {
        return try {
            conexionesCollection.document(conexionId)
                .update(mapOf(
                    "estado" to ConexionTrainerCliente.ESTADO_FINALIZADA,
                    "fechaRespuesta" to com.google.firebase.Timestamp(Date())
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CONSULTAS PARA TRAINER ====================

    /**
     * Obtener solicitudes pendientes para un trainer
     */
    fun getSolicitudesPendientes(trainerId: String): Flow<List<ConexionTrainerCliente>> = flow {
        val snapshot = conexionesCollection
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_PENDIENTE)
            .get()
            .await()

        val solicitudes = snapshot.documents
            .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
            .sortedByDescending { it.fechaSolicitud }
        emit(solicitudes)
    }

    /**
     * Obtener clientes conectados (activos) de un trainer
     */
    fun getClientesConectados(trainerId: String): Flow<List<ConexionTrainerCliente>> = flow {
        val snapshot = conexionesCollection
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_ACTIVA)
            .get()
            .await()

        val conexiones = snapshot.documents
            .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
            .sortedByDescending { it.fechaRespuesta ?: it.fechaSolicitud }
        emit(conexiones)
    }

    // ==================== CONSULTAS PARA CLIENTE ====================

    /**
     * Obtener el trainer conectado de un cliente (si tiene)
     */
    fun getTrainerConectado(clienteId: String): Flow<ConexionTrainerCliente?> = flow {
        val snapshot = conexionesCollection
            .whereEqualTo("clienteId", clienteId)
            .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_ACTIVA)
            .get()
            .await()

        val conexion = snapshot.documents
            .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
            .firstOrNull()
        emit(conexion)
    }

    /**
     * Obtener estado de solicitud del cliente
     */
    fun getSolicitudPendienteDeCliente(clienteId: String): Flow<ConexionTrainerCliente?> = flow {
        val snapshot = conexionesCollection
            .whereEqualTo("clienteId", clienteId)
            .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_PENDIENTE)
            .get()
            .await()

        val conexion = snapshot.documents
            .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
            .firstOrNull()
        emit(conexion)
    }

    // ==================== UTILIDADES ====================

    /**
     * Verificar si existe conexión entre cliente y trainer
     */
    private suspend fun getConexionEntreUsuarios(
        clienteId: String,
        trainerId: String
    ): ConexionTrainerCliente? {
        val snapshot = conexionesCollection
            .whereEqualTo("clienteId", clienteId)
            .whereEqualTo("trainerId", trainerId)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
            .firstOrNull { it.estado in listOf(
                ConexionTrainerCliente.ESTADO_PENDIENTE,
                ConexionTrainerCliente.ESTADO_ACTIVA
            )}
    }

    /**
     * Verificar si un trainer tiene permiso sobre un cliente
     */
    suspend fun trainerTienePermisoSobreCliente(trainerId: String, clienteId: String): Boolean {
        val snapshot = conexionesCollection
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("clienteId", clienteId)
            .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_ACTIVA)
            .get()
            .await()

        return snapshot.documents.isNotEmpty()
    }

    /**
     * Obtener conexión por ID
     */
    suspend fun getConexionById(conexionId: String): ConexionTrainerCliente? {
        return try {
            val document = conexionesCollection.document(conexionId).get().await()
            ConexionTrainerCliente.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }
}

