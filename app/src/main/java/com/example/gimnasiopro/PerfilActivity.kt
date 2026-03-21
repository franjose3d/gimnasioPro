package com.example.gimnasiopro

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.utils.UserPhotoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Activity para ver y editar el perfil del usuario.
 *
 * Funcionalidades:
 * - Ver datos del perfil (trainer o cliente)
 * - Editar campos (excepto email)
 * - Eliminar cuenta completamente
 */
class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Foto de perfil
    private lateinit var ivFotoPerfil: ImageView
    private var cameraFotoUri: Uri? = null

    // Launcher galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { guardarFotoSeleccionada(it) } }

    // Launcher cámara
    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraFotoUri?.let { guardarFotoSeleccionada(it) } }

    // Launcher permiso cámara
    private val permisosCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    // Views comunes
    private lateinit var tvEmail: TextView
    private lateinit var tvTipoUsuario: TextView
    private lateinit var etNombre: EditText
    private lateinit var etTelefono: EditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnEliminarCuenta: Button

    // Views de Trainer
    private lateinit var layoutTrainerFields: LinearLayout
    private lateinit var etDni: EditText
    private lateinit var etPoblacion: EditText
    private lateinit var etMunicipio: EditText
    private lateinit var etSobreMi: EditText
    private lateinit var etTarifa: EditText

    // Views de Cliente
    private lateinit var layoutClienteFields: LinearLayout
    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText
    private lateinit var etEdad: EditText
    private lateinit var spinnerObjetivo: Spinner
    private lateinit var spinnerNivel: Spinner

    private var tipoUsuario: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        userId = currentUser.uid

        setupViews()
        cargarDatosPerfil()
    }

    private fun setupViews() {
        // Header
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Foto de perfil
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        UserPhotoManager.cargarFoto(this, userId, ivFotoPerfil)
        ivFotoPerfil.setOnLongClickListener {
            mostrarOpcionesFoto()
            true
        }

        // Views comunes
        tvEmail = findViewById(R.id.tvEmail)
        tvTipoUsuario = findViewById(R.id.tvTipoUsuario)
        etNombre = findViewById(R.id.etNombre)
        etTelefono = findViewById(R.id.etTelefono)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)

        // Views Trainer
        layoutTrainerFields = findViewById(R.id.layoutTrainerFields)
        etDni = findViewById(R.id.etDni)
        etPoblacion = findViewById(R.id.etPoblacion)
        etMunicipio = findViewById(R.id.etMunicipio)
        etSobreMi = findViewById(R.id.etSobreMi)
        etTarifa = findViewById(R.id.etTarifa)

        // Views Cliente
        layoutClienteFields = findViewById(R.id.layoutClienteFields)
        etPeso = findViewById(R.id.etPeso)
        etAltura = findViewById(R.id.etAltura)
        etEdad = findViewById(R.id.etEdad)
        spinnerObjetivo = findViewById(R.id.spinnerObjetivo)
        spinnerNivel = findViewById(R.id.spinnerNivel)

        // Configurar spinners
        configurarSpinners()

        // Botones
        btnGuardarCambios.setOnClickListener { guardarCambios() }
        btnEliminarCuenta.setOnClickListener { confirmarEliminarCuenta() }
    }

    private fun configurarSpinners() {
        // Objetivos
        val objetivos = listOf("Pérdida de peso", "Ganancia muscular", "Mantenimiento", "Tonificación", "Resistencia")
        val adapterObjetivo = ArrayAdapter(this, android.R.layout.simple_spinner_item, objetivos)
        adapterObjetivo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerObjetivo.adapter = adapterObjetivo

        // Niveles
        val niveles = listOf("Principiante", "Intermedio", "Avanzado")
        val adapterNivel = ArrayAdapter(this, android.R.layout.simple_spinner_item, niveles)
        adapterNivel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNivel.adapter = adapterNivel
    }

    private fun cargarDatosPerfil() {
        // Mostrar email
        tvEmail.text = auth.currentUser?.email ?: ""

        // Buscar el tipo de usuario usando UserHelper (busca en clientes/trainers)
        lifecycleScope.launch {
            try {
                val userInfo = UserHelper.getUserInfo(userId)
                if (userInfo != null) {
                    tipoUsuario = userInfo.tipo
                    runOnUiThread {
                        tvTipoUsuario.text = tipoUsuario.uppercase()

                        // Cargar datos comunes desde el documento encontrado
                        etNombre.setText(userInfo.nombre ?: "")
                        etTelefono.setText(userInfo.documento?.getString("telefono") ?: "")

                        // Mostrar campos específicos según el tipo
                        when (tipoUsuario) {
                            "trainer" -> cargarDatosTrainer()
                            "cliente" -> cargarDatosCliente()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@PerfilActivity, "No se encontró el perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PerfilActivity, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarDatosTrainer() {
        layoutTrainerFields.visibility = View.VISIBLE
        layoutClienteFields.visibility = View.GONE

        firestore.collection("trainers").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etDni.setText(document.getString("dni") ?: "")
                    etPoblacion.setText(document.getString("poblacion") ?: "")
                    etMunicipio.setText(document.getString("municipio") ?: "")
                    etSobreMi.setText(document.getString("sobreMi") ?: "")
                    val tarifa = document.getDouble("tarifa") ?: 0.0
                    if (tarifa > 0) {
                        etTarifa.setText(tarifa.toString())
                    }
                }
            }
    }

    private fun cargarDatosCliente() {
        layoutTrainerFields.visibility = View.GONE
        layoutClienteFields.visibility = View.VISIBLE

        firestore.collection("clientes").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val peso = document.getDouble("peso") ?: 0.0
                    val altura = document.getDouble("altura") ?: 0.0
                    val edad = document.getLong("edad")?.toInt() ?: 0

                    if (peso > 0) etPeso.setText(peso.toString())
                    if (altura > 0) etAltura.setText(altura.toString())
                    if (edad > 0) etEdad.setText(edad.toString())

                    // Seleccionar objetivo en spinner
                    val objetivo = document.getString("objetivo") ?: ""
                    val objetivos = listOf("Pérdida de peso", "Ganancia muscular", "Mantenimiento", "Tonificación", "Resistencia")
                    val indexObjetivo = objetivos.indexOf(objetivo)
                    if (indexObjetivo >= 0) spinnerObjetivo.setSelection(indexObjetivo)

                    // Seleccionar nivel en spinner
                    val nivel = document.getString("nivel") ?: ""
                    val niveles = listOf("Principiante", "Intermedio", "Avanzado")
                    val indexNivel = niveles.indexOf(nivel)
                    if (indexNivel >= 0) spinnerNivel.setSelection(indexNivel)
                }
            }
    }

    private fun guardarCambios() {
        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarCambios.isEnabled = false
        btnGuardarCambios.text = "Guardando..."

        // Actualizar datos directamente en la colección correspondiente (clientes/trainers)
        when (tipoUsuario) {
            "trainer" -> guardarDatosTrainer(nombre, telefono)
            "cliente" -> guardarDatosCliente(nombre, telefono)
            else -> {
                finalizarGuardado(false)
            }
        }
    }

    private fun guardarDatosTrainer(nombre: String, telefono: String) {
        val telefonoNorm = telefono.replace(Regex("[^0-9]"), "").let { digitos ->
            if (digitos.startsWith("34") && digitos.length > 9) digitos.removePrefix("34") else digitos
        }
        val datosTrainer = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "telefonoNormalizado" to telefonoNorm,
            "dni" to etDni.text.toString().trim(),
            "poblacion" to etPoblacion.text.toString().trim(),
            "municipio" to etMunicipio.text.toString().trim(),
            "sobreMi" to etSobreMi.text.toString().trim(),
            "tarifa" to (etTarifa.text.toString().toDoubleOrNull() ?: 0.0)
        )

        firestore.collection("trainers").document(userId)
            .update(datosTrainer)
            .addOnSuccessListener { finalizarGuardado(true) }
            .addOnFailureListener { finalizarGuardado(false) }
    }

    private fun guardarDatosCliente(nombre: String, telefono: String) {
        val telefonoNorm = telefono.replace(Regex("[^0-9]"), "").let { digitos ->
            if (digitos.startsWith("34") && digitos.length > 9) digitos.removePrefix("34") else digitos
        }
        val datosCliente = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "telefonoNormalizado" to telefonoNorm,
            "peso" to (etPeso.text.toString().toDoubleOrNull() ?: 0.0),
            "altura" to (etAltura.text.toString().toDoubleOrNull() ?: 0.0),
            "edad" to (etEdad.text.toString().toIntOrNull() ?: 0),
            "objetivo" to spinnerObjetivo.selectedItem.toString(),
            "nivel" to spinnerNivel.selectedItem.toString()
        )

        firestore.collection("clientes").document(userId)
            .update(datosCliente)
            .addOnSuccessListener { finalizarGuardado(true) }
            .addOnFailureListener { finalizarGuardado(false) }
    }

    private fun finalizarGuardado(exito: Boolean) {
        btnGuardarCambios.isEnabled = true
        btnGuardarCambios.text = "GUARDAR CAMBIOS"

        if (exito) {
            Toast.makeText(this, "✅ Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Error al guardar cambios", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar cuenta")
            .setMessage("¿Estás seguro de que quieres eliminar tu cuenta?\n\nEsta acción es IRREVERSIBLE y se eliminarán:\n• Tu perfil\n• Tus datos de entrenamiento\n• Tu cuenta de acceso")
            .setPositiveButton("ELIMINAR") { _, _ ->
                confirmarSegundaVez()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarSegundaVez() {
        AlertDialog.Builder(this)
            .setTitle("🚨 Confirmación final")
            .setMessage("Escribe ELIMINAR para confirmar que quieres borrar tu cuenta permanentemente.")
            .setPositiveButton("Sí, eliminar mi cuenta") { _, _ ->
                eliminarCuenta()
            }
            .setNegativeButton("No, cancelar", null)
            .show()
    }

    // ====== GESTIÓN DE FOTO DE PERFIL ======

    private fun mostrarOpcionesFoto() {
        val prefs = getSharedPreferences("user_photo_prefs", MODE_PRIVATE)
        val tieneFoto = prefs.getString("foto_local_$userId", null) != null ||
                prefs.getString("foto_url_$userId", null) != null

        val opciones = if (tieneFoto) {
            arrayOf("Galería", "Cámara", "Eliminar foto")
        } else {
            arrayOf("Galería", "Cámara")
        }

        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(opciones) { _, which ->
                when (opciones[which]) {
                    "Galería" -> galeriaLauncher.launch("image/*")
                    "Cámara"  -> verificarPermisosCamara()
                    "Eliminar foto" -> confirmarEliminarFoto()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verificarPermisosCamara() {
        when {
            checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de cámara")
                    .setMessage("Necesitamos acceso a la cámara para tomar tu foto de perfil.")
                    .setPositiveButton("Conceder") { _, _ ->
                        permisosCamaraLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> permisosCamaraLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val fotoFile = File.createTempFile("foto_perfil_", ".jpg", cacheDir)
        cameraFotoUri = FileProvider.getUriForFile(
            this,
            "com.mmdevs.gimnasiopro.fileprovider",
            fotoFile
        )
        camaraLauncher.launch(cameraFotoUri!!)
    }

    private fun guardarFotoSeleccionada(uri: Uri) {
        UserPhotoManager.guardarFotoLocal(this, userId, uri)
        UserPhotoManager.cargarFoto(this, userId, ivFotoPerfil)
        Toast.makeText(this, "✅ Foto actualizada", Toast.LENGTH_SHORT).show()
    }

    private fun confirmarEliminarFoto() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar foto")
            .setMessage("¿Eliminar tu foto de perfil?")
            .setPositiveButton("Eliminar") { _, _ ->
                val prefs = getSharedPreferences("user_photo_prefs", MODE_PRIVATE)
                val localPath = prefs.getString("foto_local_$userId", null)
                if (localPath != null) File(localPath).delete()
                prefs.edit()
                    .remove("foto_local_$userId")
                    .remove("foto_url_$userId")
                    .apply()
                ivFotoPerfil.setImageResource(R.drawable.ic_user_placeholder)
                Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuenta() {
        btnEliminarCuenta.isEnabled = false
        btnEliminarCuenta.text = "Eliminando..."

        lifecycleScope.launch {
            try {
                // PASO 1: Eliminar de Firestore primero
                when (tipoUsuario) {
                    "trainer" -> {
                        firestore.collection("trainers").document(userId).delete().await()
                        android.util.Log.d("PerfilActivity", "✅ Trainer eliminado de Firestore")
                    }
                    "cliente" -> {
                        firestore.collection("clientes").document(userId).delete().await()
                        android.util.Log.d("PerfilActivity", "✅ Cliente eliminado de Firestore")
                    }
                }

                // PASO 2: Intentar eliminar de Firebase Authentication
                try {
                    auth.currentUser?.delete()?.await()

                    // ✅ ÉXITO TOTAL - Eliminado de Firestore Y Authentication
                    runOnUiThread {
                        Toast.makeText(
                            this@PerfilActivity,
                            "✅ Cuenta eliminada completamente",
                            Toast.LENGTH_LONG
                        ).show()

                        // Volver a MainActivity (pantalla de login)
                        val intent = Intent(this@PerfilActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }

                } catch (authError: Exception) {
                    android.util.Log.w("PerfilActivity", "⚠️ No se pudo eliminar de Auth: ${authError.message}")

                    // ⚠️ ÉXITO PARCIAL - Eliminado de Firestore pero NO de Authentication
                    runOnUiThread {
                        btnEliminarCuenta.isEnabled = true
                        btnEliminarCuenta.text = "ELIMINAR MI CUENTA"

                        // Verificar si es error de reautenticación
                        if (authError.message?.contains("requires-recent-login") == true ||
                            authError.message?.contains("recent") == true) {

                            androidx.appcompat.app.AlertDialog.Builder(this@PerfilActivity)
                                .setTitle("Reautenticación necesaria")
                                .setMessage(
                                    "✅ Tus datos se han eliminado de la base de datos.\n\n" +
                                            "⚠️ Para eliminar completamente tu cuenta de autenticación:\n\n" +
                                            "1. Cierra sesión\n" +
                                            "2. Vuelve a iniciar sesión\n" +
                                            "3. Ve a Perfil → Eliminar cuenta de nuevo\n\n" +
                                            "Esto es una medida de seguridad de Firebase.\n\n" +
                                            "IMPORTANTE: Tu perfil de trainer/cliente YA fue eliminado. " +
                                            "Solo queda limpiar la cuenta de email."
                                )
                                .setPositiveButton("Cerrar sesión ahora") { _, _ ->
                                    auth.signOut()

                                    // Limpiar badge de notificaciones
                                    com.example.gimnasiopro.utils.NotificationBadgeManager.limpiarTodo(this@PerfilActivity)

                                    val intent = Intent(this@PerfilActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .setNegativeButton("Continuar sin cerrar sesión", null)
                                .show()
                        } else {
                            // Error diferente
                            Toast.makeText(
                                this@PerfilActivity,
                                "⚠️ Datos eliminados. Error de autenticación: ${authError.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } catch (firestoreError: Exception) {
                // ❌ ERROR TOTAL - No se pudo eliminar de Firestore
                android.util.Log.e("PerfilActivity", "❌ Error eliminando de Firestore: ${firestoreError.message}")

                runOnUiThread {
                    btnEliminarCuenta.isEnabled = true
                    btnEliminarCuenta.text = "ELIMINAR MI CUENTA"

                    Toast.makeText(
                        this@PerfilActivity,
                        "❌ Error al eliminar cuenta: ${firestoreError.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}

