package com.example.gimnasiopro.presentation.progreso

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.EjercicioConRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisEjerciciosUiState(
    val isLoading: Boolean = true,
    val ejercicios: List<EjercicioConRecord> = emptyList(),
    val error: String? = null,
    val modoEliminar: Boolean = false,
    val seleccionados: Set<Long> = emptySet(),
    val isDeleting: Boolean = false,
    val showConfirmacion: Boolean = false
)

class MisEjerciciosViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GimnasioproApplication

    private val _state = MutableStateFlow(MisEjerciciosUiState())
    val state: StateFlow<MisEjerciciosUiState> = _state.asStateFlow()

    init { load() }

    fun setModoEliminar(modo: Boolean) {
        _state.update { it.copy(modoEliminar = modo, seleccionados = emptySet()) }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val ejercicios = app.registroEntrenamientoRepository.getEjerciciosConRecord()
                _state.update { it.copy(isLoading = false, ejercicios = ejercicios) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleSeleccion(ejercicioId: Long) {
        _state.update { s ->
            val nuevos = s.seleccionados.toMutableSet()
            if (ejercicioId in nuevos) nuevos.remove(ejercicioId) else nuevos.add(ejercicioId)
            s.copy(seleccionados = nuevos)
        }
    }

    fun confirmarEliminar() = _state.update { it.copy(showConfirmacion = true) }
    fun cancelarEliminar()  = _state.update { it.copy(showConfirmacion = false) }

    fun ejecutarEliminar() {
        val ids = _state.value.seleccionados
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, showConfirmacion = false) }
            try {
                ids.forEach { id ->
                    app.registroEntrenamientoRepository.deleteSeriesDeEjercicio(id)
                }
                _state.update { it.copy(seleccionados = emptySet()) }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(isDeleting = false, error = e.message) }
            }
        }
    }
}
