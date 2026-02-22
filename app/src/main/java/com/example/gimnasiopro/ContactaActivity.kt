package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity principal para gestión de contactos.
 *
 * Para CLIENTES:
 * - Buscar trainer (si no tiene)
 * - Ver trainer actual (si tiene)

 * - Enviar mensaje al trainer
 *
 * Para TRAINERS:
 * - Ver lista de clientes
 * - Gestionar solicitudes
 * - Invitar clientes con código
 */
class ContactaActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var layoutCliente: LinearLayout
    private lateinit var layoutTrainer: LinearLayout
    private lateinit var btnBack: ImageButton

    // Vistas de cliente
    private lateinit var cardBuscarTrainer: LinearLayout
    private lateinit var cardMiTrainer: LinearLayout

    private lateinit var tvNombreTrainer: TextView
    private lateinit var tvEstadoConexion: TextView

    // Vistas de trainer
    private lateinit var cardMisClientes: LinearLayout
    private lateinit var cardInvitarCliente: LinearLayout
    private lateinit var tvNumeroClientes: TextView

    private val auth = FirebaseAuth.getInstance()
    private val conexionRepository = ConexionRepository()

    private var currentUserId: String? = null
    private var currentUserTipo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacta)

        currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadUserTypeAndSetupUI()
    }

    private fun setupViews() {
        progressBar = findViewById(R.id.progressBar)
        layoutCliente = findViewById(R.id.layoutCliente)
        layoutTrainer = findViewById(R.id.layoutTrainer)
        btnBack = findViewById(R.id.btnBack)

        // Vistas cliente
        cardBuscarTrainer = findViewById(R.id.cardBuscarTrainer)
        cardMiTrainer = findViewById(R.id.cardMiTrainer)
        tvNombreTrainer = findViewById(R.id.tvNombreTrainer)
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion)

        // Vistas trainer
        cardMisClientes = findViewById(R.id.cardMisClientes)
        cardInvitarCliente = findViewById(R.id.cardInvitarCliente)
        tvNumeroClientes = findViewById(R.id.tvNumeroClientes)

        btnBack.setOnClickListener { finish() }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // === CLIENTE ===
        cardBuscarTrainer.setOnClickListener {
            // Abrir pantalla de búsqueda de trainers
            val intent = Intent(this, BuscarTrainerActivity::class.java)
            startActivity(intent)
        }

        cardMiTrainer.setOnClickListener {
            // Abrir conversación con el trainer o ver detalles
            abrirConversacionConTrainer()
        }


        // === TRAINER ===
        cardMisClientes.setOnClickListener {
            // Abrir lista de clientes
            val intent = Intent(this, ListaClientesActivity::class.java)
            startActivity(intent)
        }

        cardInvitarCliente.setOnClickListener {
            mostrarDialogoInvitarCliente()
        }
    }

    private fun loadUserTypeAndSetupUI() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val userInfo = UserHelper.getUserInfo(currentUserId!!)
                currentUserTipo = userInfo?.tipo

                progressBar.visibility = View.GONE

                when (currentUserTipo) {
                    "cliente" -> setupClienteUI()
                    "trainer" -> setupTrainerUI()
                    else -> {
                        Toast.makeText(this@ContactaActivity, "No se pudo determinar el tipo de usuario", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ContactaActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClienteUI() {
        layoutCliente.visibility = View.VISIBLE
        layoutTrainer.visibility = View.GONE

        // Verificar si tiene trainer asignado
        lifecycleScope.launch {
            try {
                val conexion = conexionRepository.getTrainerConectado(currentUserId!!).first()

                if (conexion != null) {
                    // Tiene trainer
                    cardBuscarTrainer.visibility = View.GONE
                    cardMiTrainer.visibility = View.VISIBLE

                    // Cargar nombre del trainer
                    val trainerInfo = UserHelper.getUserInfo(conexion.trainerId)
                    tvNombreTrainer.text = trainerInfo?.nombre ?: "Tu Trainer"
                    tvEstadoConexion.text = "Conexión activa"
                } else {
                    // Verificar si tiene solicitud pendiente
                    val solicitud = conexionRepository.getSolicitudPendienteDeCliente(currentUserId!!).first()

                    if (solicitud != null) {
                        cardBuscarTrainer.visibility = View.GONE
                        cardMiTrainer.visibility = View.VISIBLE
                        tvNombreTrainer.text = "Esperando respuesta..."
                        tvEstadoConexion.text = "Solicitud pendiente"
                    } else {
                        // No tiene trainer ni solicitud
                        cardBuscarTrainer.visibility = View.VISIBLE
                        cardMiTrainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                cardBuscarTrainer.visibility = View.VISIBLE
                cardMiTrainer.visibility = View.GONE
            }
        }
    }

    private fun setupTrainerUI() {
        layoutCliente.visibility = View.GONE
        layoutTrainer.visibility = View.VISIBLE

        // Contar clientes conectados
        lifecycleScope.launch {
            try {
                val clientes = conexionRepository.getClientesConectados(currentUserId!!).first()
                tvNumeroClientes.text = "${clientes.size} cliente${if (clientes.size != 1) "s" else ""}"
            } catch (e: Exception) {
                tvNumeroClientes.text = "0 clientes"
            }
        }
    }

    private fun abrirConversacionConTrainer() {
        lifecycleScope.launch {
            try {
                val conexion = conexionRepository.getTrainerConectado(currentUserId!!).first()

                if (conexion != null) {
                    val trainerInfo = UserHelper.getUserInfo(conexion.trainerId)

                    val intent = Intent(this@ContactaActivity, ConversacionActivity::class.java).apply {
                        putExtra("OTRO_USUARIO_ID", conexion.trainerId)
                        putExtra("OTRO_USUARIO_NOMBRE", trainerInfo?.nombre ?: "Trainer")
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ContactaActivity, "No tienes un trainer asignado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContactaActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoInvitarCliente() {
        val editText = android.widget.EditText(this).apply {
            hint = "Email o nº teléfono (sin prefijo)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Invitar Cliente")
            .setMessage("Introduce el email o número de teléfono del cliente.\nSi es teléfono, se añadirá +34 automáticamente:")
            .setView(editText)
            .setPositiveButton("Enviar invitación") { _, _ ->
                val valor = editText.text.toString().trim()
                if (valor.isNotEmpty()) {
                    enviarInvitacion(valor)
                } else {
                    Toast.makeText(this, "Introduce un email o teléfono válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarInvitacion(emailOTelefono: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                var valorBusqueda = emailOTelefono.trim()

                // Si parece un número de teléfono (solo dígitos, sin @), añadir +34 automáticamente
                val soloDigitos = valorBusqueda.replace(" ", "").replace("-", "")
                val esTelefono = !valorBusqueda.contains("@") && soloDigitos.all { it.isDigit() } && soloDigitos.length in 6..15

                if (esTelefono && !valorBusqueda.startsWith("+")) {
                    valorBusqueda = "+34$soloDigitos"
                    Log.d("ContactaActivity", "📱 Prefijo +34 añadido automáticamente: $valorBusqueda")
                }

                Log.d("ContactaActivity", "🔍 Buscando cliente con: '$valorBusqueda' (esTelefono=$esTelefono)")

                // Buscar cliente por email o teléfono (optimizado - una sola búsqueda)
                var clienteId: String? = null

                if (esTelefono) {
                    Log.d("ContactaActivity", "📞 Buscando por TELÉFONO...")
                    clienteId = UserHelper.getUserIdByPhone(valorBusqueda)
                } else {
                    Log.d("ContactaActivity", "📧 Buscando por EMAIL...")
                    clienteId = UserHelper.getUserIdByEmail(valorBusqueda)
                }


                progressBar.visibility = View.GONE

                if (clienteId != null) {
                    Log.d("ContactaActivity", "✅ Usuario encontrado: $clienteId")
                    // Verificar que sea cliente
                    val userInfo = UserHelper.getUserInfo(clienteId)
                    Log.d("ContactaActivity", "📋 Tipo de usuario: ${userInfo?.tipo}, nombre: ${userInfo?.nombre}, email: ${userInfo?.email}, tel: ${userInfo?.telefono}")

                    if (userInfo?.tipo != "cliente") {
                        Toast.makeText(this@ContactaActivity, "El usuario encontrado no es un cliente, es un ${userInfo?.tipo ?: "desconocido"}", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Verificar que no sea uno mismo
                    if (clienteId == currentUserId) {
                        Toast.makeText(this@ContactaActivity, "No puedes invitarte a ti mismo", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Verificar que no tenga ya una conexión activa o pendiente
                    try {
                        val conexionExistente = conexionRepository.getClientesConectados(currentUserId!!).first()
                        val yaConectado = conexionExistente.any { it.clienteId == clienteId }
                        if (yaConectado) {
                            Toast.makeText(this@ContactaActivity, "Ya tienes conexión con este cliente", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        // También verificar pendientes
                        val pendiente = conexionRepository.getConexionPendienteEntreUsuarios(clienteId, currentUserId!!)
                        if (pendiente != null) {
                            Toast.makeText(this@ContactaActivity, "Ya existe una solicitud pendiente con este cliente", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    } catch (_: Exception) { }

                    // Crear conexión pendiente en Firestore
                    val conexionResult = conexionRepository.solicitarConexionComoTrainer(
                        clienteId = clienteId,
                        trainerId = currentUserId!!
                    )

                    val conexionId = conexionResult.getOrNull() ?: ""

                    // Enviar invitación (crear notificación con conexionId)
                    val notificacionRepository = com.example.gimnasiopro.data.firestore.NotificacionRepository()
                    val trainerInfo = UserHelper.getUserInfo(currentUserId!!)
                    val resultado = notificacionRepository.enviarInvitacionTrainer(
                        clienteId = clienteId,
                        trainerId = currentUserId!!,
                        trainerNombre = trainerInfo?.nombre ?: "Trainer",
                        conexionId = conexionId
                    )

                    if (resultado.isSuccess) {
                        Toast.makeText(this@ContactaActivity, "✅ Invitación enviada a ${userInfo.nombre ?: "cliente"}", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ContactaActivity", "❌ Error al crear notificación: ${resultado.exceptionOrNull()?.message}")
                        Toast.makeText(this@ContactaActivity, "Error al enviar invitación: ${resultado.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("ContactaActivity", "⚠️ No se encontró usuario con: $valorBusqueda (original: '$emailOTelefono')")
                    val tipoContacto = if (esTelefono) "teléfono" else "email"
                    Toast.makeText(
                        this@ContactaActivity,
                        "No se encontró ningún cliente con ese $tipoContacto.\n" +
                        "Dato buscado: $valorBusqueda\n" +
                        "Verifica que el cliente esté registrado.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("ContactaActivity", "❌ Error enviando invitación: ${e.message}", e)
                val errorMsg = if (e.message?.contains("PERMISSION_DENIED") == true ||
                    e.message?.contains("Missing or insufficient permissions") == true) {
                    "Error de permisos en Firebase. Verifica las reglas de seguridad."
                } else {
                    "Error: ${e.message}"
                }
                Toast.makeText(this@ContactaActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos
        if (currentUserTipo != null) {
            when (currentUserTipo) {
                "cliente" -> setupClienteUI()
                "trainer" -> setupTrainerUI()
            }
        }
    }
}

