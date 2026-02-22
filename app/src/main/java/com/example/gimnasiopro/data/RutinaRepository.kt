package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar las rutinas de ejercicios.
 * Proporciona una capa de abstracción sobre el DAO.
 */
class RutinaRepository(private val rutinaDao: RutinaDao) {

    /**
     * Obtiene todas las rutinas como Flow.
     */
    fun getAllRutinas(): Flow<List<Rutina>> = rutinaDao.getAllRutinas()

    /**
     * Obtiene una rutina específica por su número.
     */
    fun getRutinaByNumero(numeroRutina: Int): Flow<Rutina?> = rutinaDao.getRutinaByNumero(numeroRutina)

    /**
     * Alias para compatibilidad - obtiene una rutina por su número como Flow.
     */
    fun getRutinaByIdFlow(numeroRutina: Int): Flow<Rutina?> = rutinaDao.getRutinaByNumero(numeroRutina)

    /**
     * Obtiene una rutina de forma síncrona (suspending).
     */
    suspend fun getRutinaByNumeroSync(numeroRutina: Int): Rutina? = rutinaDao.getRutinaByNumeroSync(numeroRutina)

    /**
     * Inserta o actualiza una rutina.
     */
    suspend fun insertRutina(rutina: Rutina) = rutinaDao.insertRutina(rutina)

    /**
     * Inserta múltiples rutinas.
     */
    suspend fun insertRutinas(rutinas: List<Rutina>) = rutinaDao.insertRutinas(rutinas)

    /**
     * Actualiza los ejercicios de una rutina (reemplaza todos).
     * @param numeroRutina Número de la rutina (1-10)
     * @param ejercicioIds Lista de IDs de ejercicios a guardar
     */
    suspend fun actualizarEjerciciosDeRutina(numeroRutina: Int, ejercicioIds: List<Int>) {
        val ejercicioIdsString = ejercicioIds.joinToString(",")
        rutinaDao.updateEjerciciosDeRutina(
            numeroRutina = numeroRutina,
            ejercicioIds = ejercicioIdsString,
            fechaModificacion = System.currentTimeMillis()
        )
    }

    /**
     * Resultado de añadir ejercicios a una rutina.
     */
    data class ResultadoAgregarEjercicios(
        val exito: Boolean,
        val ejerciciosAgregados: Int,
        val ejerciciosYaExistentes: Int,
        val ejerciciosNoAgregados: Int,
        val totalActual: Int,
        val mensaje: String
    )

    companion object {
        const val MAX_EJERCICIOS_POR_RUTINA = 10
    }

    /**
     * Añade ejercicios a una rutina existente sin borrar los anteriores.
     * @param numeroRutina Número de la rutina (1-10)
     * @param nuevosEjercicioIds Lista de IDs de ejercicios a añadir
     * @return Resultado indicando cuántos ejercicios se añadieron
     */
    suspend fun agregarEjerciciosARutina(numeroRutina: Int, nuevosEjercicioIds: List<Int>): ResultadoAgregarEjercicios {
        // Obtener la rutina actual
        val rutinaActual = rutinaDao.getRutinaByNumeroSync(numeroRutina)
        val ejerciciosExistentes = rutinaActual?.ejercicioIds ?: emptyList()

        // Contar ejercicios que ya existen en la rutina
        val ejerciciosYaExistentes = nuevosEjercicioIds.count { it in ejerciciosExistentes }

        // Filtrar ejercicios que no estén ya en la rutina
        val ejerciciosNuevosReales = nuevosEjercicioIds.filter { it !in ejerciciosExistentes }

        // Calcular cuántos espacios quedan
        val espaciosDisponibles = MAX_EJERCICIOS_POR_RUTINA - ejerciciosExistentes.size

        if (espaciosDisponibles <= 0) {
            return ResultadoAgregarEjercicios(
                exito = false,
                ejerciciosAgregados = 0,
                ejerciciosYaExistentes = ejerciciosYaExistentes,
                ejerciciosNoAgregados = ejerciciosNuevosReales.size,
                totalActual = ejerciciosExistentes.size,
                mensaje = "La rutina ya tiene $MAX_EJERCICIOS_POR_RUTINA ejercicios. No se pueden añadir más."
            )
        }

        // Tomar solo los que caben
        val ejerciciosAAgregar = ejerciciosNuevosReales.take(espaciosDisponibles)
        val ejerciciosNoAgregados = ejerciciosNuevosReales.size - ejerciciosAAgregar.size

        // Combinar ejercicios existentes con los nuevos
        val listaFinal = ejerciciosExistentes + ejerciciosAAgregar

        // Guardar la lista combinada
        val ejercicioIdsString = listaFinal.joinToString(",")
        rutinaDao.updateEjerciciosDeRutina(
            numeroRutina = numeroRutina,
            ejercicioIds = ejercicioIdsString,
            fechaModificacion = System.currentTimeMillis()
        )

        // Construir mensaje
        val mensaje = buildString {
            if (ejerciciosAAgregar.isNotEmpty()) {
                append("${ejerciciosAAgregar.size} ejercicio(s) añadido(s)")
            }
            if (ejerciciosYaExistentes > 0) {
                if (isNotEmpty()) append(". ")
                append("$ejerciciosYaExistentes ya estaba(n) en la rutina")
            }
            if (ejerciciosNoAgregados > 0) {
                if (isNotEmpty()) append(". ")
                append("$ejerciciosNoAgregados no se pudo(ieron) añadir (límite de $MAX_EJERCICIOS_POR_RUTINA)")
            }
            append(". Total en rutina: ${listaFinal.size}/$MAX_EJERCICIOS_POR_RUTINA")
        }

        return ResultadoAgregarEjercicios(
            exito = ejerciciosAAgregar.isNotEmpty() || ejerciciosYaExistentes > 0,
            ejerciciosAgregados = ejerciciosAAgregar.size,
            ejerciciosYaExistentes = ejerciciosYaExistentes,
            ejerciciosNoAgregados = ejerciciosNoAgregados,
            totalActual = listaFinal.size,
            mensaje = mensaje
        )
    }

