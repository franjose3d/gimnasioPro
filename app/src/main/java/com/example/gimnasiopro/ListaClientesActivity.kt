package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.ClienteAdapter
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.ConexionTrainerCliente
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity para que los trainers vean y gestionen su lista de clientes.
 *
 * Funcionalidades:
 * - Ver lista de clientes conectados
 * - Acceder al perfil/rutinas del cliente
 * - Finalizar conexión con cliente
 * - Enviar mensaje a cliente
 */
class ListaClientesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val conexionRepository = ConexionRepository()

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_clientes)

        currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
    }

    override fun onResume() {
        super.onResume()
        loadClientes()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerClientes)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        btnBack.setOnClickListener { finish() }
    }

    private fun loadClientes() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val conexiones = conexionRepository.getClientesConectados(currentUserId!!).first()

                // Cargar información de cada cliente
                val clientesConInfo = conexiones.mapNotNull { conexion ->
                    val clienteInfo = UserHelper.getUserInfo(conexion.clienteId)
                    if (clienteInfo != null) {
                        ClienteConConexion(clienteInfo, conexion)
                    } else null
                }

                progressBar.visibility = View.GONE

                if (clientesConInfo.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No tienes clientes conectados aún.\n\nPuedes invitar clientes desde la pantalla anterior."
                } else {
                    recyclerView.adapter = ClienteAdapter(
                        clientes = clientesConInfo,
                        onClienteClick = { clienteConConexion ->
                            mostrarOpcionesCliente(clienteConConexion)
                        }
                    )
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Error al cargar clientes: ${e.message}"
            }
        }
    }

    private fun mostrarOpcionesCliente(clienteConConexion: ClienteConConexion) {
        val opciones = arrayOf(
            "📋 Ver/Editar Rutinas",
            "📊 Ver Progreso",
            "💬 Enviar Mensaje",
            "❌ Finalizar Conexión"
        )

        AlertDialog.Builder(this)
            .setTitle(clienteConConexion.cliente.nombre ?: "Cliente")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirRutinasCliente(clienteConConexion)
                    1 -> abrirProgresoCliente(clienteConConexion)
                    2 -> abrirConversacion(clienteConConexion)
                    3 -> confirmarFinalizarConexion(clienteConConexion)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirRutinasCliente(clienteConConexion: ClienteConConexion) {
        // Abrir rutinas del cliente (trainer gestiona rutinas del cliente directamente en Firestore)
        val intent = Intent(this, RutinasActivity::class.java).apply {
            putExtra(RutinasActivity.EXTRA_MODO_TRAINER, true)
            putExtra(RutinasActivity.EXTRA_CLIENTE_ID, clienteConConexion.cliente.userId)
            putExtra(RutinasActivity.EXTRA_CLIENTE_NOMBRE, clienteConConexion.cliente.nombre)
        }
        startActivity(intent)
    }

    private fun abrirProgresoCliente(clienteConConexion: ClienteConConexion) {
        // Abrir progreso del cliente (trainer ve estadísticas del cliente)
        val intent = Intent(this, ProgresoActivity::class.java).apply {
            putExtra("MODO_TRAINER", true)
            putExtra("CLIENTE_ID", clienteConConexion.cliente.userId)
            putExtra("CLIENTE_NOMBRE", clienteConConexion.cliente.nombre)
        }
        startActivity(intent)
    }

    private fun abrirConversacion(clienteConConexion: ClienteConConexion) {
        val intent = Intent(this, ConversacionActivity::class.java).apply {
            putExtra("OTRO_USUARIO_ID", clienteConConexion.cliente.userId)
            putExtra("OTRO_USUARIO_NOMBRE", clienteConConexion.cliente.nombre ?: "Cliente")
        }
        startActivity(intent)
    }

    private fun confirmarFinalizarConexion(clienteConConexion: ClienteConConexion) {
        AlertDialog.Builder(this)
            .setTitle("Finalizar conexión")
            .setMessage("¿Estás seguro de que quieres finalizar la conexión con ${clienteConConexion.cliente.nombre}?\n\nYa no podrás editar sus rutinas.")
            .setPositiveButton("Finalizar") { _, _ ->
                finalizarConexion(clienteConConexion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarConexion(clienteConConexion: ClienteConConexion) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val conexionId = clienteConConexion.conexion.id
                if (conexionId != null) {
                    val result = conexionRepository.finalizarConexion(conexionId)
                    if (result.isSuccess) {
                        Toast.makeText(
                            this@ListaClientesActivity,
                            "Conexión finalizada",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadClientes() // Recargar lista
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ListaClientesActivity,
                            "Error: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ListaClientesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Clase que combina información del cliente con su conexión
     */
    data class ClienteConConexion(
        val cliente: UserHelper.UserInfo,
        val conexion: ConexionTrainerCliente
    )
}

