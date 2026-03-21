package com.example.gimnasiopro.presentation.cliente

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasiopro.R
import com.example.gimnasiopro.presentation.auth.VerificarEmailActivity
import com.example.gimnasiopro.utils.UserPhotoManager
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity para registro de clientes/usuarios.
 *
 * FLUJO:
 * 1. Usuario completa formulario
 * 2. Se crea cuenta Firebase Auth
 * 3. Se envía email verificación
 * 4. Se guarda cliente en Firestore
 * 5. Se va a VerificarEmailActivity
 */
class RegisterClienteActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: RegisterClienteViewModel

    // Views - Autenticación
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText

    // Views - Datos personales
    private lateinit var etNombre: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText
    private lateinit var etEdad: EditText
    private lateinit var spinnerGenero: Spinner
    private lateinit var spinnerObjetivo: Spinner
    private lateinit var spinnerNivel: Spinner
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSeleccionarFoto: Button

    // Views - UI
    private lateinit var btnRegistrar: Button
    private lateinit var progressBar: ProgressBar

    // Foto
    private var fotoUri: Uri? = null

    // ====== NUEVO: Términos y Condiciones ======
    private lateinit var checkboxTerminos: CheckBox
    private lateinit var tvTerminos: TextView

    // Launcher para solicitar permiso de notificaciones (Android 13+)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        }
        // Si el usuario deniega, no bloqueamos el registro — es opcional
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_cliente)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this)[RegisterClienteViewModel::class.java]

        initViews()
        setupSpinners()
        setupClickListeners()
        observeViewModel()

    }

    private fun initViews() {
        // Autenticación
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)

        // Datos
        etNombre = findViewById(R.id.etNombre)
        etTelefono = findViewById(R.id.etTelefono)
        etPeso = findViewById(R.id.etPeso)
        etAltura = findViewById(R.id.etAltura)
        etEdad = findViewById(R.id.etEdad)
        spinnerGenero = findViewById(R.id.spinnerGenero)
        spinnerObjetivo = findViewById(R.id.spinnerObjetivo)
        spinnerNivel = findViewById(R.id.spinnerNivel)
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)

        // UI
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progressBar = findViewById(R.id.progressBar)

        // ====== NUEVO: Términos y Condiciones ======
        checkboxTerminos = findViewById(R.id.checkboxTerminos)
        tvTerminos = findViewById(R.id.tvTerminos)
    }

    private fun setupClickListeners() {
        btnRegistrar.setOnClickListener {
            validarYRegistrar()
        }

        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // ====== NUEVO: Click en texto abre diálogo ======
        tvTerminos.setOnClickListener {
            mostrarTerminosYCondiciones()
        }
    }

    private fun setupSpinners() {
        spinnerGenero.adapter = createSpinnerAdapter(resources.getStringArray(R.array.generos).toList())
        spinnerObjetivo.adapter = createSpinnerAdapter(resources.getStringArray(R.array.objetivos).toList())
        spinnerNivel.adapter = createSpinnerAdapter(resources.getStringArray(R.array.niveles).toList())
    }

    private fun createSpinnerAdapter(items: List<String>): android.widget.ArrayAdapter<String> {
        return object : android.widget.ArrayAdapter<String>(
            this,
            R.layout.spinner_item_dialog_white,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(getColor(R.color.text_primary))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(getColor(R.color.text_primary))
                return view
            }
        }.apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_dialog_white)
        }
    }

    /**
     * VALIDAR Y REGISTRAR
     */
    private fun validarYRegistrar() {
        limpiarErrores()

        // Obtener valores
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val passwordConfirm = etPasswordConfirm.text.toString()
        val nombre = etNombre.text.toString().trim()
        val telefonoSinPrefijo = etTelefono.text.toString().trim()
        val pesoStr = etPeso.text.toString().trim()
        val alturaStr = etAltura.text.toString().trim()
        val edadStr = etEdad.text.toString().trim()
        val genero = spinnerGenero.selectedItem.toString()
        val objetivo = spinnerObjetivo.selectedItem.toString()
        val nivel = spinnerNivel.selectedItem.toString()

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

        // VALIDAR TELÉFONO
        if (telefonoSinPrefijo.isBlank()) {
            etTelefono.error = "Teléfono obligatorio"
            hayError = true
        } else if (!validarTelefono(telefonoSinPrefijo)) {
            etTelefono.error = "Teléfono inválido (9 dígitos)"
            hayError = true
        }

        // VALIDAR PESO
        val peso = if (pesoStr.isNotBlank()) {
            try {
                val valor = pesoStr.toDouble()
                if (valor <= 0 || valor > 300) {
                    etPeso.error = "Peso inválido (1-300 kg)"
                    hayError = true
                    0.0
                } else {
                    valor
                }
            } catch (e: NumberFormatException) {
                etPeso.error = "Peso inválido"
                hayError = true
                0.0
            }
        } else {
            0.0
        }

        // VALIDAR ALTURA
        val altura = if (alturaStr.isNotBlank()) {
            try {
                val valor = alturaStr.toDouble()
                if (valor <= 0 || valor > 250) {
                    etAltura.error = "Altura inválida (1-250 cm)"
                    hayError = true
                    0.0
                } else {
                    valor
                }
            } catch (e: NumberFormatException) {
                etAltura.error = "Altura inválida"
                hayError = true
                0.0
            }
        } else {
            0.0
        }

        // VALIDAR EDAD
        val edad = if (edadStr.isNotBlank()) {
            try {
                val valor = edadStr.toInt()
                if (valor < 16 || valor > 100) {
                    etEdad.error = "Edad inválida (16-100 años)"
                    hayError = true
                    0
                } else {
                    valor
                }
            } catch (e: NumberFormatException) {
                etEdad.error = "Edad inválida"
                hayError = true
                0
            }
        } else {
            0
        }

        if (hayError) return

        // ====== NUEVO: Validar términos ======
        if (!validarTerminos()) {
            return
        }

        // Teléfono con prefijo
        val telefonoCompleto = "+34$telefonoSinPrefijo"

        // CREAR CUENTA FIREBASE
        crearCuentaFirebase(
            email, password, nombre, telefonoCompleto,
            peso, altura, edad, genero, objetivo, nivel
        )
    }

    /**
     * CREAR CUENTA FIREBASE AUTH
     */
    private fun crearCuentaFirebase(
        email: String,
        password: String,
        nombre: String,
        telefono: String,
        peso: Double,
        altura: Double,
        edad: Int,
        genero: String,
        objetivo: String,
        nivel: String
    ) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                val userId = user?.uid ?: ""

                // Guardar foto en almacenamiento local del dispositivo (sin Firebase)
                fotoUri?.let { uri ->
                    UserPhotoManager.guardarFotoLocal(this, userId, uri)
                }

                // Enviar email de verificación
                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "📧 Email de verificación enviado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                // Guardar cliente en Firestore
                val fotoUrl = fotoUri?.toString() ?: ""

                viewModel.registerCliente(
                    userId = userId,
                    email = email,
                    nombre = nombre,
                    telefono = telefono,
                    peso = peso,
                    altura = altura,
                    edad = edad,
                    genero = genero,
                    objetivo = objetivo,
                    nivel = nivel,
                    fotoUrl = fotoUrl,
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
                is RegisterClienteUiState.Idle -> {
                    // Nada
                }
                is RegisterClienteUiState.Loading -> {
                    // Ya está en loading
                }
                is RegisterClienteUiState.Success -> {
                    showLoading(false)
                    mostrarExito()
                }
                is RegisterClienteUiState.Error -> {
                    showLoading(false)

                    val mensaje = when {
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
            .setTitle("✅ Registro exitoso")
            .setMessage(
                "Tu cuenta ha sido creada correctamente.\n\n" +

                        "📧 VERIFICA TU EMAIL\n" +
                        "Revisa tu correo (${etEmail.text}) y haz click en el enlace de verificación.\n\n" +

                        "💪 COMIENZA TU ENTRENAMIENTO\n" +
                        "Ya puedes acceder a todas las funcionalidades de la app.\n\n" +

                        "¡Bienvenido a GimnasioPro!"
            )
            .setPositiveButton("Comenzar") { _, _ ->
                // Ir a pantalla de verificación de email
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
        etTelefono.error = null
        etPeso.error = null
        etAltura.error = null
        etEdad.error = null
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
        etTelefono.isEnabled = !show
        etPeso.isEnabled = !show
        etAltura.isEnabled = !show
        etEdad.isEnabled = !show
        spinnerGenero.isEnabled = !show
        spinnerObjetivo.isEnabled = !show
        spinnerNivel.isEnabled = !show
        btnSeleccionarFoto.isEnabled = !show
    }

    // ====== NUEVO: FUNCIONES TÉRMINOS Y CONDICIONES ======

    /**
     * MOSTRAR TÉRMINOS Y CONDICIONES
     */
    private fun mostrarTerminosYCondiciones() {
        try {
            android.util.Log.d("Terminos", "=== INICIANDO CARGA DE TÉRMINOS ===")

            // Verificar que el recurso existe
            val resourceId = R.raw.terminos_condiciones
            android.util.Log.d("Terminos", "Resource ID: $resourceId")

            val inputStream = resources.openRawResource(R.raw.terminos_condiciones)
            android.util.Log.d("Terminos", "InputStream obtenido: ${inputStream != null}")

            val texto = inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("Terminos", "Texto cargado. Longitud: ${texto.length}")
            android.util.Log.d("Terminos", "Primeros 100 caracteres: ${texto.take(100)}")

            if (texto.isBlank()) {
                android.util.Log.e("Terminos", "❌ El texto está VACÍO")
                Toast.makeText(this, "⚠️ El archivo de términos está vacío", Toast.LENGTH_LONG).show()
                return
            }

            val scrollView = android.widget.ScrollView(this)
            val textView = android.widget.TextView(this).apply {
                text = texto
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)  // ← CAMBIADO A NEGRO
                setBackgroundColor(android.graphics.Color.WHITE)  // ← FONDO BLANCO
                setPadding(40, 40, 40, 40)
            }
            scrollView.addView(textView)

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Términos y Condiciones")
                .setView(scrollView)
                .setPositiveButton("Acepto") { _, _ ->
                    checkboxTerminos.isChecked = true
                    solicitarPermisoNotificaciones()
                }
                .setNegativeButton("No acepto") { _, _ ->
                    checkboxTerminos.isChecked = false
                }
                .show()

            android.util.Log.d("Terminos", "✅ Diálogo mostrado correctamente")

        } catch (e: Exception) {
            android.util.Log.e("Terminos", "❌ Error: ${e.message}", e)
            Toast.makeText(this, "Error al cargar términos: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Solicita permiso de notificaciones en Android 13+.
     * En versiones anteriores las notificaciones ya están activas por defecto.
     */
    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * VALIDAR TÉRMINOS
     */
    private fun validarTerminos(): Boolean {
        if (!checkboxTerminos.isChecked) {
            Toast.makeText(
                this,
                "⚠️ Debes aceptar los Términos y Condiciones para continuar",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }
}