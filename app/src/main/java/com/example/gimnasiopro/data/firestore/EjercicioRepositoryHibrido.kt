package com.example.gimnasiopro.data.firestore

import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.EjercicioRepository
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
     */
    private fun obtenerTodosEjercicios(): Flow<List<Ejercicio>> = flow {
        // 1. Emitir datos locales primero (inmediato)
        val ejerciciosLocales = localRepository.allEjercicios.first()

        // Si Room está vacío, no emitir lista vacía - esperar a Firebase
        if (ejerciciosLocales.isNotEmpty()) {
            emit(ejerciciosLocales)
        }

        // 2. Sincronizar con Firestore (crítico si Room está vacío)
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
     */
    suspend fun insertEjercicio(ejercicio: Ejercicio): Long {
        // Insertar en local primero
        val localId = localRepository.insertEjercicio(ejercicio)

        // Intentar insertar en Firestore
        try {
            val ejercicioFirestore = ejercicio.toFirestoreModel()
            remoteRepository.crearEjercicio(ejercicioFirestore)
        } catch (e: Exception) {
            // Log pero no fallar si Firestore falla
        }

        return localId
    }

    /**
     * Insertar múltiples ejercicios.
     */
    suspend fun insertAllEjercicios(ejercicios: List<Ejercicio>) {
        localRepository.insertAllEjercicios(ejercicios)

        // Sincronizar con Firestore en background
        try {
            ejercicios.forEach { ejercicio ->
                val ejercicioFirestore = ejercicio.toFirestoreModel()
                remoteRepository.crearEjercicio(ejercicioFirestore)
            }
        } catch (e: Exception) {
            // Log pero no fallar
        }
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
            remoteRepository.eliminarEjercicio(ejercicio.id.toString())
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
 */
fun Ejercicio.toFirestoreModel(): EjercicioFirestore {
    return EjercicioFirestore(
        id = this.id.toString(),
        grupoMuscular = this.grupoMuscular,
        nombre = this.nombre,
        descripcion = this.descripcion,
        imagenUrl = this.imagenUrl,
        esPredefinido = true,
        fechaCreacion = java.util.Date()
    )
}
