package com.example.gimnasiopro.presentation.entrenamiento

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RegistroEntrenamiento
import com.example.gimnasiopro.data.SerieEntrenamiento
import com.example.gimnasiopro.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SerieState(
    val repeticiones: Int = 10,
    val pesoKg: Float = 0f
)

data class EjercicioEntrenamientoState(
    val ejercicio: Ejercicio,
    val series: List<SerieState> = List(3) { SerieState() },
    val seriesVisibles: Int = 3,
    val completado: Boolean = false
)

data class EntrenamientoUiState(
    val isLoading: Boolean = true,
    val isFinalizing: Boolean = false,
    val ejercicios: List<EjercicioEntrenamientoState> = emptyList(),
    val toast: String? = null,
    val navigateBack: Boolean = false,
    val showConfirmSalir: Boolean = false,
    val ejerciciosParaConfirmar: List<Ejercicio> = emptyList()
)

class EntrenamientoViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val numeroRutina  = savedState.get<Int>("extra_numero_rutina") ?: 1

    private val app               = application as GimnasioproApplication
    private val db                = GymDatabase.getDatabase(application)
    private val ejercicioRepo     = app.ejercicioRepository
    private val rutinaRepo        = app.rutinaRepository
    private val registroRepo      = app.registroEntrenamientoRepository
    private val estadisticaRepo   = app.estadisticaRepositoryHibrido

    private val rutinaRepoHibrido = app.rutinaRepositoryHibrido

    private val tiempoInicio = System.currentTimeMillis()

    private val _state = MutableStateFlow(EntrenamientoUiState())
    val state: StateFlow<EntrenamientoUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "EntrenamientoVM"
        private const val MAX_SERIES = 6
        private const val MIN_SERIES = 1
        private const val KG_STEP = 5f
        private const val REP_STEP = 1
    }

    init {
        SyncManager.startTrainingMode()
        load()
    }

    override fun onCleared() {
        super.onCleared()
        SyncManager.reset()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val rutina = rutinaRepo.getRutinaByNumeroSync(numeroRutina)
                val ids = rutina?.getEjercicioIdsList() ?: emptyList()
                if (ids.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }
                val ejercicios = ejercicioRepo.getEjerciciosByIds(ids)
                val ordered = ids.mapNotNull { id -> ejercicios.find { it.id == id } }

                val items = ordered.map { ej ->
                    val ultimasSeries = registroRepo.getUltimasSeriesPorEjercicio(
                        rutinaId = numeroRutina,
                        ejercicioId = ej.id,
                        numeroSeries = 6
                    )
                    val series = if (ultimasSeries.isNotEmpty()) {
                        ultimasSeries.map { SerieState(it.repeticiones, it.pesoKg) }
                    } else {
                        List(3) { SerieState() }
                    }
                    EjercicioEntrenamientoState(
                        ejercicio = ej,
                        series = series + List(maxOf(0, 6 - series.size)) { SerieState() },
                        seriesVisibles = series.size.coerceIn(MIN_SERIES, MAX_SERIES)
                    )
                }
                _state.update { it.copy(isLoading = false, ejercicios = items) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading: ${e.message}")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Series management ────────────────────────────────────────────────────

    fun addSerie(index: Int) {
        _state.update { s ->
            val items = s.ejercicios.toMutableList()
            val item = items.getOrNull(index) ?: return@update s
            if (item.seriesVisibles >= MAX_SERIES) return@update s
            items[index] = item.copy(seriesVisibles = item.seriesVisibles + 1)
            s.copy(ejercicios = items)
        }
    }

    fun removeSerie(index: Int) {
        _state.update { s ->
            val items = s.ejercicios.toMutableList()
            val item = items.getOrNull(index) ?: return@update s
            if (item.seriesVisibles <= MIN_SERIES) return@update s
            items[index] = item.copy(seriesVisibles = item.seriesVisibles - 1)
            s.copy(ejercicios = items)
        }
    }

    fun updatePeso(ejercicioIndex: Int, serieIndex: Int, peso: Float) {
        _state.update { s ->
            val items = s.ejercicios.toMutableList()
            val item = items.getOrNull(ejercicioIndex) ?: return@update s
            val series = item.series.toMutableList()
            if (serieIndex >= series.size) return@update s
            series[serieIndex] = series[serieIndex].copy(pesoKg = maxOf(0f, peso))
            items[ejercicioIndex] = item.copy(series = series)
            s.copy(ejercicios = items)
        }
    }

    fun incrementPeso(ejercicioIndex: Int, serieIndex: Int) {
        val current = _state.value.ejercicios.getOrNull(ejercicioIndex)?.series?.getOrNull(serieIndex)?.pesoKg ?: return
        updatePeso(ejercicioIndex, serieIndex, current + KG_STEP)
    }

    fun decrementPeso(ejercicioIndex: Int, serieIndex: Int) {
        val current = _state.value.ejercicios.getOrNull(ejercicioIndex)?.series?.getOrNull(serieIndex)?.pesoKg ?: return
        updatePeso(ejercicioIndex, serieIndex, maxOf(0f, current - KG_STEP))
    }

    fun updateReps(ejercicioIndex: Int, serieIndex: Int, reps: Int) {
        _state.update { s ->
            val items = s.ejercicios.toMutableList()
            val item = items.getOrNull(ejercicioIndex) ?: return@update s
            val series = item.series.toMutableList()
            if (serieIndex >= series.size) return@update s
            series[serieIndex] = series[serieIndex].copy(repeticiones = maxOf(1, reps))
            items[ejercicioIndex] = item.copy(series = series)
            s.copy(ejercicios = items)
        }
    }

    fun incrementReps(ejercicioIndex: Int, serieIndex: Int) {
        val current = _state.value.ejercicios.getOrNull(ejercicioIndex)?.series?.getOrNull(serieIndex)?.repeticiones ?: return
        updateReps(ejercicioIndex, serieIndex, current + REP_STEP)
    }

    fun decrementReps(ejercicioIndex: Int, serieIndex: Int) {
        val current = _state.value.ejercicios.getOrNull(ejercicioIndex)?.series?.getOrNull(serieIndex)?.repeticiones ?: return
        updateReps(ejercicioIndex, serieIndex, maxOf(1, current - REP_STEP))
    }

    fun toggleCompletado(index: Int) {
        _state.update { s ->
            val items = s.ejercicios.toMutableList()
            val item = items.getOrNull(index) ?: return@update s
            items[index] = item.copy(completado = !item.completado)
            s.copy(ejercicios = items)
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun confirmarSalir() = _state.update { it.copy(showConfirmSalir = true) }
    fun dismissConfirmSalir() = _state.update { it.copy(showConfirmSalir = false) }

    fun salirSinGuardar() {
        SyncManager.reset()
        _state.update { it.copy(showConfirmSalir = false, navigateBack = true) }
    }

    fun clearNavigateBack() = _state.update { it.copy(navigateBack = false) }

    // ── Finalize ─────────────────────────────────────────────────────────────

    fun finalizar() {
        _state.update { it.copy(isFinalizing = true) }
        viewModelScope.launch {
            try {
                val tiempoFin = System.currentTimeMillis()
                val tiempoMs = tiempoFin - tiempoInicio
                val fecha = tiempoFin

                val ejs = _state.value.ejercicios
                val registros = mutableListOf<RegistroEntrenamiento>()
                val seriesPorRegistro = mutableMapOf<Int, List<SerieEntrenamiento>>()

                ejs.forEachIndexed { idx, ej ->
                    val seriesVisibles = ej.series.take(ej.seriesVisibles)
                    val pesoMax = seriesVisibles.maxOfOrNull { it.pesoKg } ?: 0f
                    val totalReps = seriesVisibles.sumOf { it.repeticiones }
                    val registro = RegistroEntrenamiento(
                        rutinaId = numeroRutina,
                        ejercicioId = ej.ejercicio.id,
                        repeticiones = totalReps,
                        pesoKg = pesoMax,
                        completado = ej.completado,
                        fechaEntrenamiento = fecha,
                        numeroSeries = ej.seriesVisibles
                    )
                    registros.add(registro)
                    seriesPorRegistro[idx] = seriesVisibles.map { SerieEntrenamiento(it.repeticiones, it.pesoKg) }
                }

                registroRepo.guardarRegistrosConSeries(registros, seriesPorRegistro)

                val ejerciciosCompletados = ejs.count { it.completado }
                val volumenTotal = ejs.filter { it.completado }
                    .sumOf { ej ->
                        ej.series.take(ej.seriesVisibles).sumOf { (it.pesoKg * it.repeticiones).toDouble() }
                    }.toFloat()

                SyncManager.endTrainingMode()
                estadisticaRepo.registrarEntrenamiento(
                    tiempoMs = tiempoMs,
                    ejerciciosCompletados = ejerciciosCompletados,
                    volumenTotal = volumenTotal,
                    rutinaId = numeroRutina
                )
                SyncManager.syncCompleted()

                toast("Entrenamiento guardado")
                _state.update { it.copy(isFinalizing = false, navigateBack = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizando: ${e.message}")
                _state.update { it.copy(isFinalizing = false) }
                toast("Error al guardar: ${e.message}")
            }
        }
    }

    // ── Agregar ejercicio durante entrenamiento ───────────────────────────────

    fun onEjerciciosRecibidos(ids: List<Long>) {
        viewModelScope.launch {
            val ejercicios = ejercicioRepo.getEjerciciosByIds(ids)
            _state.update { it.copy(ejerciciosParaConfirmar = ejercicios) }
        }
    }

    fun agregarSoloHoy() {
        val nuevos = _state.value.ejerciciosParaConfirmar
        _state.update { s ->
            val nuevosItems = nuevos.map { ej ->
                EjercicioEntrenamientoState(
                    ejercicio = ej,
                    series = List(6) { SerieState() },
                    seriesVisibles = 3
                )
            }
            s.copy(
                ejercicios = s.ejercicios + nuevosItems,
                ejerciciosParaConfirmar = emptyList()
            )
        }
    }

    fun agregarYGuardarEnRutina() {
        val nuevos = _state.value.ejerciciosParaConfirmar
        agregarSoloHoy()
        viewModelScope.launch {
            try {
                rutinaRepoHibrido.agregarEjerciciosARutina(
                    numeroRutina,
                    nuevos.map { it.id.toInt() }
                )
                toast("Ejercicios guardados en la rutina")
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando en rutina: ${e.message}")
                toast("Añadidos al entrenamiento (error al guardar en rutina)")
            }
        }
    }

    fun descartarEjerciciosPickeados() {
        _state.update { it.copy(ejerciciosParaConfirmar = emptyList()) }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
