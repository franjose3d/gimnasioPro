package com.example.gimnasiopro.presentation.gim

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.Rutina
import com.example.gimnasiopro.data.RutinaDiaSemana
import com.example.gimnasiopro.data.RutinaDiaSemanaRepository
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.firestore.GimnasioFirestore
import com.example.gimnasiopro.data.firestore.GimnasioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GimUiState(
    val diasConRutina: Set<Int> = emptySet(),
    val diaSeleccionado: Int = RutinaDiaSemana.LUNES,
    val toast: String? = null,
    val navigateToDetalle: Int? = null,          // numeroRutina to open
    val navigateToRutinas: Boolean = false,
    val showRutinaPickerDia: Int? = null,        // diaSemana for picker dialog
    val rutinasDisponibles: List<Rutina> = emptyList(),
    val showOpcionesDia: Int? = null,            // diaSemana for options (long press)
    // ── Gimnasio ─────────────────────────────────────────────────────────────
    val gimnasio: GimnasioFirestore? = null,
    val isVinculando: Boolean = false,
    val showVincularDialog: Boolean = false,
    val codigoInput: String = "",
    val errorCodigo: String? = null,
    val showQrScanner: Boolean = false
)

class GimViewModel(application: Application) : AndroidViewModel(application) {

    private val app                    = application as GimnasioproApplication
    private val db                     = GymDatabase.getDatabase(application)
    private val rutinaDiaSemanaRepo    = RutinaDiaSemanaRepository(db.rutinaDiaSemanaDao())
    private val rutinaRepo             = RutinaRepository(db.rutinaDao())
    private val gimnasioRepo           = app.gimnasioRepository

