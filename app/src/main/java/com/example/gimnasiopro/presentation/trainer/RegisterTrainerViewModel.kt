package com.example.gimnasiopro.presentation.trainer

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasiopro.data.repository.TrainerRepository
import com.example.gimnasiopro.domain.model.Trainer
import com.example.gimnasiopro.utils.UserPhotoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class RegisterTrainerUiState {
    object Idle    : RegisterTrainerUiState()
    object Loading : RegisterTrainerUiState()
    object Success : RegisterTrainerUiState()
    data class Error(val message: String) : RegisterTrainerUiState()
}

class RegisterTrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val auth       = FirebaseAuth.getInstance()
    private val storage    = FirebaseStorage.getInstance()
    private val repository = TrainerRepository()

    private val _uiState = MutableStateFlow<RegisterTrainerUiState>(RegisterTrainerUiState.Idle)
    val uiState: StateFlow<RegisterTrainerUiState> = _uiState.asStateFlow()

    fun register(
        email: String,
        password: String,
        nombre: String,
        dni: String,
        poblacion: String,
        municipio: String,
        telefonoSinPrefijo: String,
        sobreMi: String,
        tarifaStr: String,
        fotoUri: Uri?,
        certificadoUri: Uri?
    ) {
        val dniUpper = dni.trim().uppercase()
        val validationError = validate(email, password, nombre, dniUpper, poblacion, municipio, telefonoSinPrefijo, tarifaStr)
        if (validationError != null) {
            _uiState.value = RegisterTrainerUiState.Error(validationError)
            return
        }

        val tarifa  = tarifaStr.toDoubleOrNull() ?: 0.0
        val telefono = "+34$telefonoSinPrefijo"

        _uiState.value = RegisterTrainerUiState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user   = result.user
                val userId = user?.uid ?: ""
                user?.sendEmailVerification()

                subirArchivosYGuardar(
                    userId, email, nombre, dniUpper, poblacion, municipio,
                    telefono, sobreMi, tarifa, fotoUri, certificadoUri
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = RegisterTrainerUiState.Error(
                    when {
                        e.message?.contains("already in use") == true -> "Este email ya está registrado"
                        e.message?.contains("password") == true       -> "Contraseña inválida"
                        else                                           -> "Error: ${e.message}"
                    }
                )
            }
    }

    private fun subirArchivosYGuardar(
        userId: String, email: String, nombre: String, dni: String,
        poblacion: String, municipio: String, telefono: String,
        sobreMi: String, tarifa: Double,
        fotoUri: Uri?, certificadoUri: Uri?
    ) {
        val total = listOfNotNull(fotoUri, certificadoUri).size
        if (total == 0) {
            guardarEnFirestore(userId, email, nombre, dni, poblacion, municipio, telefono, sobreMi, "", "", tarifa)
            return
        }

        var done = 0
        var fotoUrl = ""
        var certUrl = ""

        val checkDone = {
            done++
            if (done >= total) {
                guardarEnFirestore(userId, email, nombre, dni, poblacion, municipio, telefono, sobreMi, fotoUrl, certUrl, tarifa)
            }
        }

        fotoUri?.let { uri ->
            val ref = storage.reference.child("trainers/$userId/foto_${UUID.randomUUID()}")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        fotoUrl = url.toString()
                        UserPhotoManager.guardarFotoUrl(getApplication(), userId, fotoUrl)
                        checkDone()
                    }.addOnFailureListener { checkDone() }
                }
                .addOnFailureListener { checkDone() }
        }

        certificadoUri?.let { uri ->
            val ext = getExtension(uri)
            val ref = storage.reference.child("trainers/$userId/certificado_${UUID.randomUUID()}.$ext")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        certUrl = url.toString()
                        checkDone()
                    }.addOnFailureListener { checkDone() }
                }
                .addOnFailureListener { checkDone() }
        }
    }

    private fun guardarEnFirestore(
        userId: String, email: String, nombre: String, dni: String,
        poblacion: String, municipio: String, telefono: String,
        sobreMi: String, fotoUrl: String, certificadoUrl: String, tarifa: Double
    ) {
        viewModelScope.launch {
            val trainer = Trainer(
                userId = userId,
                email = email,
                nombre = nombre,
                dni = dni,
                poblacion = poblacion,
                municipio = municipio,
                telefono = telefono,
                sobreMi = sobreMi,
                fotoUrl = fotoUrl,
                certificadoUrl = certificadoUrl,
                tarifa = tarifa,
                activo = true,
                emailVerificado = false,
                fechaRegistro = System.currentTimeMillis()
            )
            val res = repository.registerTrainer(trainer)
            _uiState.value = if (res.isSuccess) {
                RegisterTrainerUiState.Success
            } else {
                RegisterTrainerUiState.Error(res.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    private fun getExtension(uri: Uri): String {
        val mime = getApplication<Application>().contentResolver.getType(uri)
        return when {
            mime?.contains("pdf")  == true -> "pdf"
            mime?.contains("png")  == true -> "png"
            else                           -> "jpg"
        }
    }

    fun resetState() { _uiState.value = RegisterTrainerUiState.Idle }

    private fun validate(
        email: String, password: String, nombre: String, dni: String,
        poblacion: String, municipio: String, telefono: String, tarifaStr: String
    ): String? = when {
        email.isBlank()                                           -> "Email obligatorio"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches()          -> "Email inválido"
        password.isBlank()                                        -> "Contraseña obligatoria"
        password.length < 6                                       -> "Mínimo 6 caracteres"
        nombre.isBlank()                                          -> "Nombre obligatorio"
        dni.isBlank()                                             -> "DNI obligatorio"
        !validarDNI(dni)                                          -> "DNI inválido (8 dígitos + letra)"
        poblacion.isBlank()                                       -> "Población obligatoria"
        municipio.isBlank()                                       -> "Municipio obligatorio"
        telefono.isBlank()                                        -> "Teléfono obligatorio"
        !telefono.matches("^[0-9]{9}$".toRegex())                -> "Teléfono inválido (9 dígitos)"
        tarifaStr.isNotBlank() && (tarifaStr.toDoubleOrNull() ?: -1.0) < 0 -> "Tarifa no puede ser negativa"
        else                                                       -> null
    }

    private fun validarDNI(dni: String): Boolean {
        val regex = "^[0-9]{8}[A-Z]$".toRegex()
        if (!regex.matches(dni)) return false
        val letras = "TRWAGMYFPDXBNJZSQVHLCKE"
        val numero = dni.substring(0, 8).toIntOrNull() ?: return false
        return letras[numero % 23] == dni[8]
    }
}
