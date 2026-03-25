package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para acceder a los datos de ejercicios.
 * Actúa como intermediario entre la fuente de datos (Room) y el resto de la aplicación.
 */
class EjercicioRepository(private val ejercicioDao: EjercicioDao) {

    /**
     * Obtiene todos los ejercicios como un Flow.
     */
    val allEjercicios: Flow<List<Ejercicio>> = ejercicioDao.getAllEjercicios()

    /**
     * Obtiene todos los grupos musculares únicos.
     */
    val allGruposMusculares: Flow<List<String>> = ejercicioDao.getAllGruposMusculares()

    /**
     * Obtiene ejercicios filtrados por grupo muscular.
     */
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>> {
        return ejercicioDao.getEjerciciosByGrupoMuscular(grupoMuscular)
    }

    /**
     * Busca ejercicios por nombre.
     */
    fun searchEjercicios(query: String): Flow<List<Ejercicio>> {
        return ejercicioDao.searchEjercicios(query)
    }

    /**
     * Obtiene un ejercicio por su ID.
     */
    suspend fun getEjercicioById(id: Long): Ejercicio? {
        return ejercicioDao.getEjercicioById(id)
    }

    /**
     * Inserta un nuevo ejercicio.
     */
    suspend fun insertEjercicio(ejercicio: Ejercicio): Long {
        return ejercicioDao.insertEjercicio(ejercicio)
    }

    /**
     * Inserta múltiples ejercicios.
     */
    suspend fun insertAllEjercicios(ejercicios: List<Ejercicio>) {
        ejercicioDao.insertAllEjercicios(ejercicios)
    }

    /**
     * Actualiza un ejercicio existente.
     */
    suspend fun updateEjercicio(ejercicio: Ejercicio) {
        ejercicioDao.updateEjercicio(ejercicio)
    }

    /**
     * Elimina un ejercicio.
     */
    suspend fun deleteEjercicio(ejercicio: Ejercicio) {
        ejercicioDao.deleteEjercicio(ejercicio)
    }

    /**
     * Elimina todos los ejercicios.
     */
    suspend fun deleteAllEjercicios() {
        ejercicioDao.deleteAllEjercicios()
    }

    /**
     * Obtiene el número total de ejercicios.
     */
    suspend fun getEjerciciosCount(): Int {
        return ejercicioDao.getEjerciciosCount()
    }

    /**
     * Obtiene ejercicios por una lista de IDs.
     */
    suspend fun getEjerciciosByIds(ids: List<Long>): List<Ejercicio> {
        return ejercicioDao.getEjerciciosByIds(ids)
    }

    /**
     * Obtiene ejercicios por una lista de IDs como Flow.
     */
    fun getEjerciciosByIdsFlow(ids: List<Long>): Flow<List<Ejercicio>> {
        return ejercicioDao.getEjerciciosByIdsFlow(ids)
    }

    /**
     * Obtiene el conteo de ejercicios por grupo muscular.
     * Los ejercicios con grupo secundario cuentan para ambos grupos.
     * Se usa para calcular el equilibrio muscular en las estadísticas.
     */
    suspend fun getConteoPorGrupoMuscular(ejercicioIds: List<Long>): Map<String, Int> {
        if (ejercicioIds.isEmpty()) return emptyMap()

        val ejercicios = ejercicioDao.getEjerciciosByIds(ejercicioIds)
        val resultado = mutableMapOf<String, Int>()
        ejercicios.forEach { ej ->
            resultado[ej.grupoMuscular] = (resultado[ej.grupoMuscular] ?: 0) + 1
            ej.grupoMuscularSecundario?.let { secundario ->
                resultado[secundario] = (resultado[secundario] ?: 0) + 1
            }
        }
        return resultado
    }
}
