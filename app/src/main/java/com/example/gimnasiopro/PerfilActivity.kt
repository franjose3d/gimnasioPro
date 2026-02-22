package com.example.gimnasiopro

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gimnasiopro.data.firestore.UserHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private fun eliminarCuenta() {
        btnEliminarCuenta.isEnabled = false
        btnEliminarCuenta.text = "Eliminando..."

        lifecycleScope.launch {
            try {
                // Eliminar de la colección específica (trainers o clientes)
                when (tipoUsuario) {
                    "trainer" -> {
                        firestore.collection("trainers").document(userId).delete().await()
                    }
                    "cliente" -> {
                        firestore.collection("clientes").document(userId).delete().await()
                    }
                }

                // Eliminar la cuenta de Firebase Auth
                auth.currentUser?.delete()?.await()

                runOnUiThread {
                    Toast.makeText(this@PerfilActivity, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()
                    // Volver a MainActivity
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnEliminarCuenta.isEnabled = true
                    btnEliminarCuenta.text = "ELIMINAR MI CUENTA"

                    // Si falla por reautenticación necesaria
                    if (e.message?.contains("recent") == true) {
                        AlertDialog.Builder(this@PerfilActivity)
                            .setTitle("Reautenticación necesaria")
                            .setMessage("Por seguridad, necesitas cerrar sesión e iniciar sesión de nuevo antes de eliminar tu cuenta.")
                            .setPositiveButton("Cerrar sesión") { _, _ ->
                                auth.signOut()
                                finish()
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        Toast.makeText(this@PerfilActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