    /**
     * Obtiene el número de ejercicios en una rutina.
     */
    suspend fun getNumeroEjerciciosEnRutina(numeroRutina: Int): Int {
        val rutina = rutinaDao.getRutinaByNumeroSync(numeroRutina)
        return rutina?.ejercicioIds?.size ?: 0
    }

    /**
     * Limpia los ejercicios de una rutina.
     */
    suspend fun clearEjerciciosDeRutina(numeroRutina: Int) {
        rutinaDao.clearEjerciciosDeRutina(numeroRutina)
    }

    /**
     * Alias para compatibilidad - limpia los ejercicios de una rutina.
     */
    suspend fun limpiarEjerciciosDeRutina(numeroRutina: Int) {
        rutinaDao.clearEjerciciosDeRutina(numeroRutina)
    }

    /**
     * Elimina ejercicios específicos de una rutina.
     * @param numeroRutina Número de la rutina (1-10)
     * @param ejercicioIdsAEliminar Lista de IDs de ejercicios a eliminar
     * @return Número de ejercicios eliminados
     */
    suspend fun eliminarEjerciciosDeRutina(numeroRutina: Int, ejercicioIdsAEliminar: List<Long>): Int {
        val rutinaActual = rutinaDao.getRutinaByNumeroSync(numeroRutina)
        val ejerciciosExistentes = rutinaActual?.ejercicioIds ?: emptyList()

        // Filtrar los ejercicios que NO están en la lista a eliminar
        val ejerciciosRestantes = ejerciciosExistentes.filter { it !in ejercicioIdsAEliminar.map { id -> id.toInt() } }

        val ejerciciosEliminados = ejerciciosExistentes.size - ejerciciosRestantes.size

        // Guardar la lista actualizada
        val ejercicioIdsString = ejerciciosRestantes.joinToString(",")
        rutinaDao.updateEjerciciosDeRutina(
            numeroRutina = numeroRutina,
            ejercicioIds = if (ejerciciosRestantes.isEmpty()) "" else ejercicioIdsString,
            fechaModificacion = System.currentTimeMillis()
        )

        return ejerciciosEliminados
    }

    /**
     * Obtiene el número de rutinas existentes.
     */
    suspend fun getCountRutinas(): Int = rutinaDao.getCountRutinas()

    /**
     * Inicializa las 10 rutinas vacías si no existen.
     */
    suspend fun initializeRutinasIfNeeded() {
        val count = rutinaDao.getCountRutinas()
        if (count == 0) {
            val rutinas = (1..10).map { numero ->
                Rutina(
                    numeroRutina = numero,
                    nombre = "Rutina $numero",
                    ejercicioIds = emptyList()
                )
            }
            rutinaDao.insertRutinas(rutinas)
        }
    }

    /**
     * Actualiza el nombre de una rutina.
     */
    suspend fun actualizarNombreRutina(numeroRutina: Int, nuevoNombre: String) {
        rutinaDao.updateNombreRutina(
            numeroRutina = numeroRutina,
            nombre = nuevoNombre,
            fechaModificacion = System.currentTimeMillis()
        )
    }

    /**
     * Obtiene el número máximo de rutina existente.
     */
    suspend fun getMaxNumeroRutina(): Int = rutinaDao.getMaxNumeroRutina() ?: 0

    /**
     * Agrega una nueva rutina vacía.
     * @return El número de la nueva rutina, o null si ya se alcanzó el límite máximo
     */
    suspend fun agregarNuevaRutina(maxRutinas: Int = 20): Int? {
        val count = rutinaDao.getCountRutinas()
        if (count >= maxRutinas) {
            return null
        }

        val nuevoNumero = (rutinaDao.getMaxNumeroRutina() ?: 0) + 1
        val nuevaRutina = Rutina(
            numeroRutina = nuevoNumero,
            nombre = "Rutina $nuevoNumero",
            ejercicioIds = emptyList()
        )
        rutinaDao.insertRutina(nuevaRutina)
        return nuevoNumero
    }

    /**
     * Elimina la última rutina (la de mayor número).
     * @return true si se eliminó correctamente, false si no había rutinas o solo queda una
     */
    suspend fun eliminarUltimaRutina(minRutinas: Int = 1): Boolean {
        val count = rutinaDao.getCountRutinas()
        if (count <= minRutinas) {
            return false
        }

        val maxNumero = rutinaDao.getMaxNumeroRutina() ?: return false
        rutinaDao.deleteRutinaByNumero(maxNumero)
        return true
    }
}

