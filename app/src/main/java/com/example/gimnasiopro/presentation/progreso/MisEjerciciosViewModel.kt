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
    val error: String? = null
)

class MisEjerciciosViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GimnasioproApplication

    private val _state = MutableStateFlow(MisEjerciciosUiState())
    val state: StateFlow<MisEjerciciosUiState> = _state.asStateFlow()

    init { load() }

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
}
