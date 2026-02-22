package com.example.gimnasiopro

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.MensajeAdapter
import com.example.gimnasiopro.data.firestore.Mensaje
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity para conversaciones entre trainer y cliente.
 *
 * Características:
 * - Mensajes breves (máx 200 caracteres)
 * - Persistencia semanal (se eliminan después de 7 días)
 * - Aviso de expiración
 */
class ConversacionActivity : AppCompatActivity() {

    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvNombreOtro: TextView
    private lateinit var tvAvisoExpiracion: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout

    private lateinit var adapter: MensajeAdapter
    private val mensajeRepository = MensajeRepository()
    private val auth = FirebaseAuth.getInstance()

    private var currentUserId: String? = null
    private var currentUserNombre: String? = null
    private var otroUsuarioId: String? = null
    private var otroUsuarioNombre: String? = null

    companion object {
        const val MAX_CARACTERES = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversacion)

        currentUserId = auth.currentUser?.uid
        otroUsuarioId = intent.getStringExtra("OTRO_USUARIO_ID")
        otroUsuarioNombre = intent.getStringExtra("OTRO_USUARIO_NOMBRE")

        if (currentUserId == null || otroUsuarioId == null) {
            Toast.makeText(this, "Error al abrir conversación", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadCurrentUserName()
        observeMensajes()
    }

    private fun setupViews() {
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnBack = findViewById(R.id.btnBack)
        tvNombreOtro = findViewById(R.id.tvNombreOtro)
        tvAvisoExpiracion = findViewById(R.id.tvAvisoExpiracion)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        tvNombreOtro.text = otroUsuarioNombre ?: "Conversación"

        btnBack.setOnClickListener { finish() }

        btnEnviar.setOnClickListener {
            enviarMensaje()
        }

        // Limitar caracteres
        etMensaje.filters = arrayOf(android.text.InputFilter.LengthFilter(MAX_CARACTERES))

        // Contador de caracteres
        etMensaje.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val remaining = MAX_CARACTERES - (s?.length ?: 0)
                // Podrías mostrar el contador si lo deseas
            }
        })

        adapter = MensajeAdapter(currentUserId!!)
        recyclerMensajes.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerMensajes.adapter = adapter
    }

    private fun loadCurrentUserName() {
        lifecycleScope.launch {
            val userInfo = UserHelper.getUserInfo(currentUserId!!)
            currentUserNombre = userInfo?.nombre ?: "Usuario"
        }
    }

    private fun observeMensajes() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            mensajeRepository.getMensajesConversacion(currentUserId!!, otroUsuarioId!!)
                .collectLatest { mensajes ->
                    progressBar.visibility = View.GONE

                    if (mensajes.isEmpty()) {
                        recyclerMensajes.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                    } else {
                        recyclerMensajes.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                        adapter.submitList(mensajes)

                        // Scroll al último mensaje
                        recyclerMensajes.scrollToPosition(mensajes.size - 1)
                    }
                }
        }
    }

    private fun enviarMensaje() {
        val texto = etMensaje.text.toString().trim()

        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        if (texto.length > MAX_CARACTERES) {
            Toast.makeText(this, "Máximo $MAX_CARACTERES caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val result = mensajeRepository.enviarMensaje(
                    remitenteId = currentUserId!!,
                    remitenteNombre = currentUserNombre ?: "Usuario",
                    destinatarioId = otroUsuarioId!!,
                    texto = texto
                )

                if (result.isSuccess) {
                    etMensaje.text.clear()
                } else {
                    Toast.makeText(this@ConversacionActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConversacionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