    private val _state = MutableStateFlow(GimUiState(diaSeleccionado = getDiaActual()))
    val state: StateFlow<GimUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "GimViewModel"
    }

    init {
        syncRutinas()
        cargarDiasConRutina()
        cargarGimnasio()
    }

    private fun syncRutinas() {
        viewModelScope.launch {
            try {
                app.rutinaRepositoryHibrido.sincronizarRutinasConFirebase()
                    .onSuccess { n -> if (n > 0) Log.d(TAG, "✅ $n rutinas sincronizadas") }
                    .onFailure { e -> Log.w(TAG, "⚠️ Sync: ${e.message}") }
            } catch (e: Exception) {
                Log.e(TAG, "Error sync: ${e.message}")
            }
        }
    }

    fun cargarDiasConRutina() {
        viewModelScope.launch {
            val dias = mutableSetOf<Int>()
            for (dia in 1..7) {
                if (rutinaDiaSemanaRepo.getNumeroRutinaPorDia(dia) != null) dias.add(dia)
            }
            _state.update { it.copy(diasConRutina = dias) }
        }
    }

    fun onDiaSeleccionado(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        val diaSemana = calendarDayToRutinaDia(cal.get(Calendar.DAY_OF_WEEK))
        _state.update { it.copy(diaSeleccionado = diaSemana) }
        toast("Fecha: $day/${month + 1}/$year")
    }

    fun onDiaLongPress(diaSemana: Int) {
        _state.update { it.copy(showOpcionesDia = diaSemana) }
    }

    fun dismissOpcionesDia() = _state.update { it.copy(showOpcionesDia = null) }

    // ── Mi Rutina button ─────────────────────────────────────────────────────

    fun verificarRutinaDelDia() {
        val dia = _state.value.diaSeleccionado
        val nombreDia = RutinaDiaSemana.getNombreDia(dia)
        viewModelScope.launch {
            val numero = rutinaDiaSemanaRepo.getNumeroRutinaPorDia(dia)
            if (numero != null) {
                _state.update { it.copy(navigateToDetalle = numero) }
            } else {
                rutinaRepo.initializeRutinasIfNeeded()
                val rutinas = rutinaRepo.getAllRutinas().first()
                if (rutinas.isEmpty()) {
                    toast("No tienes rutinas. Crea una primero.")
                    _state.update { it.copy(navigateToRutinas = true) }
                } else {
                    _state.update { it.copy(
                        showRutinaPickerDia = dia,
                        rutinasDisponibles = rutinas
                    )}
                }
            }
        }
    }

    fun onRutinaSeleccionadaParaDia(diaSemana: Int, numeroRutina: Int) {
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        viewModelScope.launch {
            rutinaRepo.initializeRutinasIfNeeded()
            rutinaDiaSemanaRepo.asignarRutinaADia(diaSemana, numeroRutina)
            toast("Rutina asignada a los $nombreDia")
            cargarDiasConRutina()
            _state.update { it.copy(showRutinaPickerDia = null, navigateToDetalle = numeroRutina) }
        }
    }

    fun cambiarRutinaDia(diaSemana: Int) {
        viewModelScope.launch {
            rutinaRepo.initializeRutinasIfNeeded()
            val rutinas = rutinaRepo.getAllRutinas().first()
            _state.update { it.copy(showOpcionesDia = null, showRutinaPickerDia = diaSemana, rutinasDisponibles = rutinas) }
        }
    }

    fun eliminarRutinaDia(diaSemana: Int) {
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        viewModelScope.launch {
            rutinaDiaSemanaRepo.eliminarRutinaDia(diaSemana)
            toast("Rutina eliminada de los $nombreDia")
            cargarDiasConRutina()
            _state.update { it.copy(showOpcionesDia = null) }
        }
    }

    fun dismissRutinaPicker() = _state.update { it.copy(showRutinaPickerDia = null) }

    fun clearNavigateToDetalle() = _state.update { it.copy(navigateToDetalle = null) }
    fun clearNavigateToRutinas() = _state.update { it.copy(navigateToRutinas = false) }

    fun onNuevaRutina() = _state.update { it.copy(navigateToRutinas = true) }

    fun clearToast() = _state.update { it.copy(toast = null) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }

    // ── Gimnasio ─────────────────────────────────────────────────────────────

    fun cargarGimnasio() {
        viewModelScope.launch {
            try {
                val gym = gimnasioRepo.getGimnasioActual()
                _state.update { it.copy(gimnasio = gym) }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando gimnasio: ${e.message}")
            }
        }
    }

    fun onMostrarVincularDialog() = _state.update {
        it.copy(showVincularDialog = true, codigoInput = "", errorCodigo = null)
    }

    fun onDismissVincularDialog() = _state.update {
        it.copy(showVincularDialog = false, codigoInput = "", errorCodigo = null)
    }

    fun onCodigoChange(codigo: String) = _state.update {
        it.copy(codigoInput = codigo, errorCodigo = null)
    }

    fun vincularPorCodigo() {
        val codigo = _state.value.codigoInput.trim()
        if (codigo.isBlank()) {
            _state.update { it.copy(errorCodigo = "Introduce el código del gimnasio") }
            return
        }
        _state.update { it.copy(isVinculando = true, errorCodigo = null) }
        viewModelScope.launch {
            val gym = gimnasioRepo.buscarPorCodigo(codigo)
            if (gym == null) {
                _state.update { it.copy(isVinculando = false, errorCodigo = "Código no encontrado o gimnasio inactivo") }
                return@launch
            }
            val ok = gimnasioRepo.vincularGimnasio(gym.id)
            if (ok) {
                _state.update { it.copy(
                    isVinculando = false,
                    showVincularDialog = false,
                    codigoInput = "",
                    gimnasio = gym
                )}
                toast("Vinculado a ${gym.nombre}")
            } else {
                _state.update { it.copy(isVinculando = false, errorCodigo = "No se pudo guardar. Inténtalo de nuevo.") }
            }
        }
    }

    fun onCodigoQrDetectado(codigo: String) {
        _state.update { it.copy(showQrScanner = false, codigoInput = codigo) }
        vincularPorCodigo()
    }

    fun desvincularGimnasio() {
        viewModelScope.launch {
            val ok = gimnasioRepo.desvincularGimnasio()
            if (ok) {
                _state.update { it.copy(gimnasio = null) }
                toast("Gimnasio desvinculado")
            } else {
                toast("Error al desvincular. Inténtalo de nuevo.")
            }
        }
    }

    fun onMostrarQrScanner() = _state.update { it.copy(showQrScanner = true) }
    fun onDismissQrScanner() = _state.update { it.copy(showQrScanner = false) }

    private fun getDiaActual(): Int {
        val cal = Calendar.getInstance()
        return calendarDayToRutinaDia(cal.get(Calendar.DAY_OF_WEEK))
    }

    private fun calendarDayToRutinaDia(calDay: Int) = when (calDay) {
        Calendar.MONDAY    -> RutinaDiaSemana.LUNES
        Calendar.TUESDAY   -> RutinaDiaSemana.MARTES
        Calendar.WEDNESDAY -> RutinaDiaSemana.MIERCOLES
        Calendar.THURSDAY  -> RutinaDiaSemana.JUEVES
        Calendar.FRIDAY    -> RutinaDiaSemana.VIERNES
        Calendar.SATURDAY  -> RutinaDiaSemana.SABADO
        Calendar.SUNDAY    -> RutinaDiaSemana.DOMINGO
        else               -> RutinaDiaSemana.LUNES
    }
}
