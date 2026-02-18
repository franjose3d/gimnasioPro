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

        Log.d(TAG, "🔄 Sincronizando rutina ${rutina.numeroRutina} para usuario $userId...")

        try {
            val tipoUsuario = obtenerTipoUsuario(userId)
            Log.d(TAG, "📋 Tipo de usuario detectado: $tipoUsuario")

            val rutinasCollection = getRutinasCollection(userId, tipoUsuario)
            Log.d(TAG, "📁 Colección destino: ${tipoUsuario}s/$userId/rutinas")

            // Buscar si ya existe esta rutina en Firebase
            val rutinaFirebaseId = buscarRutinaEnFirebase(userId, tipoUsuario, rutina.numeroRutina)

            val rutinaFirestore = RutinaFirestore(
                rutinaId = rutinaFirebaseId ?: "",
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

            if (rutinaFirebaseId != null) {
                // Actualizar existente
                rutinasCollection.document(rutinaFirebaseId)
                    .update(rutinaFirestore.toMap().filterValues { it != null } as Map<String, Any>)
                    .await()
                Log.d(TAG, "✅ Rutina ${rutina.numeroRutina} actualizada en Firebase (${tipoUsuario}s/$userId/rutinas)")
            } else {
                // Crear nueva
                val docRef = rutinasCollection.add(rutinaFirestore.toMap()).await()
                docRef.update("rutinaId", docRef.id).await()
                Log.d(TAG, "✅ Rutina ${rutina.numeroRutina} creada en Firebase: ${docRef.id} (${tipoUsuario}s/$userId/rutinas)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando rutina ${rutina.numeroRutina} con Firebase: ${e.message}", e)
        }
    }

    /**
     * Buscar si una rutina ya existe en Firebase por número.
     */
    private suspend fun buscarRutinaEnFirebase(userId: String, tipoUsuario: String, numeroRutina: Int): String? {
        val rutinasCollection = getRutinasCollection(userId, tipoUsuario)
        val snapshot = rutinasCollection
            .whereEqualTo("nombre", "Rutina $numeroRutina")
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.id
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

                // Extraer número de rutina del nombre (ej: "Rutina 1" -> 1)
                val numeroRutina = rutinaFirestore.nombre
                    .replace("Rutina ", "")
                    .toIntOrNull() ?: return@forEach

                if (numeroRutina in 1..10) {
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
            val rutinasCollection = getRutinasCollection(clienteId, "cliente")
            val snapshot = rutinasCollection
                .whereEqualTo("compartidaConTrainer", true)
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
     */
    suspend fun crearRutinaParaCliente(
        trainerId: String,
        clienteId: String,
        nombre: String,
        ejercicioIds: List<String>
    ): Result<String> {
        return try {
            // Verificar permiso
            val tienePermiso = verificarPermisoTrainer(trainerId, clienteId)
            if (!tienePermiso) {
                return Result.failure(SecurityException("No tienes permiso para crear rutinas para este cliente"))
            }

            val rutina = RutinaFirestore(
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

            // Guardar en la colección del cliente: clientes/{clienteId}/rutinas/
            val rutinasCollection = getRutinasCollection(clienteId, "cliente")
            val docRef = rutinasCollection.add(rutina.toMap()).await()
            docRef.update("rutinaId", docRef.id).await()

            Log.d(TAG, "Rutina creada para cliente $clienteId por trainer $trainerId: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
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

