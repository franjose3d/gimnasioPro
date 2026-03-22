package com.example.gimnasiopro.presentation.detallerutina

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.Ejercicio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleRutinaUiState(
    val isLoading: Boolean = true,
    val titulo: String = "Rutina",
    val ejercicios: List<Ejercicio> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val toast: String? = null,
    val error: String? = null
)

class DetalleRutinaViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val numeroRutina  = savedState.get<Int>("extra_numero_rutina") ?: 1
    private val modoTrainer   = savedState.get<Boolean>("MODO_TRAINER") ?: false
    private val clienteId     = savedState.get<String>("CLIENTE_ID")
    private val clienteNombre = savedState.get<String>("CLIENTE_NOMBRE")

    private val app              = application as GimnasioproApplication
    private val rutinaRepo       = app.rutinaRepositoryHibrido
    private val ejercicioRepo    = app.ejercicioRepository
    private val auth             = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(DetalleRutinaUiState(
        titulo = buildTitulo()
    ))
    val state: StateFlow<DetalleRutinaUiState> = _state.asStateFlow()

    val esModoTrainer get() = modoTrainer
    val numRutina     get() = numeroRutina

    companion object {
        private const val TAG = "DetalleRutinaVM"
    }

    init {
        load()
    }

    private fun buildTitulo() = when {
        modoTrainer && clienteNombre != null -> "Rutina $numeroRutina de $clienteNombre"
        else -> "Rutina $numeroRutina"
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        if (modoTrainer && clienteId != null) {
            loadFromFirestore()
        } else {
            loadFromRoom()
        }
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            rutinaRepo.getRutinaByIdFlow(numeroRutina).collectLatest { rutina ->
                val ids = rutina?.getEjercicioIdsList() ?: emptyList()
                resolveEjercicios(ids)
            }
        }
    }

    private fun loadFromFirestore() {
        viewModelScope.launch {
            try {
                val trainerId = auth.currentUser?.uid ?: return@launch
                val rutina = rutinaRepo.getRutinaDeClientePorNumero(clienteId!!, trainerId, numeroRutina)
                val ids = rutina?.ejercicioIds?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                resolveEjercicios(ids)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Firestore: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun resolveEjercicios(ids: List<Long>) {
        if (ids.isEmpty()) {
            _state.update { it.copy(isLoading = false, ejercicios = emptyList()) }
            return
        }
        val raw = ejercicioRepo.getEjerciciosByIds(ids)
        val ordered = ids.mapNotNull { id -> raw.find { it.id == id } }
        _state.update { it.copy(isLoading = false, ejercicios = ordered) }
    }

    // ── Selection ────────────────────────────────────────────────────────────

    fun toggleSelection(id: Long) {
        _state.update { s ->
            val sel = s.selectedIds.toMutableSet()
            if (id in sel) sel.remove(id) else sel.add(id)
            s.copy(selectedIds = sel)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun limpiarRutina() {
        viewModelScope.launch {
            try {
                if (modoTrainer && clienteId != null) {
                    val trainerId = auth.currentUser?.uid ?: return@launch
                    rutinaRepo.limpiarEjerciciosDeRutinaCliente(clienteId!!, trainerId, numeroRutina).fold(
                        onSuccess = {
                            toast("Rutina limpiada")
                            loadFromFirestore()
                        },
                        onFailure = { e -> toast("Error: ${e.message}") }
                    )
                } else {
                    rutinaRepo.limpiarEjerciciosDeRutina(numeroRutina).fold(
                        onSuccess = { toast("Rutina limpiada") },
                        onFailure = { e -> toast("Error: ${e.message}") }
                    )
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun eliminarSeleccionados() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                if (modoTrainer && clienteId != null) {
                    val trainerId = auth.currentUser?.uid ?: return@launch
                    rutinaRepo.eliminarEjerciciosDeRutinaCliente(clienteId!!, trainerId, numeroRutina, ids).fold(
                        onSuccess = { n ->
                            clearSelection()
                            toast("$n ejercicio(s) eliminado(s)")
                            loadFromFirestore()
                        },
                        onFailure = { e -> toast("Error: ${e.message}") }
                    )
                } else {
                    val n = rutinaRepo.eliminarEjerciciosDeRutina(numeroRutina, ids)
                    clearSelection()
                    toast("$n ejercicio(s) eliminado(s)")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
