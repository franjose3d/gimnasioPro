package com.example.gimnasiopro.presentation.progreso

import android.app.Application
import com.example.gimnasiopro.data.EjercicioConRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.firestore.EstadisticaFirestore
import com.example.gimnasiopro.data.firestore.EstadisticaFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class ProgresoUiState(
    val isLoading: Boolean = true,
    val titulo: String = "Mi Progreso",
    val tiempoHoy: String = "0:00",
    val tiempoMes: String = "0:00",
    val entrenamientosMes: Int = 0,
    val racha: Int = 0,
    val pesoHoy: String = "0 kg",
    val recordPeso: String = "0 kg",
    val equilibrio: Map<String, Int> = emptyMap(),
    val error: String? = null,
    val historialEjercicios: List<EjercicioConRecord> = emptyList(),
    val showMenuOpciones: Boolean = false,
    val showDialogEliminar: Boolean = false,
    val seccionConfirmando: String? = null,
    val isDeleting: Boolean = false
)

class ProgresoViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val modoTrainer  = savedState.get<Boolean>("MODO_TRAINER") ?: false
    private val clienteId    = savedState.get<String>("CLIENTE_ID")
    private val clienteNombre= savedState.get<String>("CLIENTE_NOMBRE")

    private val app = application as GimnasioproApplication
    private fun getPrefs() = app.getSharedPreferences("gimnasiopro_prefs",
        android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(ProgresoUiState(
        titulo = if (modoTrainer && clienteNombre != null) "Progreso de $clienteNombre" else "Mi Progreso"
    ))
    val state: StateFlow<ProgresoUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        if (modoTrainer && clienteId != null) {
            loadFromFirestore(clienteId)
        } else {
            loadLocal()
        }
    }

    private fun loadLocal() {
        viewModelScope.launch {
            try {
                val estadRepo   = app.estadisticaRepositoryHibrido
                val registroRepo= app.registroEntrenamientoRepository
                val ejercRepo   = app.ejercicioRepository

                val tiempoHoy   = estadRepo.getTiempoHoy()
                val tiempoMes   = estadRepo.getTiempoMesActual()
                val entreMes    = estadRepo.getEntrenamientosMesActual()
                val racha       = estadRepo.getRachaActual()
                val pesoHoy     = estadRepo.getVolumenHoy()
                val record      = estadRepo.getRecordVolumen()

                // Equilibrio muscular — últimos 30 días naturales
                val fin = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0);       cal.set(Calendar.MILLISECOND, 0)
                val inicio = cal.timeInMillis
                val efectivoInicio = maxOf(inicio, getPrefs().getLong("equilibrio_reset_date", 0L))

                val ids = registroRepo.getEjerciciosCompletadosEntreFechas(efectivoInicio, fin)
                val conteos = ejercRepo.getConteoPorGrupoMuscular(ids)
                val total   = conteos.values.sum()
                val equilibrio = if (total > 0) conteos.mapValues { (_, v) -> ((v.toFloat() / total) * 100).toInt() }
                                 else emptyMap()

                val historial = registroRepo.getEjerciciosConRecord()

                _state.update { it.copy(
                    isLoading = false,
                    tiempoHoy = EstadisticaRepository.formatearTiempo(tiempoHoy),
                    tiempoMes = EstadisticaRepository.formatearTiempo(tiempoMes),
                    entrenamientosMes = entreMes,
                    racha = racha,
                    pesoHoy = formatearPeso(pesoHoy),
                    recordPeso = formatearPeso(record),
                    equilibrio = equilibrio,
                    historialEjercicios = historial
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadFromFirestore(cId: String) {
        viewModelScope.launch {
            try {
                val repo  = EstadisticaFirestoreRepository(cId, "cliente")
                val cal   = Calendar.getInstance()
                val anio  = cal.get(Calendar.YEAR)
                val mes   = cal.get(Calendar.MONTH) + 1
                val list  = repo.getEstadisticasPorMes(anio, mes).first()

                val hoy     = EstadisticaFirestore.formatFecha(Date())
                val hoyData = list.find { it.fecha == hoy }

                val tiempoHoy   = hoyData?.tiempoEntrenamientoMs ?: 0L
                val tiempoMes   = list.sumOf { it.tiempoEntrenamientoMs }
                val entreMes    = list.sumOf { it.numeroEntrenamientos }
                val racha       = calcularRacha(list)
                val pesoHoy     = hoyData?.volumenTotal ?: 0f
                val record      = list.maxOfOrNull { it.volumenTotal } ?: 0f

                _state.update { it.copy(
                    isLoading = false,
                    tiempoHoy = EstadisticaRepository.formatearTiempo(tiempoHoy),
                    tiempoMes = EstadisticaRepository.formatearTiempo(tiempoMes),
                    entrenamientosMes = entreMes,
                    racha = racha,
                    pesoHoy = formatearPeso(pesoHoy),
                    recordPeso = formatearPeso(record),
                    equilibrio = emptyMap()
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun calcularRacha(list: List<EstadisticaFirestore>): Int {
        val dias = list.filter { it.numeroEntrenamientos > 0 }.sortedByDescending { it.fechaTimestamp }
        if (dias.isEmpty()) return 0
        val cal = Calendar.getInstance()
        var racha = 0
        var esperado = Calendar.getInstance()
        for (e in dias) {
            cal.time = e.fechaTimestamp
            val mismo = cal.get(Calendar.YEAR) == esperado.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == esperado.get(Calendar.DAY_OF_YEAR)
            if (mismo || racha == 0) {
                racha++
                esperado.time = e.fechaTimestamp
                esperado.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return racha
    }

    private fun formatearPeso(v: Float) = when {
        v >= 1000 -> String.format(Locale.getDefault(), "%.1f t", v / 1000)
        v > 0     -> String.format(Locale.getDefault(), "%.0f kg", v)
        else      -> "0 kg"
    }
    // ── Gestión del menú y dialogs ───────────────────────────────────────────

    fun toggleMenu() = _state.update { it.copy(showMenuOpciones =
        !it.showMenuOpciones) }

    fun mostrarDialogEliminar() =
        _state.update { it.copy(showMenuOpciones = false, showDialogEliminar =
            true) }

    fun ocultarDialogEliminar() =
        _state.update { it.copy(showDialogEliminar = false, seccionConfirmando =
            null) }

    fun confirmarEliminar(seccion: String) =
        _state.update { it.copy(seccionConfirmando = seccion) }

    fun cancelarConfirmacion() =
        _state.update { it.copy(seccionConfirmando = null) }

    fun ejecutarEliminar(seccion: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, seccionConfirmando = null)
            }
            try {
                val repo = app.estadisticaRepositoryHibrido
                when (seccion) {
                    "tiempo_hoy"         -> repo.resetTiempoHoy()
                    "tiempo_mes"         -> repo.resetTiempoMes()
                    "entrenamientos_mes" -> repo.resetEntrenamientosMes()
                    "racha"              -> repo.resetRacha()
                    "peso_movido"        -> repo.resetPesoMovido()
                    "equilibrio"         -> getPrefs().edit().putLong("equilibrio_reset_date", System.currentTimeMillis()).apply()
                    "todo"               -> repo.deleteAll()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isDeleting = false, showDialogEliminar =
                    false) }
                load()
            }
        }
    }
}
