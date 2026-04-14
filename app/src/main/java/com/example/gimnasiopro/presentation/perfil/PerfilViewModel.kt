package com.example.gimnasiopro.presentation.perfil

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val userId: String get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(PerfilUiState())
    val state: StateFlow<PerfilUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "PerfilViewModel"
    }

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
        val uid  = userId
        if (uid.isEmpty()) {
            _state.update { it.copy(error = "Sesión no iniciada") }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val coleccion = if (tipo == "trainer") "trainers" else "clientes"

                // 1. Subcolecciones del documento principal
                val subcollections = if (tipo == "trainer") {
                    listOf("rutinas", "estadisticas", "entrenamientos", "calendario", "misClientes")
                } else {
                    listOf("rutinas", "estadisticas", "entrenamientos", "calendario", "miTrainer")
                }
                for (sub in subcollections) {
                    eliminarSubcoleccion(coleccion, uid, sub)
                }

                // 2. Mensajes pendientes entrantes
                eliminarSubcoleccion("mensajes_pendientes", uid, "mensajes")

                // 3. Documento principal
                try {
                    firestore.collection(coleccion).document(uid).delete().await()
                    Log.d(TAG, "✅ Documento principal eliminado: $coleccion/$uid")
                } catch (e: Exception) {
                    Log.w(TAG, "Error eliminando documento principal: ${e.message}")
                }

                // 4. Conexiones (como cliente o como trainer)
                val campoConexion = if (tipo == "trainer") "trainerId" else "clienteId"
                eliminarPorCampo("conexiones", campoConexion, uid)

                // 5. Notificaciones (como destinatario y como remitente)
                eliminarPorCampo("notificaciones", "destinatarioId", uid)
                eliminarPorCampo("notificaciones", "remitenteId", uid)

                // 6. Token FCM
                try {
                    firestore.collection("users").document(uid).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error eliminando users/$uid: ${e.message}")
                }

                // 7. Datos locales de Room
                try {
                    val db = GymDatabase.getDatabase(getApplication())
                    withContext(Dispatchers.IO) {
                        db.clearAllTables()
                    }
                    Log.d(TAG, "✅ Datos locales eliminados")
                } catch (e: Exception) {
                    Log.w(TAG, "Error eliminando datos locales: ${e.message}")
                }

                // 8. Cuenta Firebase Auth — SIEMPRE al final
                try {
                    auth.currentUser?.delete()?.await()
                    Log.d(TAG, "✅ Cuenta Auth eliminada")
                    _state.update { it.copy(isLoading = false, cuentaEliminada = true) }
                } catch (authEx: Exception) {
                    val isReauth = authEx.message?.contains("requires-recent-login") == true ||
                                   authEx.message?.contains("recent") == true
                    _state.update { it.copy(
                        isLoading = false,
                        needsReauth = isReauth,
                        error = if (!isReauth) "Datos eliminados. Error de auth: ${authEx.message}" else null
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al eliminar cuenta: ${e.message}") }
            }
        }
    }

    // Elimina todos los documentos de una subcolección en lotes de 500
    private suspend fun eliminarSubcoleccion(coleccion: String, docId: String, subcoleccion: String) {
        try {
            val snapshot = firestore.collection(coleccion).document(docId)
                .collection(subcoleccion).get().await()
            if (snapshot.isEmpty) return
            // Firestore batch limit: 500 operaciones
            snapshot.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Log.d(TAG, "✅ Subcolección $coleccion/$docId/$subcoleccion eliminada (${snapshot.size()} docs)")
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando $coleccion/$docId/$subcoleccion: ${e.message}")
        }
    }

    // Elimina documentos de una colección raíz filtrando por campo=valor, en lotes de 500
    private suspend fun eliminarPorCampo(coleccion: String, campo: String, valor: String) {
        try {
            val snapshot = firestore.collection(coleccion)
                .whereEqualTo(campo, valor).get().await()
            if (snapshot.isEmpty) return
            snapshot.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Log.d(TAG, "✅ $coleccion donde $campo=$valor eliminados (${snapshot.size()} docs)")
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando $coleccion por $campo: ${e.message}")
        }
    }
}
