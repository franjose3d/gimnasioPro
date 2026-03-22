package com.example.gimnasiopro.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PerfilUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val tipoUsuario: String = "",  // "trainer" | "cliente"
    val nombre: String = "",
    val telefono: String = "",
    // Trainer
    val dni: String = "",
    val poblacion: String = "",
    val municipio: String = "",
    val sobreMi: String = "",
    val tarifa: String = "",
    // Cliente
    val peso: String = "",
    val altura: String = "",
    val edad: String = "",
    val objetivo: String = "Pérdida de peso",
    val nivel: String = "Principiante",
    // Status
    val saveSuccess: Boolean = false,
    val cuentaEliminada: Boolean = false,
    val needsReauth: Boolean = false,
    val error: String? = null
)

class PerfilViewModel : ViewModel() {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val userId: String get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(PerfilUiState())
    val state: StateFlow<PerfilUiState> = _state.asStateFlow()

    init { cargarPerfil() }

    fun cargarPerfil() {
        _state.update { it.copy(isLoading = true, error = null) }
        val uid = userId
        viewModelScope.launch {
            try {
                val userInfo = UserHelper.getUserInfo(uid)
                if (userInfo == null) {
                    _state.update { it.copy(isLoading = false, error = "No se encontró el perfil") }
                    return@launch
                }
                val email    = auth.currentUser?.email ?: ""
                val tipo     = userInfo.tipo
                val nombre   = userInfo.nombre ?: ""
                val telefono = userInfo.documento?.getString("telefono") ?: ""

                when (tipo) {
                    "trainer" -> {
                        val doc = firestore.collection("trainers").document(uid).get().await()
                        _state.update { it.copy(
                            isLoading = false, email = email, tipoUsuario = tipo,
                            nombre = nombre, telefono = telefono,
                            dni       = doc.getString("dni") ?: "",
                            poblacion = doc.getString("poblacion") ?: "",
                            municipio = doc.getString("municipio") ?: "",
                            sobreMi   = doc.getString("sobreMi") ?: "",
                            tarifa    = doc.getDouble("tarifa")?.let { if (it > 0) it.toString() else "" } ?: ""
                        )}
                    }
                    "cliente" -> {
                        val doc = firestore.collection("clientes").document(uid).get().await()
                        val objetivos = listOf("Pérdida de peso","Ganancia muscular","Mantenimiento","Tonificación","Resistencia")
                        val niveles   = listOf("Principiante","Intermedio","Avanzado")
                        _state.update { it.copy(
                            isLoading = false, email = email, tipoUsuario = tipo,
                            nombre = nombre, telefono = telefono,
                            peso    = doc.getDouble("peso")?.let { if (it > 0) it.toString() else "" } ?: "",
                            altura  = doc.getDouble("altura")?.let { if (it > 0) it.toString() else "" } ?: "",
                            edad    = doc.getLong("edad")?.let { if (it > 0) it.toString() else "" } ?: "",
                            objetivo = doc.getString("objetivo")?.takeIf { it in objetivos } ?: objetivos[0],
                            nivel    = doc.getString("nivel")?.takeIf { it in niveles } ?: niveles[0]
                        )}
                    }
                    else -> _state.update { it.copy(isLoading = false, tipoUsuario = tipo, email = email, nombre = nombre) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al cargar perfil: ${e.message}") }
            }
        }
    }

    fun onNombreChange(v: String)   = _state.update { it.copy(nombre = v) }
    fun onTelefonoChange(v: String) = _state.update { it.copy(telefono = v) }
    fun onDniChange(v: String)      = _state.update { it.copy(dni = v) }
    fun onPoblacionChange(v: String)= _state.update { it.copy(poblacion = v) }
    fun onMunicipioChange(v: String)= _state.update { it.copy(municipio = v) }
    fun onSobreMiChange(v: String)  = _state.update { it.copy(sobreMi = v) }
    fun onTarifaChange(v: String)   = _state.update { it.copy(tarifa = v) }
    fun onPesoChange(v: String)     = _state.update { it.copy(peso = v) }
    fun onAlturaChange(v: String)   = _state.update { it.copy(altura = v) }
    fun onEdadChange(v: String)     = _state.update { it.copy(edad = v) }
    fun onObjetivoChange(v: String) = _state.update { it.copy(objetivo = v) }
    fun onNivelChange(v: String)    = _state.update { it.copy(nivel = v) }
    fun clearError()                = _state.update { it.copy(error = null) }
    fun clearSaveSuccess()          = _state.update { it.copy(saveSuccess = false) }

    fun guardarCambios() {
        val s = _state.value
        if (s.nombre.isBlank()) {
            _state.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val normTelefono = s.telefono.replace(Regex("[^0-9]"), "").let { d ->
                    if (d.startsWith("34") && d.length > 9) d.removePrefix("34") else d
                }
                val datos: Map<String, Any> = when (s.tipoUsuario) {
                    "trainer" -> mapOf(
                        "nombre" to s.nombre, "telefono" to s.telefono,
                        "telefonoNormalizado" to normTelefono,
                        "dni" to s.dni, "poblacion" to s.poblacion,
                        "municipio" to s.municipio, "sobreMi" to s.sobreMi,
                        "tarifa" to (s.tarifa.toDoubleOrNull() ?: 0.0)
                    )
                    "cliente" -> mapOf(
                        "nombre" to s.nombre, "telefono" to s.telefono,
                        "telefonoNormalizado" to normTelefono,
                        "peso"    to (s.peso.toDoubleOrNull() ?: 0.0),
                        "altura"  to (s.altura.toDoubleOrNull() ?: 0.0),
                        "edad"    to (s.edad.toIntOrNull() ?: 0),
                        "objetivo" to s.objetivo, "nivel" to s.nivel
                    )
                    else -> emptyMap()
                }
                val coleccion = if (s.tipoUsuario == "trainer") "trainers" else "clientes"
                firestore.collection(coleccion).document(userId).update(datos).await()
                _state.update { it.copy(isLoading = false, saveSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun eliminarCuenta() {
        val tipo = _state.value.tipoUsuario
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val coleccion = if (tipo == "trainer") "trainers" else "clientes"
                firestore.collection(coleccion).document(userId).delete().await()
                try {
                    auth.currentUser?.delete()?.await()
                    _state.update { it.copy(isLoading = false, cuentaEliminada = true) }
                } catch (authEx: Exception) {
                    val isReauth = authEx.message?.contains("requires-recent-login") == true ||
                                   authEx.message?.contains("recent") == true
                    _state.update { it.copy(isLoading = false, needsReauth = isReauth,
                        error = if (!isReauth) "Datos eliminados. Error de auth: ${authEx.message}" else null) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al eliminar cuenta: ${e.message}") }
            }
        }
    }
}
