package com.example.gimnasiopro

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.TrainerAdapter
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Activity para que los clientes busquen y soliciten conexión con trainers.
 */
class BuscarTrainerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val conexionRepository = ConexionRepository()
    private val notificacionRepository = NotificacionRepository()

    private var currentUserId: String? = null
    private var currentUserNombre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_trainer)

        currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadTrainers()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerTrainers)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        btnBack.setOnClickListener { finish() }
    }

    private fun loadTrainers() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val userId = currentUserId ?: return@launch
            try {
                // Obtener nombre del usuario actual
                val userInfo = UserHelper.getUserInfo(userId)
                currentUserNombre = userInfo?.nombre ?: "Cliente"

                // Obtener lista de trainers disponibles
                val trainers = UserHelper.getTrainersDisponibles()

                progressBar.visibility = View.GONE

                if (trainers.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No hay trainers disponibles en este momento"
                } else {
                    recyclerView.adapter = TrainerAdapter(trainers) { trainer ->
                        mostrarDialogoSolicitud(trainer)
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Error al cargar trainers: ${e.message}"
            }
        }
    }

    private fun mostrarDialogoSolicitud(trainer: UserHelper.UserInfo) {
        val editText = android.widget.EditText(this).apply {
            hint = "Mensaje opcional (máx. 200 caracteres)"
            maxLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(200))
        }

        AlertDialog.Builder(this)
            .setTitle("Solicitar conexión")
            .setMessage("¿Deseas enviar una solicitud de conexión a ${trainer.nombre ?: "este trainer"}?")
            .setView(editText)
            .setPositiveButton("Enviar solicitud") { _, _ ->
                val mensaje = editText.text.toString().trim()
                enviarSolicitud(trainer, mensaje)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarSolicitud(trainer: UserHelper.UserInfo, mensaje: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val userId = currentUserId ?: return@launch
            try {
                // Crear conexión pendiente
                val resultConexion = conexionRepository.solicitarConexion(
                    clienteId = userId,
                    trainerId = trainer.userId,
                    mensaje = mensaje
                )

                if (resultConexion.isSuccess) {
                    val conexionId = resultConexion.getOrNull() ?: ""
                    // Enviar notificación al trainer con el conexionId
                    notificacionRepository.enviarSolicitudConexion(
                        trainerId = trainer.userId,
                        clienteId = currentUserId ?: return@launch,
                        clienteNombre = currentUserNombre ?: "Cliente",
                        mensaje = mensaje,
                        conexionId = conexionId
                    )

                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@BuscarTrainerActivity,
                        "Solicitud enviada a ${trainer.nombre}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@BuscarTrainerActivity,
                        "Error: ${resultConexion.exceptionOrNull()?.message ?: "No se pudo enviar la solicitud"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@BuscarTrainerActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

