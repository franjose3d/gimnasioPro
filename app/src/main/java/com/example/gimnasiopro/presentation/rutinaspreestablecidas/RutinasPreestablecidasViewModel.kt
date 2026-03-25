package com.example.gimnasiopro.presentation.rutinaspreestablecidas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.Rutina
import com.example.gimnasiopro.data.RutinaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RutinasPreestablecidasUiState(
    val rutinas: List<RutinaPreestablecida> = CatalogoRutinas.todas,
    val filtroNivel: Nivel? = null,
    val filtroObjetivo: Objetivo? = null,
    val isLoading: Boolean = false,
    val toast: String? = null,
    val rutinasCargadas: Set<Int> = emptySet()   // IDs de rutinas ya aplicadas
)

class RutinasPreestablecidasViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "RutinasPreestablecidas"

    private val db         = GymDatabase.getDatabase(application)
    private val rutinaRepo = RutinaRepository(db.rutinaDao())

    private val _state = MutableStateFlow(RutinasPreestablecidasUiState())
    val state: StateFlow<RutinasPreestablecidasUiState> = _state.asStateFlow()

    val rutinasDisponibles: List<RutinaPreestablecida>
        get() = CatalogoRutinas.todas

    // ── Filtros ──────────────────────────────────────────────────────────

    fun setFiltroNivel(nivel: Nivel?) {
        _state.update { it.copy(filtroNivel = nivel) }
        aplicarFiltros()
    }

    fun setFiltroObjetivo(objetivo: Objetivo?) {
        _state.update { it.copy(filtroObjetivo = objetivo) }
        aplicarFiltros()
    }

    fun limpiarFiltros() {
        _state.update { it.copy(filtroNivel = null, filtroObjetivo = null, rutinas = CatalogoRutinas.todas) }
    }

    private fun aplicarFiltros() {
        val nivel    = _state.value.filtroNivel
        val objetivo = _state.value.filtroObjetivo
        val filtradas = CatalogoRutinas.todas.filter { r ->
            (nivel    == null || r.nivel    == nivel) &&
            (objetivo == null || r.objetivo == objetivo)
        }
        _state.update { it.copy(rutinas = filtradas) }
    }

    // ── Cargar en Room ────────────────────────────────────────────────────

    /**
     * Carga la rutina preestablecida en el slot indicado de Room (1..MAX).
     * Sustituye únicamente los ejercicioIds, conservando el nombre si ya existía
     * o usando el nombre de la plantilla.
     */
    fun cargarEnRutina(plantilla: RutinaPreestablecida, numeroSlot: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val existing = rutinaRepo.getRutinaByNumeroSync(numeroSlot)
                val nueva = Rutina(
                    numeroRutina      = numeroSlot,
                    nombre            = plantilla.nombre,
                    ejercicioIds      = plantilla.ejercicioIds,
                    fechaCreacion     = existing?.fechaCreacion ?: System.currentTimeMillis(),
                    fechaModificacion = System.currentTimeMillis()
                )
                rutinaRepo.insertRutina(nueva)

                _state.update { st ->
                    st.copy(
                        isLoading      = false,
                        rutinasCargadas = st.rutinasCargadas + plantilla.id,
                        toast          = "\"${plantilla.nombre}\" cargada en Rutina $numeroSlot ✅"
                    )
                }
                Log.d(TAG, "Plantilla ${plantilla.nombre} cargada en slot $numeroSlot")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando plantilla", e)
                _state.update { it.copy(isLoading = false, toast = "Error al cargar la rutina") }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }

    // ── Slots disponibles ─────────────────────────────────────────────────

    /**
     * Devuelve cuántos slots tiene el usuario (número de rutinas en Room).
     * Se usa en la UI para mostrar el selector de slot.
     */
    fun obtenerNumeroSlots(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val rutinas = rutinaRepo.getAllRutinas().first()
            onResult(rutinas.size.coerceAtLeast(1))
        }
    }
}

