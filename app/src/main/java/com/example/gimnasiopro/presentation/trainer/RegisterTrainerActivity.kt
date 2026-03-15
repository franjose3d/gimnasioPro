package com.example.gimnasiopro.presentation.trainer

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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasiopro.R
import com.example.gimnasiopro.presentation.auth.VerificarEmailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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
    private lateinit var storage: FirebaseStorage
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

    // Views - Certificado
    private lateinit var ivCertificadoIcon: ImageView
    private lateinit var btnSeleccionarCertificado: Button
    private lateinit var tvCertificadoNombre: TextView

    // Views - UI
    private lateinit var btnRegistrar: Button
    private lateinit var progressBar: ProgressBar

    // Foto
    private var fotoUri: Uri? = null

    // Certificado
    private var certificadoUri: Uri? = null

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
        private const val PICK_IMAGE_REQUEST = 1001
        private const val PICK_CERTIFICADO_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_trainer)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
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

        // Certificado
        ivCertificadoIcon = findViewById(R.id.ivCertificadoIcon)
        btnSeleccionarCertificado = findViewById(R.id.btnSeleccionarCertificado)
        tvCertificadoNombre = findViewById(R.id.tvCertificadoNombre)

        // UI
        btnRegistrar = findViewById(R.id.btnregistrar)
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

        btnSeleccionarCertificado.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Seleccionar certificado"), PICK_CERTIFICADO_REQUEST)
        }

        // ====== NUEVO: Click en texto abre diálogo ======
        tvTerminos.setOnClickListener {
            mostrarTerminosYCondiciones()
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

        // ====== NUEVO: Validar términos ======
        if (!validarTerminos()) {
            return
        }

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

                // Subir archivos a Firebase Storage y luego guardar trainer
                subirArchivosYGuardarTrainer(
                    userId = userId,
                    email = email,
                    nombre = nombre,
                    dni = dni,
                    poblacion = poblacion,
                    municipio = municipio,
                    telefono = telefono,
                    sobreMi = sobreMi,
                    tarifa = tarifa
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
     * SUBIR ARCHIVOS A FIREBASE STORAGE Y GUARDAR TRAINER
     */
    private fun subirArchivosYGuardarTrainer(
        userId: String,
        email: String,
        nombre: String,
        dni: String,
        poblacion: String,
        municipio: String,
        telefono: String,
        sobreMi: String,
        tarifa: Double
    ) {
        var fotoUrlFinal = ""
        var certificadoUrlFinal = ""
        var archivosSubidos = 0
        val totalArchivos = listOfNotNull(fotoUri, certificadoUri).size

        // Si no hay archivos para subir, guardar directamente
        if (totalArchivos == 0) {
            guardarTrainerEnFirestore(
                userId, email, nombre, dni, poblacion, municipio,
                telefono, sobreMi, fotoUrlFinal, certificadoUrlFinal, tarifa
            )
            return
        }

        val verificarYGuardar = {
            archivosSubidos++
            if (archivosSubidos >= totalArchivos) {
                guardarTrainerEnFirestore(
                    userId, email, nombre, dni, poblacion, municipio,
                    telefono, sobreMi, fotoUrlFinal, certificadoUrlFinal, tarifa
                )
            }
        }

        // Subir foto de perfil
        fotoUri?.let { uri ->
            val fotoRef = storage.reference.child("trainers/$userId/foto_perfil_${UUID.randomUUID()}")
            fotoRef.putFile(uri)
                .addOnSuccessListener {
                    fotoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        fotoUrlFinal = downloadUrl.toString()
                        verificarYGuardar()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "⚠️ No se pudo subir la foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    verificarYGuardar()
                }
        }

        // Subir certificado
        certificadoUri?.let { uri ->
            val extension = obtenerExtensionArchivo(uri)
            val certificadoRef = storage.reference.child("trainers/$userId/certificado_${UUID.randomUUID()}.$extension")
            certificadoRef.putFile(uri)
                .addOnSuccessListener {
                    certificadoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        certificadoUrlFinal = downloadUrl.toString()
                        verificarYGuardar()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "⚠️ No se pudo subir el certificado: ${e.message}", Toast.LENGTH_SHORT).show()
                    verificarYGuardar()
                }
        }
    }

    /**
     * GUARDAR TRAINER EN FIRESTORE
     */
    private fun guardarTrainerEnFirestore(
        userId: String,
        email: String,
        nombre: String,
        dni: String,
        poblacion: String,
        municipio: String,
        telefono: String,
        sobreMi: String,
        fotoUrl: String,
        certificadoUrl: String,
        tarifa: Double
    ) {
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
            certificadoUrl = certificadoUrl,
            tarifa = tarifa,
            emailVerificado = false
        )
    }

    /**
     * Obtener extensión del archivo
     */
    private fun obtenerExtensionArchivo(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when {
            mimeType?.contains("pdf") == true -> "pdf"
            mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
            mimeType?.contains("png") == true -> "png"
            else -> "file"
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
            .setTitle("✅ Registro exitoso")
            .setMessage(
                "Tu cuenta ha sido creada correctamente.\n\n" +

                        "📧 VERIFICA TU EMAIL\n" +
                        "Revisa tu correo (${etEmail.text}) y haz click en el enlace de verificación.\n\n" +

                        "✅ ACCESO COMPLETO\n" +
                        "Ya puedes usar todas las funcionalidades de la aplicación.\n\n" +

                        "⚠️ IMPORTANTE\n" +
                        "Tu perfil y documentación serán revisados. Tu cuenta puede ser anulada si se detecta falsedad o incumplimiento de la legalidad en los documentos presentados.\n\n" +

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
     * RESULTADO SELECCIONAR FOTO O CERTIFICADO
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        fotoUri = uri
                        ivFotoPerfil.setImageURI(uri)
                        Toast.makeText(this, "✅ Foto seleccionada", Toast.LENGTH_SHORT).show()
                    }
                }
                PICK_CERTIFICADO_REQUEST -> {
                    data?.data?.let { uri ->
                        certificadoUri = uri
                        // Obtener nombre del archivo
                        val nombreArchivo = obtenerNombreArchivo(uri)
                        tvCertificadoNombre.text = "📄 $nombreArchivo"
                        ivCertificadoIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                        Toast.makeText(this, "✅ Certificado seleccionado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Obtener nombre del archivo desde URI
     */
    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "Archivo seleccionado"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nombreIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nombreIndex >= 0) {
                    nombre = cursor.getString(nombreIndex)
                }
            }
        }
        return nombre
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