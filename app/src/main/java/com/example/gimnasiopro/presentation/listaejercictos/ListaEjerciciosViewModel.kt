package com.example.gimnasiopro.presentation.listaejercictos

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.RutinaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaEjerciciosUiState(
    val isLoading: Boolean = true,
    val grupoMuscular: String = "",
    val ejercicios: List<Ejercicio> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isTrainer: Boolean = false,
    val isCargandoFirebase: Boolean = false,
    val toast: String? = null,
    val error: String? = null
)

class ListaEjerciciosViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val grupoMuscular    = savedState.get<String>("extra_grupo_muscular") ?: ""
    private val numeroRutinaDestino = savedState.get<Int>("extra_numero_rutina") ?: -1

    private val app              = application as GimnasioproApplication
    private val ejercicioRepo    = app.ejercicioRepository
    private val rutinaRepo       = app.rutinaRepositoryHibrido
    private val auth             = FirebaseAuth.getInstance()
    private val firestore        = FirebaseFirestore.getInstance()

    val numRutina get() = numeroRutinaDestino

    private val _state = MutableStateFlow(ListaEjerciciosUiState(grupoMuscular = grupoMuscular))
    val state: StateFlow<ListaEjerciciosUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ListaEjerciciosVM"
        private const val MAX_EJERCICIOS = 10
        val GRUPOS_MUSCULARES = listOf(
            "Pectorales", "Espalda", "Hombros", "Bíceps y Antebrazo",
            "Tríceps", "Abdominales", "Piernas", "Glúteos", "Gemelos"
        )
    }

    init {
        detectarTipoUsuario()
        loadEjercicios()
    }

    private fun detectarTipoUsuario() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("trainers").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) _state.update { it.copy(isTrainer = true) }
            }
    }

    fun loadEjercicios() {
        if (grupoMuscular.isBlank()) {
            _state.update { it.copy(isLoading = false, error = "Grupo muscular no especificado") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                ejercicioRepo.getEjerciciosByGrupoMuscular(grupoMuscular).collectLatest { lista ->
                    _state.update { it.copy(isLoading = false, ejercicios = lista) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

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

    // ── Save selection ────────────────────────────────────────────────────────

    fun guardarEnRutina(numeroRutina: Int) {
        val ids = _state.value.selectedIds.map { it.toInt() }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val resultado = rutinaRepo.agregarEjerciciosARutina(numeroRutina, ids)
                toast(resultado.mensaje)
                if (resultado.exito) clearSelection()
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    suspend fun getRutinaInfo(): List<Pair<Int, Int>> {
        return (1..10).map { num ->
            num to rutinaRepo.getNumeroEjerciciosEnRutina(num)
        }
    }

    // ── Firebase load ────────────────────────────────────────────────────────

    fun cargarDesdeFirebase() {
        val uid = auth.currentUser?.uid
        if (uid == null) { toast("Inicia sesión para cargar ejercicios"); return }
        _state.update { it.copy(isCargandoFirebase = true) }
        viewModelScope.launch {
            try {
                val remote = app.ejercicioFirestoreRepository
                val remotos = remote.getEjerciciosByGrupoMuscular(grupoMuscular).first()
                var cargados = 0
                remotos.forEach { remoto ->
                    val existe = ejercicioRepo.getEjerciciosByGrupoMuscular(grupoMuscular).first()
                        .any { it.nombre.equals(remoto.nombre, ignoreCase = true) }
                    if (!existe) {
                        val e = Ejercicio(
                            grupoMuscular = remoto.grupoMuscular,
                            nombre = remoto.nombre,
                            descripcion = remoto.descripcion,
                            imagenUrl = remoto.imagenUrl
                        )
                        ejercicioRepo.insertEjercicio(e)
                        cargados++
                    }
                }
                val listaActualizada = app.localEjercicioRepository
                    .getEjerciciosByGrupoMuscular(grupoMuscular).first()
                _state.update { it.copy(isCargandoFirebase = false, ejercicios = listaActualizada) }
                toast(if (cargados > 0) "$cargados ejercicio(s) cargado(s)" else "No hay más ejercicios nuevos")
            } catch (e: Exception) {
                _state.update { it.copy(isCargandoFirebase = false) }
                toast("Error: ${e.message}")
            }
        }
    }

    // ── Add exercise (trainer only) ───────────────────────────────────────────

    fun agregarEjercicio(grupo: String, nombre: String, descripcion: String, grupoSecundario: String? = null) {
        if (nombre.isBlank()) { toast("El nombre no puede estar vacío"); return }
        if (descripcion.isBlank()) { toast("La descripción no puede estar vacía"); return }
        viewModelScope.launch {
            try {
                val remote = app.ejercicioFirestoreRepository
                val existentes = remote.getEjerciciosByGrupoMuscular(grupo).first()
                if (existentes.any { it.nombre.equals(nombre, ignoreCase = true) }) {
                    toast("Ya existe un ejercicio llamado \"$nombre\"")
                    return@launch
                }
                val trainerId = auth.currentUser?.uid
                val nuevo = Ejercicio(
                    grupoMuscular = grupo,
                    grupoMuscularSecundario = grupoSecundario,
                    nombre = nombre,
                    descripcion = descripcion
                )
                ejercicioRepo.insertEjercicio(nuevo, trainerId)
                toast("✅ Ejercicio agregado correctamente")
            } catch (e: Exception) {
                toast("Error al guardar: ${e.message}")
            }
        }
    }

    // ── Delete local ─────────────────────────────────────────────────────────

    fun eliminarLocal(ejercicio: Ejercicio) {
        viewModelScope.launch {
            try {
                app.localEjercicioRepository.deleteEjercicio(ejercicio)
                val listaActualizada = app.localEjercicioRepository
                    .getEjerciciosByGrupoMuscular(grupoMuscular).first()
                _state.update { it.copy(ejercicios = listaActualizada) }
                toast("Ejercicio eliminado de tu dispositivo")
            } catch (e: Exception) {
                toast("Error al eliminar: ${e.message}")
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
