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
     * Trainer solicita conexión con un cliente (crea conexión pendiente al invitar)
     */
    suspend fun solicitarConexionComoTrainer(
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
                return Result.failure(IllegalStateException("Ya existe una conexión con este cliente"))
            }

            val conexion = ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = clienteId,
                estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
                solicitadoPor = "trainer",
                fechaSolicitud = Date(),
                mensaje = mensaje
            )

            val docRef = conexionesCollection.add(conexion.toMap()).await()
            android.util.Log.d("ConexionRepository", "✅ Conexión pendiente creada por trainer: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "❌ Error creando conexión como trainer: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Trainer acepta una solicitud de conexión.
     * También actualiza:
     * - trainerId en el documento del cliente
     * - misClientes subcolección del trainer
     */
    suspend fun aceptarSolicitud(conexionId: String): Result<Unit> {
        return try {
            val conexionDoc = conexionesCollection.document(conexionId).get().await()
            val conexion = ConexionTrainerCliente.fromDocument(conexionDoc)
                ?: return Result.failure(IllegalStateException("Conexión no encontrada"))

            // PASO 1: misClientes PRIMERO → activa isTrainerDelCliente()
            firestore.collection("trainers")
                .document(conexion.trainerId)
                .collection("misClientes")
                .document(conexion.clienteId)
                .set(mapOf(
                    "clienteId" to conexion.clienteId,
                    "fechaConexion" to com.google.firebase.Timestamp(Date()),
                    "estado" to "activo"
                ))
                .await()

            // PASO 2: Ahora sí puede actualizar el documento del cliente
            firestore.collection("clientes")
                .document(conexion.clienteId)
                .update("trainerId", conexion.trainerId)
                .await()

            // PASO 3: Marcar conexión como activa
            conexionesCollection.document(conexionId)
                .update(mapOf(
                    "estado" to ConexionTrainerCliente.ESTADO_ACTIVA,
                    "fechaRespuesta" to com.google.firebase.Timestamp(Date())
                ))
                .await()

            // PASO 4: Registrar trainer en miTrainer del cliente
            firestore.collection("clientes")
                .document(conexion.clienteId)
                .collection("miTrainer")
                .document(conexion.trainerId)
                .set(mapOf(
                    "trainerId" to conexion.trainerId,
                    "fechaConexion" to com.google.firebase.Timestamp(Date()),
                    "estado" to "activo"
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "❌ Error aceptando solicitud: ${e.message}")
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
     * Finalizar una conexión activa (cliente o trainer).
     * También limpia:
     * - trainerId en el documento del cliente
     * - misClientes del trainer
     * - miTrainer del cliente
     */
    suspend fun finalizarConexion(conexionId: String): Result<Unit> {
        return try {
            val conexionDoc = conexionesCollection.document(conexionId).get().await()
            val conexion = ConexionTrainerCliente.fromDocument(conexionDoc)
                ?: return Result.failure(IllegalStateException("Conexión no encontrada"))

            // PASO 1: Limpiar trainerId del cliente ANTES de perder el permiso
            firestore.collection("clientes")
                .document(conexion.clienteId)
                .update("trainerId", "")
                .await()

            // PASO 2: Eliminar trainer de miTrainer del cliente
            firestore.collection("clientes")
                .document(conexion.clienteId)
                .collection("miTrainer")
                .document(conexion.trainerId)
                .delete()
                .await()

            // PASO 3: Marcar conexión como finalizada
            conexionesCollection.document(conexionId)
                .update(mapOf(
                    "estado" to ConexionTrainerCliente.ESTADO_FINALIZADA,
                    "fechaRespuesta" to com.google.firebase.Timestamp(Date())
                ))
                .await()

            // PASO 4: Eliminar cliente de misClientes AL FINAL
            firestore.collection("trainers")
                .document(conexion.trainerId)
                .collection("misClientes")
                .document(conexion.clienteId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "❌ Error finalizando conexión: ${e.message}")
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
     * Buscar conexión pendiente entre un cliente y un trainer específico
     */
    suspend fun getConexionPendienteEntreUsuarios(
        clienteId: String,
        trainerId: String
    ): ConexionTrainerCliente? {
        return try {
            val snapshot = conexionesCollection
                .whereEqualTo("clienteId", clienteId)
                .whereEqualTo("trainerId", trainerId)
                .whereEqualTo("estado", ConexionTrainerCliente.ESTADO_PENDIENTE)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
                .firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "Error buscando conexión pendiente: ${e.message}")
            null
        }
    }

    /**
     * Crear una conexión y aceptarla inmediatamente (para invitaciones de trainer aceptadas por cliente)
     */
    suspend fun crearYAceptarConexion(
        clienteId: String,
        trainerId: String
    ): Result<String> {
        return try {
            // PASO 1: misClientes PRIMERO → activa isTrainerDelCliente()
            firestore.collection("trainers")
                .document(trainerId)
                .collection("misClientes")
                .document(clienteId)
                .set(mapOf(
                    "clienteId" to clienteId,
                    "fechaConexion" to com.google.firebase.Timestamp(Date()),
                    "estado" to "activo"
                ))
                .await()

            // PASO 2: Actualizar trainerId en el cliente
            firestore.collection("clientes")
                .document(clienteId)
                .update("trainerId", trainerId)
                .await()

            // PASO 3: Crear la conexión como activa
            val conexion = ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = clienteId,
                estado = ConexionTrainerCliente.ESTADO_ACTIVA,
                solicitadoPor = "trainer",
                fechaSolicitud = Date(),
                fechaRespuesta = Date(),
                mensaje = "Invitación aceptada"
            )
            val docRef = conexionesCollection.add(conexion.toMap()).await()

            // PASO 4: Registrar en miTrainer del cliente
            firestore.collection("clientes")
                .document(clienteId)
                .collection("miTrainer")
                .document(trainerId)
                .set(mapOf(
                    "trainerId" to trainerId,
                    "fechaConexion" to com.google.firebase.Timestamp(Date()),
                    "estado" to "activo"
                ))
                .await()

            android.util.Log.d("ConexionRepository", "✅ Conexión creada y aceptada: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "❌ Error creando conexión: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verificar si existe conexión entre cliente y trainer
     */
    private suspend fun getConexionEntreUsuarios(
        clienteId: String,
        trainerId: String
    ): ConexionTrainerCliente? {
        // Query solo por clienteId para evitar requerir índice compuesto en Firestore.
        // El filtro por trainerId se aplica en memoria.
        return try {
            val snapshot = conexionesCollection
                .whereEqualTo("clienteId", clienteId)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { ConexionTrainerCliente.fromDocument(it) }
                .firstOrNull {
                    it.trainerId == trainerId &&
                    it.estado in listOf(
                        ConexionTrainerCliente.ESTADO_PENDIENTE,
                        ConexionTrainerCliente.ESTADO_ACTIVA
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("ConexionRepository", "Error buscando conexión entre usuarios: ${e.message}")
            null
        }
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

    // ==================== CONSULTAS DESDE SUBCOLECCIONES ====================

    /**
     * Obtener lista de IDs de clientes desde subcolección misClientes del trainer
     */
    suspend fun getMisClientesIds(trainerId: String): List<String> {
        return try {
            val snapshot = firestore.collection("trainers")
                .document(trainerId)
                .collection("misClientes")
                .whereEqualTo("estado", "activo")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.getString("clienteId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Obtener el ID del trainer desde subcolección miTrainer del cliente
     */
    suspend fun getMiTrainerId(clienteId: String): String? {
        return try {
            val snapshot = firestore.collection("clientes")
                .document(clienteId)
                .collection("miTrainer")
                .whereEqualTo("estado", "activo")
                .get()
                .await()

            snapshot.documents.firstOrNull()?.getString("trainerId")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verificar si existe una relación activa en misClientes
     */
    suspend fun existeRelacionEnMisClientes(trainerId: String, clienteId: String): Boolean {
        return try {
            val doc = firestore.collection("trainers")
                .document(trainerId)
                .collection("misClientes")
                .document(clienteId)
                .get()
                .await()

            doc.exists() && doc.getString("estado") == "activo"
        } catch (e: Exception) {
            false
        }
    }
}

