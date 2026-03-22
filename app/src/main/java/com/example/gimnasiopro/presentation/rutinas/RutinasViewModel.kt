package com.example.gimnasiopro.presentation.rutinas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.firestore.RutinaFirestore
import com.example.gimnasiopro.data.firestore.RutinaFirestoreRepository
import com.example.gimnasiopro.utils.RutinaClipboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RutinaItem(
    val numero: Int,
    val nombre: String,
    val tieneEjercicios: Boolean
)

data class RutinasUiState(
    val isLoading: Boolean = true,
    val titulo: String = "Mis Rutinas",
    val cantidadRutinas: Int = 10,
    val rutinas: List<RutinaItem> = emptyList(),
    val tieneRutinaCopiada: Boolean = false,
    val toast: String? = null,
    val error: String? = null
)

class RutinasViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val modoTrainer   = savedState.get<Boolean>("MODO_TRAINER") ?: false
    private val clienteId     = savedState.get<String>("CLIENTE_ID")
    private val clienteNombre = savedState.get<String>("CLIENTE_NOMBRE")

    private val app           = application as GimnasioproApplication
    private val auth          = FirebaseAuth.getInstance()
    private val db            = GymDatabase.getDatabase(application)
    private val rutinaRepo    = RutinaRepository(db.rutinaDao())

    // In-memory cache of Firestore routines (trainer mode)
    private var rutinasFirestoreCache: Map<Int, RutinaFirestore> = emptyMap()

    private val _state = MutableStateFlow(RutinasUiState(
        titulo = if (modoTrainer && clienteNombre != null) "Rutinas de $clienteNombre" else "Mis Rutinas"
    ))
    val state: StateFlow<RutinasUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "RutinasViewModel"
        private const val MIN_RUTINAS = 1
        private const val MAX_RUTINAS = 20
    }

    init {
        actualizarRutinaCopiada()
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            if (modoTrainer && clienteId != null) {
                cargarRutinasFirestore()
            } else {
                sincronizarYCargar()
            }
        }
    }

    fun actualizarRutinaCopiada() {
        val tiene = RutinaClipboard.tieneRutinaCopiada(getApplication())
        _state.update { it.copy(tieneRutinaCopiada = tiene) }
    }

    // ── Load paths ────────────────────────────────────────────────────────────

    private suspend fun sincronizarYCargar() {
        try {
            app.rutinaRepositoryHibrido.sincronizarRutinasConFirebase()
                .onSuccess { n -> Log.d(TAG, "✅ $n rutinas sincronizadas") }
                .onFailure { e -> Log.w(TAG, "⚠️ Sync: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "Error sync: ${e.message}")
        }
        cargarRutinasRoom()
    }

    private suspend fun cargarRutinasRoom() {
        try {
            var cantidad = rutinaRepo.getCountRutinas()
            if (cantidad < MIN_RUTINAS) {
                cantidad = 10
                rutinaRepo.initializeRutinasIfNeeded()
            }
            val todas = rutinaRepo.getAllRutinas().first().associateBy { it.numeroRutina }
            val items = (1..cantidad).map { num ->
                val r = todas[num]
                RutinaItem(num, r?.nombre ?: "Rutina $num", r?.ejercicioIds?.isNotEmpty() == true)
            }
            _state.update { it.copy(isLoading = false, cantidadRutinas = cantidad, rutinas = items) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    private suspend fun cargarRutinasFirestore() {
        try {
            val trainerId = auth.currentUser?.uid ?: return
            val result = app.rutinaRepositoryHibrido.getRutinasDeCliente(clienteId!!, trainerId)
            result.fold(
                onSuccess = { rutinas ->
                    rutinasFirestoreCache = rutinas.associateBy { it.numeroRutina }
                    val cantidad = if (rutinas.isEmpty()) 10 else maxOf(10, rutinas.maxOf { it.numeroRutina })
                    val items = (1..cantidad).map { num ->
                        val r = rutinasFirestoreCache[num]
                        RutinaItem(num, r?.nombre ?: "Rutina $num", r?.ejercicioIds?.isNotEmpty() == true)
                    }
                    _state.update { it.copy(isLoading = false, cantidadRutinas = cantidad, rutinas = items) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    // ── Count management ──────────────────────────────────────────────────────

    fun incrementar() {
        val actual = _state.value.cantidadRutinas
        if (actual >= MAX_RUTINAS) { toast("Máximo $MAX_RUTINAS rutinas"); return }
        viewModelScope.launch {
            if (modoTrainer && clienteId != null) {
                val nuevo = actual + 1
                _state.update { it.copy(cantidadRutinas = nuevo, rutinas = it.rutinas + RutinaItem(nuevo, "Rutina $nuevo", false)) }
                toast("Rutina $nuevo disponible")
            } else {
                val nuevoNum = rutinaRepo.agregarNuevaRutina(MAX_RUTINAS)
                if (nuevoNum != null) {
                    val nuevo = actual + 1
                    _state.update { it.copy(cantidadRutinas = nuevo, rutinas = it.rutinas + RutinaItem(nuevo, "Rutina $nuevo", false)) }
                    toast("Rutina $nuevoNum añadida")
                } else toast("No se pudo añadir la rutina")
            }
        }
    }

    fun decrementar() {
        val actual = _state.value.cantidadRutinas
        if (actual <= MIN_RUTINAS) { toast("Mínimo $MIN_RUTINAS rutina"); return }
        viewModelScope.launch {
            if (modoTrainer && clienteId != null) {
                _state.update { it.copy(cantidadRutinas = actual - 1, rutinas = it.rutinas.dropLast(1)) }
                toast("Rutina ocultada")
            } else {
                val eliminada = rutinaRepo.eliminarUltimaRutina(MIN_RUTINAS)
                if (eliminada) {
                    _state.update { it.copy(cantidadRutinas = actual - 1, rutinas = it.rutinas.dropLast(1)) }
                    toast("Rutina eliminada")
                }
            }
        }
    }

    // ── Routine operations ────────────────────────────────────────────────────

    fun renombrar(numero: Int, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch {
            try {
                if (modoTrainer && clienteId != null) {
                    val docId = RutinaFirestore.generarDocumentId(numero)
                    FirebaseFirestore.getInstance()
                        .collection("clientes").document(clienteId!!)
                        .collection("rutinas").document(docId)
                        .update(
                            "nombre", nuevoNombre,
                            "fechaModificacion", com.google.firebase.Timestamp.now()
                        ).addOnSuccessListener {
                            rutinasFirestoreCache = rutinasFirestoreCache.toMutableMap().apply {
                                this[numero]?.let { put(numero, it.copy(nombre = nuevoNombre)) }
                            }
                            actualizarItem(numero) { item -> item.copy(nombre = nuevoNombre) }
                            toast("Nombre actualizado")
                        }
                } else {
                    rutinaRepo.actualizarNombreRutina(numero, nuevoNombre)
                    val rutina = rutinaRepo.getRutinaByNumeroSync(numero)
                    rutina?.let { app.rutinaRepositoryHibrido.saveRutina(it) }
                    actualizarItem(numero) { item -> item.copy(nombre = nuevoNombre) }
                    toast("Nombre actualizado")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun copiarRutina(numero: Int) {
        viewModelScope.launch {
            try {
                val ids: List<String>
                val nombre: String
                if (modoTrainer && clienteId != null) {
                    val r = rutinasFirestoreCache[numero]
                    if (r == null) { toast("⚠️ No se pudo copiar la rutina"); return@launch }
                    ids = r.ejercicioIds
                    nombre = r.nombre
                } else {
                    val r = rutinaRepo.getRutinaByNumero(numero).first()
                    if (r == null) { toast("⚠️ No se pudo copiar la rutina"); return@launch }
                    ids = r.ejercicioIds.map { it.toString() }
                    nombre = r.nombre
                }
                if (ids.isEmpty()) { toast("⚠️ La rutina está vacía (sin ejercicios)"); return@launch }
                RutinaClipboard.copiar(getApplication(), numero, nombre, ids)
                _state.update { it.copy(tieneRutinaCopiada = true) }
                toast("✅ Rutina copiada: $nombre (${ids.size} ejercicios)")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun pegarRutina(numero: Int) {
        val copiada = RutinaClipboard.obtenerRutinaCopiada(getApplication()) ?: run {
            toast("⚠️ No hay ninguna rutina copiada")
            return
        }
        viewModelScope.launch {
            try {
                val nombreDestino = "Copia de ${copiada.nombreRutina}"
                if (modoTrainer && clienteId != null) {
                    val trainerId = auth.currentUser?.uid ?: return@launch
                    app.rutinaRepositoryHibrido.crearRutinaParaCliente(
                        trainerId, clienteId!!, numero, nombreDestino, copiada.ejercicioIds
                    )
                    cargarRutinasFirestore()
                } else {
                    val ids = copiada.ejercicioIds.mapNotNull { it.toIntOrNull() }
                    app.rutinaRepositoryHibrido.actualizarEjerciciosDeRutina(numero, ids)
                    rutinaRepo.actualizarNombreRutina(numero, nombreDestino)
                    cargarRutinasRoom()
                }
                toast("✅ Rutina pegada correctamente")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun limpiarRutina(numero: Int) {
        viewModelScope.launch {
            try {
                if (modoTrainer && clienteId != null) {
                    val trainerId = auth.currentUser?.uid ?: return@launch
                    app.rutinaRepositoryHibrido.limpiarEjerciciosDeRutinaCliente(clienteId!!, trainerId, numero)
                    rutinasFirestoreCache = rutinasFirestoreCache.toMutableMap().apply { remove(numero) }
                    actualizarItem(numero) { item -> item.copy(tieneEjercicios = false) }
                } else {
                    app.rutinaRepositoryHibrido.limpiarEjerciciosDeRutina(numero)
                    actualizarItem(numero) { item -> item.copy(tieneEjercicios = false) }
                }
                toast("Ejercicios eliminados")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun actualizarItem(numero: Int, transform: (RutinaItem) -> RutinaItem) {
        _state.update { s ->
            s.copy(rutinas = s.rutinas.map { if (it.numero == numero) transform(it) else it })
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }

    // Expose extras for DetalleRutinaActivity navigation
    val esModoTrainer get() = modoTrainer
    val idCliente get() = clienteId
    val nombreCliente get() = clienteNombre
}
