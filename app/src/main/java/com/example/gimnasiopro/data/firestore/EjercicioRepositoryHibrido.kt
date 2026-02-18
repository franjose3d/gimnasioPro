package com.example.gimnasiopro.data.firestore

import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.EjercicioRepository
import com.example.gimnasiopro.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

/**
 * Repositorio híbrido que combina Room (cache local) y Firestore (fuente remota).
 * 
 * Estrategia:
 * - Carga primero desde Room (rápido, offline)
 * - Sincroniza con Firestore en background
 * - Actualiza Room con datos de Firestore
 * 
 * Útil durante la transición de Room a Firestore.
 */
class EjercicioRepositoryHibrido(
    private val localRepository: EjercicioRepository,
    private val remoteRepository: EjercicioFirestoreRepository
) {

    /**
     * Flow de todos los ejercicios (compatible con el EjercicioRepository original).
     */
    val allEjercicios: Flow<List<Ejercicio>>
        get() = obtenerTodosEjercicios()

    /**
     * Flow de todos los grupos musculares (compatible con el EjercicioRepository original).
     */
    val allGruposMusculares: Flow<List<String>>
        get() = obtenerTodosGruposMusculares()


    /**
     * Obtener todos los ejercicios con sincronización híbrida.
     * Primero carga de Room, luego sincroniza con Firestore.
     *
     * IMPORTANTE: Respeta el SyncManager:
     * - En modo entrenamiento: SOLO usa datos locales (Room)
     * - En modo normal: Sincroniza con Firestore
     */
    private fun obtenerTodosEjercicios(): Flow<List<Ejercicio>> = flow {
        // 1. Emitir datos locales primero (inmediato - funciona offline)
        val ejerciciosLocales = localRepository.allEjercicios.first()

        // Si Room está vacío, no emitir lista vacía - esperar a Firebase
        if (ejerciciosLocales.isNotEmpty()) {
            emit(ejerciciosLocales)
        }

        // 2. Si estamos en modo entrenamiento, NO sincronizar con Firestore
        if (SyncManager.isTrainingMode) {
            // En modo entrenamiento solo usamos datos locales
            if (ejerciciosLocales.isEmpty()) {
                emit(emptyList())
            }
            return@flow
        }

        // 3. Sincronizar con Firestore (solo si no estamos en modo entrenamiento)
        try {
            val ejerciciosRemotos = remoteRepository.getAllEjercicios().first()
            
            if (ejerciciosRemotos.isNotEmpty()) {
                // Convertir Firestore a Room y actualizar cache local
                ejerciciosRemotos.forEach { ejercicioFirestore ->
                    val ejercicioRoom = ejercicioFirestore.toRoom()
                    localRepository.updateEjercicio(ejercicioRoom)
                }

                // Emitir datos actualizados desde Room (para mantener consistencia)
                emit(localRepository.allEjercicios.first())
            } else if (ejerciciosLocales.isEmpty()) {
                // Ni Room ni Firebase tienen datos - emitir vacío como último recurso
                emit(emptyList())
            }
        } catch (e: Exception) {
            // Si falla la sincronización, usar datos locales o emitir vacío
            if (ejerciciosLocales.isEmpty()) {
                emit(emptyList()) // Forzar emisión para que UI sepa que terminó la carga
            }
        }
    }

    /**
     * Obtener ejercicios por grupo muscular.
     */
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>> = flow {
        // 1. Emitir datos locales solo si no están vacíos
        val ejerciciosLocales = localRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first()
        if (ejerciciosLocales.isNotEmpty()) {
            emit(ejerciciosLocales)
        }

        // 2. Sincronizar con Firestore (prioritario si Room vacío)
        try {
            val ejerciciosRemotos = remoteRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first()

            if (ejerciciosRemotos.isNotEmpty()) {
                ejerciciosRemotos.forEach { ejercicioFirestore ->
                    val ejercicioRoom = ejercicioFirestore.toRoom()
                    localRepository.updateEjercicio(ejercicioRoom)
                }
                emit(localRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first())
            } else if (ejerciciosLocales.isEmpty()) {
                emit(emptyList())
            }
        } catch (e: Exception) {
            if (ejerciciosLocales.isEmpty()) {
                emit(emptyList())
            }
        }
    }

    /**
     * Buscar ejercicios por nombre.
     */
    fun searchEjercicios(query: String): Flow<List<Ejercicio>> = flow {
        // Buscar primero en local
        val ejerciciosLocales = localRepository.searchEjercicios(query).first()
        emit(ejerciciosLocales)

        // Sincronizar con Firestore
        try {
            val ejerciciosRemotos = remoteRepository.searchEjercicios(query).first()
            ejerciciosRemotos.forEach { ejercicioFirestore ->
                val ejercicioRoom = ejercicioFirestore.toRoom()
                localRepository.updateEjercicio(ejercicioRoom)
            }
            emit(localRepository.searchEjercicios(query).first())
        } catch (e: Exception) {
            // Mantener datos locales
        }
    }

    /**
     * Obtener todos los grupos musculares (función con sincronización).
     */
    private fun obtenerTodosGruposMusculares(): Flow<List<String>> = flow {
        val gruposLocales = localRepository.allGruposMusculares.first()
        emit(gruposLocales)

        // Sincronizar con Firestore
        try {
            val gruposRemotos = remoteRepository.getAllGruposMusculares().first()
            if (gruposRemotos.isNotEmpty()) {
                emit(gruposRemotos)
            }
        } catch (e: Exception) {
            // Mantener datos locales
        }
    }

    // =============== MÉTODOS DE ESCRITURA (delegados al local por ahora) ===============

    /**
     * Obtener ejercicio por ID.
     */
    suspend fun getEjercicioById(id: Long): Ejercicio? {
        return localRepository.getEjercicioById(id)
    }

    /**
     * Insertar nuevo ejercicio (local + Firestore).
     * @param ejercicio El ejercicio a insertar
     * @param creadoPor ID del usuario (trainer) que crea el ejercicio (opcional)
     *                  Si es null, solo se guarda en local (sincronización desde Firebase)
     */
    suspend fun insertEjercicio(ejercicio: Ejercicio, creadoPor: String? = null): Long {
        // Insertar en local primero
        val localId = localRepository.insertEjercicio(ejercicio)

        // Solo subir a Firestore si es un trainer creando un ejercicio nuevo
        // (creadoPor != null significa creación explícita desde UI de trainer)
        if (creadoPor != null) {
            try {
                val ejercicioFirestore = ejercicio.toFirestoreModel(creadoPor)
                // forzarCreacion = false para que verifique duplicados
                remoteRepository.crearEjercicio(ejercicioFirestore, forzarCreacion = false)
            } catch (e: Exception) {
                // Log pero no fallar si Firestore falla
                android.util.Log.e("EjercicioRepoHibrido", "Error al guardar en Firestore", e)
            }
        }

        return localId
    }

    /**
     * Insertar múltiples ejercicios.
     * Solo guarda en local (Room) - NO sube a Firestore.
     * Este método se usa principalmente para sincronización desde Firestore al local,
     * por lo que los ejercicios ya existen en la nube.
     */
    suspend fun insertAllEjercicios(ejercicios: List<Ejercicio>) {
        localRepository.insertAllEjercicios(ejercicios)
        // NO sincronizar con Firestore - esto causaba duplicados
        // Los ejercicios ya vienen de Firestore o son predefinidos
    }

    /**
     * Actualizar ejercicio.
     */
    suspend fun updateEjercicio(ejercicio: Ejercicio) {
        localRepository.updateEjercicio(ejercicio)

        try {
            val ejercicioFirestore = ejercicio.toFirestoreModel()
            remoteRepository.actualizarEjercicio(ejercicioFirestore)
        } catch (e: Exception) {
            // Log pero no fallar
        }
    }

    /**
     * Eliminar ejercicio.
     */
    suspend fun deleteEjercicio(ejercicio: Ejercicio) {
        localRepository.deleteEjercicio(ejercicio)

        try {
            remoteRepository.eliminarEjercicio(ejercicio.grupoMuscular, ejercicio.id.toString())
        } catch (e: Exception) {
            // Log pero no fallar
        }
    }

    /**
     * Eliminar todos los ejercicios.
     */
    suspend fun deleteAllEjercicios() {
        localRepository.deleteAllEjercicios()
        // No eliminamos de Firestore ya que son datos compartidos
    }

    /**
     * Obtener conteo de ejercicios.
     */
    suspend fun getEjerciciosCount(): Int {
        return localRepository.getEjerciciosCount()
    }

    /**
     * Obtener ejercicios por lista de IDs.
     */
    suspend fun getEjerciciosByIds(ids: List<Long>): List<Ejercicio> {
        return localRepository.getEjerciciosByIds(ids)
    }

    /**
     * Obtener conteo de ejercicios por grupo muscular.
     * Se usa para calcular el equilibrio muscular.
     */
    suspend fun getConteoPorGrupoMuscular(ejercicioIds: List<Long>): Map<String, Int> {
        return localRepository.getConteoPorGrupoMuscular(ejercicioIds)
    }
}

/**
 * Extension function para convertir EjercicioFirestore a Ejercicio (Room)
 */
fun EjercicioFirestore.toRoom(): Ejercicio {
    return Ejercicio(
        id = this.id.toLongOrNull() ?: 0L,
        grupoMuscular = this.grupoMuscular,
        nombre = this.nombre,
        descripcion = this.descripcion,
        imagenUrl = this.imagenUrl
    )
}

/**
 * Extension function para convertir Ejercicio (Room) a EjercicioFirestore
 * @param creadoPor ID del trainer que crea el ejercicio (opcional)
 */
fun Ejercicio.toFirestoreModel(creadoPor: String? = null): EjercicioFirestore {
    return EjercicioFirestore(
        id = "", // Dejar vacío para que Firestore genere el ID automáticamente
        grupoMuscular = this.grupoMuscular,
        nombre = this.nombre,
        descripcion = this.descripcion,
        imagenUrl = this.imagenUrl,
        creadoPor = creadoPor, // ID del trainer que lo creó (guardado en campo separado)
        esPredefinido = creadoPor == null, // Es predefinido solo si no tiene creador
        fechaCreacion = java.util.Date()
    )
}
