package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para rutinas en Firestore.
 *
 * Estructura de datos:
 * clientes/{userId}/rutinas/{rutinaId}/...
 * trainers/{trainerId}/rutinas/{rutinaId}/...
 *
 * Las rutinas se guardan como subcolección dentro del usuario correspondiente.
 * Esto permite una mejor organización y seguridad con reglas de Firestore.
 */
class RutinaFirestoreRepository(
    private val userId: String,
    private val tipoUsuario: String = "cliente" // "cliente" o "trainer"
) {

    private val firestore = FirebaseFirestore.getInstance()

    // Colección base según el tipo de usuario
    private val coleccionBase: String
        get() = if (tipoUsuario == "trainer") "trainers" else "clientes"

    // Referencia a la subcolección de rutinas del usuario
    private val rutinasCollection
        get() = firestore.collection(coleccionBase).document(userId).collection("rutinas")

    /**
     * Constructor sin parámetros para compatibilidad con código existente
     * (Se usará temporalmente hasta migrar todo el código)
     */
    constructor() : this("", "cliente")

    /**
     * Obtener todas las rutinas del usuario actual
     */
    fun getRutinasDeUsuario(): Flow<List<RutinaFirestore>> = flow {
        if (userId.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val snapshot = rutinasCollection
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .get()
            .await()

        val rutinas = snapshot.documents
            .mapNotNull { doc -> RutinaFirestore.fromDocument(doc) }
        emit(rutinas)
    }

    /**
     * Obtener todas las rutinas de un usuario específico (para trainers viendo clientes)
     */
    fun getRutinasDeUsuario(otroUserId: String): Flow<List<RutinaFirestore>> = flow {
        val snapshot = firestore
            .collection("clientes")
            .document(otroUserId)
            .collection("rutinas")
            .whereEqualTo("compartidaConTrainer", true)
            .get()
            .await()

        val rutinas = snapshot.documents
            .mapNotNull { doc -> RutinaFirestore.fromDocument(doc) }
            .filter { it.trainerId == null || it.trainerId == userId }
            .sortedByDescending { it.fechaCreacion }
        emit(rutinas)
    }

    /**
     * Obtener rutinas de un cliente que el trainer puede ver.
     * Solo las que tienen compartidaConTrainer = true y trainerId coincide.
     */
    fun getRutinasDeClienteParaTrainer(clienteId: String, trainerId: String): Flow<List<RutinaFirestore>> = flow {
        val snapshot = firestore
            .collection("clientes")
            .document(clienteId)
            .collection("rutinas")
            .whereEqualTo("compartidaConTrainer", true)
            .get()
            .await()

        val rutinas = snapshot.documents
            .mapNotNull { doc -> RutinaFirestore.fromDocument(doc) }
            .filter { it.trainerId == null || it.trainerId == trainerId }
            .sortedByDescending { it.fechaCreacion }
        emit(rutinas)
    }

    /**
     * Obtener rutinas creadas por un trainer (guardadas en su propia colección)
     */
    fun getRutinasCredasPorTrainer(trainerId: String): Flow<List<RutinaFirestore>> = flow {
        val snapshot = firestore
            .collection("trainers")
            .document(trainerId)
            .collection("rutinas")
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .get()
            .await()
        
        val rutinas = snapshot.documents
            .mapNotNull { doc -> RutinaFirestore.fromDocument(doc) }
        emit(rutinas)
    }

    /**
     * Obtener una rutina por ID del usuario actual
     */
    suspend fun getRutinaById(rutinaId: String): RutinaFirestore? {
        if (userId.isEmpty()) return null

        return try {
            val document = rutinasCollection.document(rutinaId).get().await()
            RutinaFirestore.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtener una rutina de cualquier usuario por su ID (para trainers)
     */
    suspend fun getRutinaDeCliente(clienteId: String, rutinaId: String): RutinaFirestore? {
        return try {
            val document = firestore
                .collection("clientes")
                .document(clienteId)
                .collection("rutinas")
                .document(rutinaId)
                .get()
                .await()
            RutinaFirestore.fromDocument(document)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crear una nueva rutina para el usuario actual
     */
    suspend fun crearRutina(rutina: RutinaFirestore): Result<String> {
        if (userId.isEmpty()) {
            return Result.failure(IllegalStateException("userId no puede estar vacío"))
        }

        return try {
            val rutinaCompleta = rutina.copy(
                propietarioId = userId,
                creadoPorId = userId,
                creadoPorTipo = tipoUsuario,
                fechaCreacion = java.util.Date(),
                fechaModificacion = java.util.Date()
            )
            val docRef = rutinasCollection.add(rutinaCompleta.toMap()).await()

            // Actualizar con el ID generado
            docRef.update("rutinaId", docRef.id).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear una nueva rutina (con parámetros legacy para compatibilidad)
     */
    suspend fun crearRutina(
        rutina: RutinaFirestore,
        userId: String,
        tipoUsuario: String
    ): Result<String> {
        return try {
            val coleccion = if (tipoUsuario == "trainer") "trainers" else "clientes"
            val rutinaCompleta = rutina.copy(
                propietarioId = userId,
                creadoPorId = userId,
                creadoPorTipo = tipoUsuario,
                fechaCreacion = java.util.Date(),
                fechaModificacion = java.util.Date()
            )

            val docRef = firestore
                .collection(coleccion)
                .document(userId)
                .collection("rutinas")
                .add(rutinaCompleta.toMap())
                .await()

            // Actualizar con el ID generado
            docRef.update("rutinaId", docRef.id).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear rutina como trainer para un cliente (se guarda en la colección del cliente)
     */
    suspend fun crearRutinaParaCliente(
        rutina: RutinaFirestore,
        trainerId: String,
        clienteId: String
    ): Result<String> {
        return try {
            val rutinaCompleta = rutina.copy(
                propietarioId = clienteId,      // El cliente es el dueño
                creadoPorId = trainerId,        // El trainer la creó
                creadoPorTipo = "trainer",
                trainerId = trainerId,
                compartidaConTrainer = true,
                fechaCreacion = java.util.Date(),
                fechaModificacion = java.util.Date()
            )

            // Guardar en la subcolección del cliente
            val docRef = firestore
                .collection("clientes")
                .document(clienteId)
                .collection("rutinas")
                .add(rutinaCompleta.toMap())
                .await()

            docRef.update("rutinaId", docRef.id).await()
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
                .update(rutinaActualizada.toMap().filterValues { it != null } as Map<String, Any>)
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
     * Asignar un trainer a una rutina del cliente
     */
    suspend fun asignarTrainerARutina(rutinaId: String, trainerId: String): Result<Unit> {
        return try {
            rutinasCollection
                .document(rutinaId)
                .update(
                    mapOf(
                        "trainerId" to trainerId,
                        "compartidaConTrainer" to true,
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

    /**
     * Verificar si el usuario tiene permiso para editar la rutina
     */
    suspend fun tienePermisoEditar(rutinaId: String, userId: String, tipoUsuario: String): Boolean {
        val rutina = getRutinaById(rutinaId) ?: return false

        return when {
            // El propietario siempre puede editar
            rutina.propietarioId == userId -> true
            // El trainer asignado puede editar si la rutina está compartida
            tipoUsuario == "trainer" && rutina.trainerId == userId && rutina.compartidaConTrainer -> true
            // El creador (trainer) puede editar
            rutina.creadoPorId == userId -> true
            else -> false
        }
    }
}
