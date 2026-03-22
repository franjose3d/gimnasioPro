package com.example.gimnasiopro.presentation.buscarejercictos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.Ejercicio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class BuscarEjerciciosUiState(
    val query: String = "",
    val resultados: List<Ejercicio> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoaded: Boolean = false,
    val toast: String? = null,
    val navigateBack: Boolean = false
)

class BuscarEjerciciosViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val numeroRutinaDestino = savedState.get<Int>("extra_numero_rutina") ?: -1

    private val app           = application as GimnasioproApplication
    private val ejercicioRepo = app.ejercicioRepository
    private val rutinaRepo    = app.rutinaRepositoryHibrido
    private val auth          = FirebaseAuth.getInstance()

    val numRutina get() = numeroRutinaDestino

    private var todosLosEjercicios: List<Ejercicio> = emptyList()

    private val _state = MutableStateFlow(BuscarEjerciciosUiState())
    val state: StateFlow<BuscarEjerciciosUiState> = _state.asStateFlow()

    companion object {
        private const val MAX_EJERCICIOS = 10
    }

    init {
        viewModelScope.launch {
            try {
                todosLosEjercicios = ejercicioRepo.allEjercicios.first()
                _state.update { it.copy(isLoaded = true) }
            } catch (_: Exception) {}
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            _state.update { it.copy(resultados = emptyList()) }
            return
        }
        buscar(query)
    }

    private fun buscar(query: String) {
        val q = normalizar(query)
        val resultados = todosLosEjercicios.filter { ej ->
            normalizar(ej.nombre).contains(q) ||
                normalizar(ej.descripcion).contains(q) ||
                normalizar(ej.grupoMuscular).contains(q)
        }.sortedWith(compareBy(
            { !normalizar(it.nombre).contains(q) },
            { it.nombre }
        ))
        _state.update { it.copy(resultados = resultados) }
    }

    private fun normalizar(s: String) = s.lowercase(Locale.getDefault())
        .replace("á","a").replace("é","e").replace("í","i")
        .replace("ó","o").replace("ú","u").replace("ü","u").replace("ñ","n")

    // ── Selection ────────────────────────────────────────────────────────────

    fun toggleSelection(id: Long) {
        _state.update { s ->
            val sel = s.selectedIds.toMutableSet()
            if (id in sel) {
                sel.remove(id)
            } else if (sel.size < MAX_EJERCICIOS) {
                sel.add(id)
            } else {
                toast("Máximo $MAX_EJERCICIOS ejercicios")
                return@update s
            }
            s.copy(selectedIds = sel)
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedIds = emptySet()) }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun guardarSeleccionados() {
        val ids = _state.value.selectedIds.map { it.toInt() }
        if (ids.isEmpty()) { toast("Selecciona al menos un ejercicio"); return }
        if (auth.currentUser == null) { toast("Debes iniciar sesión para guardar"); return }
        viewModelScope.launch {
            try {
                val resultado = rutinaRepo.agregarEjerciciosARutina(numeroRutinaDestino, ids)
                if (resultado.exito) {
                    toast(resultado.mensaje)
                    _state.update { it.copy(navigateBack = true) }
                } else {
                    toast(resultado.mensaje)
                }
            } catch (e: Exception) {
                toast("Error al guardar: ${e.message}")
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    fun clearNavigateBack() = _state.update { it.copy(navigateBack = false) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
