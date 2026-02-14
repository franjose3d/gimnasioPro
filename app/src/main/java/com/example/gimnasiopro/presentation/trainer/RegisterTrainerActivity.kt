package com.example.gimnasiopro.presentation.trainer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasiopro.R
import com.example.gimnasiopro.presentation.auth.VerificarEmailActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity SIMPLIFICADA para registro de trainers.
 *
 * TODO en un solo archivo, fácil de entender.
 *
 * FLUJO:
 * 1. Usuario completa formulario
 * 2. Se crea cuenta Firebase Auth (email + password)
 * 3. Se envía email de verificación
 * 4. Se guarda trainer en Firestore
 * 5. Se va a VerificarEmailActivity
 */
class RegisterTrainerActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: RegisterTrainerViewModel

    // Views - Autenticación
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText

    // Views - Datos personales
    private lateinit var etNombre: EditText
    private lateinit var etDNI: EditText
    private lateinit var etPoblacion: EditText
    private lateinit var etMunicipio: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etSobreMi: EditText
    private lateinit var etTarifa: EditText
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSeleccionarFoto: Button

    // Views - UI
    private lateinit var btnRegistrar: Button
    private lateinit var progressBar: ProgressBar

    // Foto
    private var fotoUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_trainer)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this)[RegisterTrainerViewModel::class.java]

        initViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initViews() {
        // Autenticación
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etpasswordConfirm)

        // Datos
        etNombre = findViewById(R.id.etnombre)
        etDNI = findViewById(R.id.etDNI)
        etPoblacion = findViewById(R.id.etpoblacion)
        etMunicipio = findViewById(R.id.etmunicipio)
        etTelefono = findViewById(R.id.ettelefono)
        etSobreMi = findViewById(R.id.etsobreMi)
        etTarifa = findViewById(R.id.ettarifa)
        ivFotoPerfil = findViewById(R.id.ivfotoPerfil)
        btnSeleccionarFoto = findViewById(R.id.btnseleccionarFoto)

        // UI
        btnRegistrar = findViewById(R.id.btnregistrar)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnRegistrar.setOnClickListener {
            validarYRegistrar()
        }

        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    /**
     * VALIDAR Y REGISTRAR
     */
    private fun validarYRegistrar() {
        // Limpiar errores
        limpiarErrores()

        // Obtener valores
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val passwordConfirm = etPasswordConfirm.text.toString()
        val nombre = etNombre.text.toString().trim()
        val dni = etDNI.text.toString().trim().uppercase()
        val poblacion = etPoblacion.text.toString().trim()
        val municipio = etMunicipio.text.toString().trim()
        val telefonoSinPrefijo = etTelefono.text.toString().trim()
        val sobreMi = etSobreMi.text.toString().trim()
        val tarifaStr = etTarifa.text.toString().trim()

        var hayError = false

        // VALIDAR EMAIL
        if (email.isBlank()) {
            etEmail.error = "Email obligatorio"
            hayError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            hayError = true
        }

        // VALIDAR PASSWORD
        if (password.isBlank()) {
            etPassword.error = "Contraseña obligatoria"
            hayError = true
        } else if (password.length < 6) {
            etPassword.error = "Mínimo 6 caracteres"
            hayError = true
        }

        if (passwordConfirm != password) {
            etPasswordConfirm.error = "Las contraseñas no coinciden"
            hayError = true
        }

        // VALIDAR NOMBRE
        if (nombre.isBlank()) {
            etNombre.error = "Nombre obligatorio"
            hayError = true
        }

        // VALIDAR DNI
        if (dni.isBlank()) {
            etDNI.error = "DNI obligatorio"
            hayError = true
        } else if (!validarDNI(dni)) {
            etDNI.error = "DNI inválido (8 dígitos + letra)"
            hayError = true
        }

        // VALIDAR UBICACIÓN
        if (poblacion.isBlank()) {
            etPoblacion.error = "Población obligatoria"
            hayError = true
        }

        if (municipio.isBlank()) {
            etMunicipio.error = "Municipio obligatorio"
            hayError = true
        }

        // VALIDAR TELÉFONO
        if (telefonoSinPrefijo.isBlank()) {
            etTelefono.error = "Teléfono obligatorio"
            hayError = true
        } else if (!validarTelefono(telefonoSinPrefijo)) {
            etTelefono.error = "Teléfono inválido (9 dígitos)"
            hayError = true
        }

        // VALIDAR TARIFA
        val tarifa = if (tarifaStr.isNotBlank()) {
            try {
                val valor = tarifaStr.toDouble()
                if (valor < 0) {
                    etTarifa.error = "Tarifa no puede ser negativa"
                    hayError = true
                    0.0
                } else {
                    valor
                }
            } catch (e: NumberFormatException) {
                etTarifa.error = "Tarifa inválida"
                hayError = true
                0.0
            }
        } else {
            0.0
        }

        if (hayError) return

        // Teléfono con prefijo
        val telefonoCompleto = "+34$telefonoSinPrefijo"

        // CREAR CUENTA FIREBASE
        crearCuentaFirebase(
            email, password, nombre, dni,
            poblacion, municipio, telefonoCompleto,
            sobreMi, tarifa
        )
    }

    /**
     * CREAR CUENTA FIREBASE AUTH
     */
    private fun crearCuentaFirebase(
        email: String,
        password: String,
        nombre: String,
        dni: String,
        poblacion: String,
        municipio: String,
        telefono: String,
        sobreMi: String,
        tarifa: Double
    ) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                val userId = user?.uid ?: ""

                // Enviar email de verificación
                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "📧 Email de verificación enviado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                // Guardar trainer en Firestore
                val fotoUrl = fotoUri?.toString() ?: ""

                viewModel.registerTrainer(
                    userId = userId,
                    email = email,
                    nombre = nombre,
                    dni = dni,
                    poblacion = poblacion,
                    municipio = municipio,
                    telefono = telefono,
                    sobreMi = sobreMi,
                    fotoUrl = fotoUrl,
                    tarifa = tarifa,
                    emailVerificado = false
                )
            }
            .addOnFailureListener { e ->
                showLoading(false)

                val mensaje = when {
                    e.message?.contains("already in use") == true ->
                        "Este email ya está registrado"
                    e.message?.contains("password") == true ->
                        "Contraseña inválida"
                    else ->
                        "Error: ${e.message}"
                }

                Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * VALIDAR DNI ESPAÑOL
     */
    private fun validarDNI(dni: String): Boolean {
        val regex = "^[0-9]{8}[A-Z]$".toRegex()
        if (!regex.matches(dni)) return false

        val letras = "TRWAGMYFPDXBNJZSQVHLCKE"
        val numero = dni.substring(0, 8).toIntOrNull() ?: return false
        val letraCalculada = letras[numero % 23]
        val letraProporcionada = dni[8]

        return letraCalculada == letraProporcionada
    }

    /**
     * VALIDAR TELÉFONO (9 dígitos)
     */
    private fun validarTelefono(telefono: String): Boolean {
        val regex = "^[0-9]{9}$".toRegex()
        return regex.matches(telefono)
    }

    /**
     * OBSERVAR VIEWMODEL
     */
    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is RegisterTrainerUiState.Idle -> {
                    // Nada
                }
                is RegisterTrainerUiState.Loading -> {
                    // Ya está en loading
                }
                is RegisterTrainerUiState.Success -> {
                    showLoading(false)
                    mostrarExito()
                }
                is RegisterTrainerUiState.Error -> {
                    showLoading(false)

                    val mensaje = when {
                        state.message.contains("DNI") ->
                            "Este DNI ya está registrado"
                        state.message.contains("teléfono") ->
                            "Este teléfono ya está registrado"
                        else ->
                            state.message
                    }

                    Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * MOSTRAR MENSAJE DE ÉXITO
     */
    private fun mostrarExito() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("✅ Cuenta creada")
            .setMessage(
                "Tu cuenta ha sido creada correctamente.\n\n" +
                        "📧 Revisa tu email (${etEmail.text}) para verificarlo.\n\n" +
                        "Tu perfil será revisado en 24-48h."
            )
            .setPositiveButton("Verificar ahora") { _, _ ->
                // Ir a pantalla de verificación
                val intent = Intent(this, VerificarEmailActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * RESULTADO SELECCIONAR FOTO
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                fotoUri = uri
                ivFotoPerfil.setImageURI(uri)
                Toast.makeText(this, "✅ Foto seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * LIMPIAR ERRORES
     */
    private fun limpiarErrores() {
        etEmail.error = null
        etPassword.error = null
        etPasswordConfirm.error = null
        etNombre.error = null
        etDNI.error = null
        etPoblacion.error = null
        etMunicipio.error = null
        etTelefono.error = null
        etTarifa.error = null
    }

    /**
     * LOADING
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegistrar.isEnabled = !show

        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etPasswordConfirm.isEnabled = !show
        etNombre.isEnabled = !show
        etDNI.isEnabled = !show
        etPoblacion.isEnabled = !show
        etMunicipio.isEnabled = !show
        etTelefono.isEnabled = !show
        etSobreMi.isEnabled = !show
        etTarifa.isEnabled = !show
        btnSeleccionarFoto.isEnabled = !show
    }
}