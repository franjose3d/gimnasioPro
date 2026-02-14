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
     * Obtener todos los ejercicios.
     * Primero carga de Room, luego sincroniza con Firestore.
     */
    fun getAllEjercicios(): Flow<List<Ejercicio>> = flow {
        // 1. Emitir datos locales primero (inmediato)
        val ejerciciosLocales = localRepository.allEjercicios.first()
        emit(ejerciciosLocales)

        // 2. Sincronizar con Firestore en background
        try {
            val ejerciciosRemotos = remoteRepository.getAllEjercicios().first()
            
            // Convertir Firestore a Room y actualizar cache local
            ejerciciosRemotos.forEach { ejercicioFirestore ->
                val ejercicioRoom = ejercicioFirestore.toRoom()
                localRepository.updateEjercicio(ejercicioRoom)
            }
            
            // Emitir datos actualizados
            emit(localRepository.allEjercicios.first())
        } catch (e: Exception) {
            // Si falla la sincronización, mantener datos locales
            // Log error pero no fallar
        }
    }

    /**
     * Obtener ejercicios por grupo muscular.
     */
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>> = flow {
        // 1. Emitir datos locales primero
        val ejerciciosLocales = localRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first()
        emit(ejerciciosLocales)

        // 2. Sincronizar con Firestore
        try {
            val ejerciciosRemotos = remoteRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first()
            ejerciciosRemotos.forEach { ejercicioFirestore ->
                val ejercicioRoom = ejercicioFirestore.toRoom()
                localRepository.updateEjercicio(ejercicioRoom)
            }
            emit(localRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first())
        } catch (e: Exception) {
            // Mantener datos locales si falla
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
     * Obtener todos los grupos musculares.
     */
    fun getAllGruposMusculares(): Flow<List<String>> = flow {
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
 * Se ha renombrado para evitar conflictos si ya existiera otra extensión similar.
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
