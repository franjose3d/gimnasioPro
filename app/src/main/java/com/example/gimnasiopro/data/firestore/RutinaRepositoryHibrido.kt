package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.example.gimnasiopro.data.Rutina
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repositorio híbrido para rutinas.
 *
 * Combina Room (local) con Firebase (remoto) para:
 * - Mantener las rutinas disponibles offline
 * - Sincronizar con Firebase para que el trainer pueda verlas
 * - Permitir que el trainer cree/modifique rutinas para clientes
 */
class RutinaRepositoryHibrido(
    private val localRepository: RutinaRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "RutinaRepoHibrido"
    }

    /**
     * Obtiene la colección de rutinas para un usuario específico.
     * Las rutinas se guardan como subcolección: clientes/{userId}/rutinas/
     * o trainers/{trainerId}/rutinas/
     */
    private fun getRutinasCollection(userId: String, tipoUsuario: String = "cliente"): com.google.firebase.firestore.CollectionReference {
        val coleccionBase = if (tipoUsuario == "trainer") "trainers" else "clientes"
        return firestore.collection(coleccionBase).document(userId).collection("rutinas")
    }

    /**
     * Obtiene la colección de rutinas del usuario actual.
     */
    private suspend fun getRutinasCollectionActual(): com.google.firebase.firestore.CollectionReference? {
        val userId = auth.currentUser?.uid ?: return null
        val tipoUsuario = obtenerTipoUsuario(userId)
        return getRutinasCollection(userId, tipoUsuario)
    }

    // ==================== OBTENER RUTINAS ====================

    /**
     * Obtener todas las rutinas del usuario actual.
     * Primero carga de local, luego sincroniza con Firebase.
     */
    fun getAllRutinas(): Flow<List<Rutina>> = localRepository.getAllRutinas()

    /**
     * Obtener una rutina por su número.
     */
    fun getRutinaByNumero(numeroRutina: Int): Flow<Rutina?> =
        localRepository.getRutinaByNumero(numeroRutina)

    /**
     * Alias para compatibilidad - obtiene una rutina por su número como Flow.
     */
    fun getRutinaByIdFlow(numeroRutina: Int): Flow<Rutina?> =
        localRepository.getRutinaByIdFlow(numeroRutina)

    /**
     * Obtener número de ejercicios en una rutina.
     */
    suspend fun getNumeroEjerciciosEnRutina(numeroRutina: Int): Int {
        val rutina = localRepository.getRutinaByNumeroSync(numeroRutina)
        return rutina?.ejercicioIds?.size ?: 0
    }

    /**
     * Obtener rutinas desde Firebase para el usuario actual.
     */
    fun getRutinasFromFirebase(): Flow<List<RutinaFirestore>> = flow {
        val userId = auth.currentUser?.uid ?: run {
            emit(emptyList())
            return@flow
        }

        val tipoUsuario = obtenerTipoUsuario(userId)
        val rutinasCollection = getRutinasCollection(userId, tipoUsuario)

        val snapshot = rutinasCollection
            .get()
            .await()

        val rutinas = snapshot.documents
            .mapNotNull { RutinaFirestore.fromDocument(it) }
            .sortedByDescending { it.fechaCreacion }

        emit(rutinas)
    }

    // ==================== GUARDAR RUTINAS ====================

    /**
     * Guardar rutina en local y sincronizar con Firebase.
     */
    suspend fun saveRutina(rutina: Rutina): Result<Unit> {
        return try {
            // 1. Guardar en local
            localRepository.insertRutina(rutina)

            // 2. Sincronizar con Firebase
            sincronizarRutinaConFirebase(rutina)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando rutina", e)
            Result.failure(e)
        }
    }

    /**
     * Limpiar todos los ejercicios de una rutina y sincronizar.
     */
    suspend fun limpiarEjerciciosDeRutina(numeroRutina: Int): Result<Unit> {
        return try {
            // 1. Limpiar en local
            localRepository.limpiarEjerciciosDeRutina(numeroRutina)

            // 2. Obtener la rutina actualizada
            val rutina = localRepository.getRutinaByNumeroSync(numeroRutina)

            // 3. Sincronizar con Firebase
            rutina?.let { sincronizarRutinaConFirebase(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando rutina", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar ejercicios específicos de una rutina y sincronizar.
     * @param numeroRutina Número de la rutina (1-10)
     * @param ejercicioIdsAEliminar Lista de IDs de ejercicios a eliminar
     * @return Número de ejercicios eliminados
     */
    suspend fun eliminarEjerciciosDeRutina(numeroRutina: Int, ejercicioIdsAEliminar: List<Long>): Int {
        return try {
            // 1. Eliminar en local
            val eliminados = localRepository.eliminarEjerciciosDeRutina(numeroRutina, ejercicioIdsAEliminar)

            // 2. Obtener la rutina actualizada
            val rutina = localRepository.getRutinaByNumeroSync(numeroRutina)

            // 3. Sincronizar con Firebase
            rutina?.let { sincronizarRutinaConFirebase(it) }

            eliminados
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando ejercicios", e)
            0
        }
    }

    /**
     * Actualizar ejercicios de una rutina y sincronizar.
     */
    suspend fun actualizarEjerciciosDeRutina(
        numeroRutina: Int,
        ejercicioIds: List<Int>
    ): Result<Unit> {
        return try {
            // 1. Actualizar en local
            localRepository.actualizarEjerciciosDeRutina(numeroRutina, ejercicioIds)

            // 2. Obtener la rutina actualizada
            val rutina = localRepository.getRutinaByNumeroSync(numeroRutina)

            // 3. Sincronizar con Firebase
            rutina?.let { sincronizarRutinaConFirebase(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando ejercicios", e)
            Result.failure(e)
        }
    }

    /**
     * Agregar ejercicios a una rutina existente y sincronizar.
     */
    suspend fun agregarEjerciciosARutina(
        numeroRutina: Int,
        nuevosEjercicioIds: List<Int>
    ): RutinaRepository.ResultadoAgregarEjercicios {
        // 1. Agregar en local
        val resultado = localRepository.agregarEjerciciosARutina(numeroRutina, nuevosEjercicioIds)

        // 2. Si fue exitoso, sincronizar con Firebase
        if (resultado.exito) {
            val rutina = localRepository.getRutinaByNumeroSync(numeroRutina)
            rutina?.let {
                try {
                    sincronizarRutinaConFirebase(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando con Firebase", e)
                }
            }
        }

        return resultado
    }

    // ==================== SINCRONIZACIÓN ====================

    /**
     * Sincronizar una rutina local con Firebase.
     * Guarda en clientes/{userId}/rutinas/ o trainers/{userId}/rutinas/
     *
     * SISTEMA DE IDs FIJOS:
     * - Cada rutina usa un ID fijo: "rutina_{numeroRutina}"
     * - Esto evita duplicaciones: siempre se sobreescribe el mismo documento
     * - El numeroRutina (1-20) es inmutable
     *
     * IMPORTANTE: Respeta el SyncManager:
     * - En modo entrenamiento: NO sincroniza (solo local)
     * - En modo normal: Sincroniza inmediatamente
     */
    private suspend fun sincronizarRutinaConFirebase(rutina: Rutina) {
        // Verificar si la sincronización está habilitada
        if (SyncManager.isTrainingMode) {
            Log.d(TAG, "🏋️ Modo entrenamiento activo - sincronización de rutina ${rutina.numeroRutina} OMITIDA")
            return
        }

        if (!SyncManager.isSyncEnabled) {
            Log.d(TAG, "⏸️ Sincronización deshabilitada - rutina ${rutina.numeroRutina} guardada solo localmente")
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "❌ No hay usuario autenticado para sincronizar rutina")
            return
        }

        // Validar número de rutina en rango permitido
        if (rutina.numeroRutina !in 1..RutinaFirestore.MAX_RUTINAS) {
            Log.e(TAG, "❌ Número de rutina ${rutina.numeroRutina} fuera de rango (1-${RutinaFirestore.MAX_RUTINAS})")
            return
        }

        Log.d(TAG, "🔄 Sincronizando rutina ${rutina.numeroRutina} para usuario $userId...")

        try {
            val tipoUsuario = obtenerTipoUsuario(userId)
            Log.d(TAG, "📋 Tipo de usuario detectado: $tipoUsuario")

            val rutinasCollection = getRutinasCollection(userId, tipoUsuario)

            // Usar ID fijo basado en el número de rutina
            val documentId = RutinaFirestore.generarDocumentId(rutina.numeroRutina)
            Log.d(TAG, "📁 Documento destino: ${tipoUsuario}s/$userId/rutinas/$documentId")

            val rutinaFirestore = RutinaFirestore(
                rutinaId = documentId,
                numeroRutina = rutina.numeroRutina,
                nombre = rutina.nombre,
                propietarioId = userId,
                creadoPorId = userId,
                creadoPorTipo = tipoUsuario,
                compartidaConTrainer = true,
                ejercicioIds = rutina.ejercicioIds.map { it.toString() },
                activa = true,
                fechaCreacion = Date(rutina.fechaCreacion),
                fechaModificacion = Date(rutina.fechaModificacion)
            )

            // Usar set() con el ID fijo - esto crea o sobreescribe, NUNCA duplica
            rutinasCollection.document(documentId)
                .set(rutinaFirestore.toMap())
                .await()

            Log.d(TAG, "✅ Rutina ${rutina.numeroRutina} guardada en Firebase: ${tipoUsuario}s/$userId/rutinas/$documentId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando rutina ${rutina.numeroRutina} con Firebase: ${e.message}", e)
        }
    }

    /**
     * @deprecated Ya no es necesario buscar - usamos IDs fijos
     * Se mantiene solo por compatibilidad con código existente
     */
    private suspend fun buscarRutinaEnFirebase(userId: String, tipoUsuario: String, numeroRutina: Int): String? {
        // Ahora simplemente retornamos el ID fijo
        return RutinaFirestore.generarDocumentId(numeroRutina)
    }

    /**
     * Obtener el tipo de usuario (cliente o trainer).
     * Usa UserHelper para buscar en clientes/trainers directamente.
     */
    private suspend fun obtenerTipoUsuario(userId: String): String {
        return UserHelper.getTipoUsuario(userId)
    }

    /**
     * Migrar todas las rutinas locales a Firebase.
     * Se ejecuta cuando el usuario inicia sesión.
     */
    suspend fun migrarRutinasAFirebase(): Result<Int> {
        val userId = auth.currentUser?.uid ?: return Result.failure(
            IllegalStateException("Usuario no autenticado")
        )

        return try {
            val rutinasLocales = localRepository.getAllRutinas().first()
            var migradasCount = 0

            rutinasLocales.forEach { rutina ->
                if (rutina.ejercicioIds.isNotEmpty()) {
                    sincronizarRutinaConFirebase(rutina)
                    migradasCount++
                }
            }

            Log.d(TAG, "Migradas $migradasCount rutinas a Firebase")
            Result.success(migradasCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error migrando rutinas", e)
            Result.failure(e)
        }
    }

    /**
     * Descargar rutinas desde Firebase al dispositivo local.
     * Útil cuando el usuario instala en un nuevo dispositivo.
     */
    suspend fun descargarRutinasDeFirebase(): Result<Int> {
        val userId = auth.currentUser?.uid ?: return Result.failure(
            IllegalStateException("Usuario no autenticado")
        )

        return try {
            val tipoUsuario = obtenerTipoUsuario(userId)
            val rutinasCollection = getRutinasCollection(userId, tipoUsuario)

            val snapshot = rutinasCollection
                .get()
                .await()

            var descargadasCount = 0

            snapshot.documents.forEach { doc ->
                val rutinaFirestore = RutinaFirestore.fromDocument(doc) ?: return@forEach


                val numeroRutina = rutinaFirestore.numeroRutina

                if (numeroRutina in 1..RutinaFirestore.MAX_RUTINAS) {
                    val rutinaLocal = Rutina(
                        numeroRutina = numeroRutina,
                        nombre = rutinaFirestore.nombre,
                        ejercicioIds = rutinaFirestore.ejercicioIds.mapNotNull { it.toIntOrNull() },
                        fechaCreacion = rutinaFirestore.fechaCreacion.time,
                        fechaModificacion = rutinaFirestore.fechaModificacion.time
                    )
                    localRepository.insertRutina(rutinaLocal)
                    descargadasCount++
                }
            }

            Log.d(TAG, "Descargadas $descargadasCount rutinas de Firebase")
            Result.success(descargadasCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando rutinas", e)
            Result.failure(e)
        }
    }

    // ==================== FUNCIONES PARA TRAINERS ====================

    /**
     * Obtener rutinas de un cliente (solo si el trainer tiene permiso).
     * Las rutinas están en: clientes/{clienteId}/rutinas/
     */
    suspend fun getRutinasDeCliente(
        clienteId: String,
        trainerId: String
    ): Result<List<RutinaFirestore>> {
        return try {
            // Verificar que el trainer tiene conexión activa con el cliente
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para ver las rutinas de este cliente"))
            }

            // Las rutinas del cliente están en clientes/{clienteId}/rutinas/
            // El trainer puede ver TODAS las rutinas del cliente (no solo las compartidas)
            val rutinasCollection = getRutinasCollection(clienteId, "cliente")
            val snapshot = rutinasCollection
                .get()
                .await()

            val rutinas = snapshot.documents
                .mapNotNull { RutinaFirestore.fromDocument(it) }
                .sortedByDescending { it.fechaCreacion }

            Result.success(rutinas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear una rutina para un cliente (como trainer).
     * Se guarda en: clientes/{clienteId}/rutinas/
     *
     * SISTEMA DE IDs FIJOS:
     * - Usa el numeroRutina para generar un ID fijo
     * - Si la rutina ya existe, se sobreescribe
     */
    suspend fun crearRutinaParaCliente(
        trainerId: String,
        clienteId: String,
        numeroRutina: Int,
        nombre: String,
        ejercicioIds: List<String>
    ): Result<String> {
        return try {
            // Verificar permiso
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para crear rutinas para este cliente"))
            }

            // Validar número de rutina
            if (numeroRutina !in 1..RutinaFirestore.MAX_RUTINAS) {
                return Result.failure(IllegalArgumentException("Número de rutina debe estar entre 1 y ${RutinaFirestore.MAX_RUTINAS}"))
            }

            // Generar ID fijo
            val documentId = RutinaFirestore.generarDocumentId(numeroRutina)

            val rutina = RutinaFirestore(
                rutinaId = documentId,
                numeroRutina = numeroRutina,
                nombre = nombre,
                propietarioId = clienteId,
                creadoPorId = trainerId,
                creadoPorTipo = "trainer",
                trainerId = trainerId,
                compartidaConTrainer = true,
                ejercicioIds = ejercicioIds,
                activa = true,
                fechaCreacion = Date(),
                fechaModificacion = Date()
            )

            // Guardar en la colección del cliente usando ID fijo (set() en lugar de add())
            val rutinasCollection = getRutinasCollection(clienteId, "cliente")
            rutinasCollection.document(documentId)
                .set(rutina.toMap())
                .await()

            Log.d(TAG, "✅ Rutina $numeroRutina creada para cliente $clienteId por trainer $trainerId: $documentId")
            Result.success(documentId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando rutina para cliente: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener una rutina específica de un cliente (para modo trainer).
     * Lee directamente de Firestore: clientes/{clienteId}/rutinas/{documentId}
     * NO toca Room local.
     */
    suspend fun getRutinaDeClientePorNumero(
        clienteId: String,
        trainerId: String,
        numeroRutina: Int
    ): RutinaFirestore? {
        return try {
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) return null

            val documentId = RutinaFirestore.generarDocumentId(numeroRutina)
            val doc = firestore.collection("clientes")
                .document(clienteId)
                .collection("rutinas")
                .document(documentId)
                .get()
                .await()

            RutinaFirestore.fromDocument(doc)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo rutina $numeroRutina del cliente $clienteId: ${e.message}")
            null
        }
    }

    /**
     * Limpiar ejercicios de una rutina de un cliente (como trainer).
     * Opera SOLO contra Firestore del cliente, NO toca Room local del trainer.
     */
    suspend fun limpiarEjerciciosDeRutinaCliente(
        clienteId: String,
        trainerId: String,
        numeroRutina: Int
    ): Result<Unit> {
        return try {
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para modificar las rutinas de este cliente"))
            }

            val documentId = RutinaFirestore.generarDocumentId(numeroRutina)
            firestore.collection("clientes")
                .document(clienteId)
                .collection("rutinas")
                .document(documentId)
                .update(
                    mapOf(
                        "ejercicioIds" to emptyList<String>(),
                        "fechaModificacion" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()

            Log.d(TAG, "✅ Rutina $numeroRutina del cliente $clienteId limpiada por trainer $trainerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error limpiando rutina del cliente: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Eliminar ejercicios específicos de una rutina de un cliente (como trainer).
     * Opera SOLO contra Firestore del cliente, NO toca Room local del trainer.
     */
    suspend fun eliminarEjerciciosDeRutinaCliente(
        clienteId: String,
        trainerId: String,
        numeroRutina: Int,
        ejercicioIdsAEliminar: List<Long>
    ): Result<Int> {
        return try {
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para modificar las rutinas de este cliente"))
            }

            // Obtener la rutina actual del cliente
            val rutina = getRutinaDeClientePorNumero(clienteId, trainerId, numeroRutina)
                ?: return Result.failure(IllegalStateException("Rutina no encontrada"))

            // Filtrar los ejercicios que quedan
            val idsAEliminarStr = ejercicioIdsAEliminar.map { it.toString() }.toSet()
            val nuevosEjercicioIds = rutina.ejercicioIds.filterNot { it in idsAEliminarStr }
            val eliminados = rutina.ejercicioIds.size - nuevosEjercicioIds.size

            // Actualizar en Firestore
            val documentId = RutinaFirestore.generarDocumentId(numeroRutina)
            firestore.collection("clientes")
                .document(clienteId)
                .collection("rutinas")
                .document(documentId)
                .update(
                    mapOf(
                        "ejercicioIds" to nuevosEjercicioIds,
                        "fechaModificacion" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()

            Log.d(TAG, "✅ $eliminados ejercicios eliminados de rutina $numeroRutina del cliente $clienteId")
            Result.success(eliminados)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando ejercicios de rutina del cliente: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Agregar ejercicios a una rutina de un cliente (como trainer).
     * Opera SOLO contra Firestore del cliente, NO toca Room local del trainer.
     */
    suspend fun agregarEjerciciosARutinaCliente(
        clienteId: String,
        trainerId: String,
        numeroRutina: Int,
        nuevosEjercicioIds: List<String>
    ): Result<Unit> {
        return try {
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para modificar las rutinas de este cliente"))
            }

            // Obtener la rutina actual del cliente (puede no existir aún)
            val rutina = getRutinaDeClientePorNumero(clienteId, trainerId, numeroRutina)
            val ejerciciosActuales = rutina?.ejercicioIds ?: emptyList()

            // Agregar nuevos ejercicios (evitando duplicados)
            val todosLosIds = ejerciciosActuales.toMutableList()
            nuevosEjercicioIds.forEach { id ->
                if (id !in todosLosIds) {
                    todosLosIds.add(id)
                }
            }

            val documentId = RutinaFirestore.generarDocumentId(numeroRutina)
            val rutinasCollection = getRutinasCollection(clienteId, "cliente")

            if (rutina != null) {
                // Actualizar rutina existente
                rutinasCollection.document(documentId)
                    .update(
                        mapOf(
                            "ejercicioIds" to todosLosIds,
                            "fechaModificacion" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()
            } else {
                // Crear nueva rutina para el cliente
                val nuevaRutina = RutinaFirestore(
                    rutinaId = documentId,
                    numeroRutina = numeroRutina,
                    nombre = "Rutina $numeroRutina",
                    propietarioId = clienteId,
                    creadoPorId = trainerId,
                    creadoPorTipo = "trainer",
                    trainerId = trainerId,
                    compartidaConTrainer = true,
                    ejercicioIds = todosLosIds,
                    activa = true,
                    fechaCreacion = Date(),
                    fechaModificacion = Date()
                )
                rutinasCollection.document(documentId)
                    .set(nuevaRutina.toMap())
                    .await()
            }

            Log.d(TAG, "✅ ${nuevosEjercicioIds.size} ejercicios agregados a rutina $numeroRutina del cliente $clienteId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error agregando ejercicios a rutina del cliente: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verificar si un trainer tiene permiso sobre un cliente.
     */
    private suspend fun verificarPermisoTrainer(trainerId: String, clienteId: String): Boolean {
        return try {
            val snapshot = firestore.collection("conexiones")
                .whereEqualTo("trainerId", trainerId)
                .whereEqualTo("clienteId", clienteId)
                .whereEqualTo("estado", "activa")
                .get()
                .await()

            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}

