package com.example.gimnasiopro.presentation.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.data.repository.ClienteRepository
import com.example.gimnasiopro.data.repository.TrainerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity que muestra instrucciones para verificar email.
 *
 * Comprueba cada 3 segundos si el email ha sido verificado.
 * Cuando se verifica, permite continuar.
 */
class VerificarEmailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvMensaje: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnReenviar: Button
    private lateinit var btnContinuar: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var verificando = false
    
    // Repositorios
    private val clienteRepository = ClienteRepository()
    private val trainerRepository = TrainerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verificar_email)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()

        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        tvEmail.text = user.email

        // Comprobar si ya está verificado
        if (user.isEmailVerified) {
            mostrarVerificado()
            actualizarEmailVerificadoEnFirestore(user.uid)
        } else {
            iniciarVerificacionAutomatica()
        }
    }

    private fun initViews() {
        tvMensaje = findViewById(R.id.tvMensaje)
        tvEmail = findViewById(R.id.tvEmail)
        btnReenviar = findViewById(R.id.btnReenviar)
        btnContinuar = findViewById(R.id.btnContinuar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        progressBar = findViewById(R.id.progressBar)
        tvEstado = findViewById(R.id.tvEstado)
    }

    private fun setupClickListeners() {
        btnReenviar.setOnClickListener {
            reenviarEmailVerificacion()
        }

        btnContinuar.setOnClickListener {
            verificarManualmente()
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            finish()
        }
    }

    /**
     * Verificar automáticamente cada 3 segundos
     */
    private fun iniciarVerificacionAutomatica() {
        verificando = true
        tvEstado.text = "🔄 Comprobando verificación..."
        verificarEstado()
    }

    private fun verificarEstado() {
        if (!verificando) return

        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    // ✅ Email verificado
                    verificando = false
                    mostrarVerificado()
                    // ACTUALIZAR EN FIRESTORE
                    actualizarEmailVerificadoEnFirestore(user.uid)
                } else {
                    // ⏳ Aún no verificado, reintentar en 3 segundos
                    tvEstado.text = "⏳ Esperando verificación..."
                    handler.postDelayed({ verificarEstado() }, 3000)
                }
            } else {
                // Error al recargar
                tvEstado.text = "❌ Error al verificar"
                handler.postDelayed({ verificarEstado() }, 5000)
            }
        }
    }

    /**
     * Verificar manualmente (cuando el usuario hace click)
     */
    private fun verificarManualmente() {
        progressBar.visibility = View.VISIBLE
        btnContinuar.isEnabled = false
        tvEstado.text = "🔄 Verificando..."

        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            btnContinuar.isEnabled = true

            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    // ✅ Email verificado
                    mostrarVerificado()
                    // ACTUALIZAR EN FIRESTORE
                    actualizarEmailVerificadoEnFirestore(user.uid)

                    // Ir a MainActivity o donde corresponda
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                } else {
                    // ❌ Aún no verificado
                    tvEstado.text = "❌ Email aún no verificado"
                    Toast.makeText(
                        this,
                        "Por favor, verifica tu email antes de continuar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                tvEstado.text = "❌ Error al verificar"
                Toast.makeText(
                    this,
                    "Error: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * ACTUALIZAR EMAIL VERIFICADO EN FIRESTORE
     * Determina si es cliente o trainer y actualiza el estado correspondiente
     */
    private fun actualizarEmailVerificadoEnFirestore(userId: String) {
        lifecycleScope.launch {
            try {
                // Usar UserHelper para determinar el tipo de usuario
                val userInfo = UserHelper.getUserInfo(userId)

                when (userInfo?.tipo) {
                    "cliente" -> {
                        val result = clienteRepository.updateEmailVerified(userId, true)
                        if (result.isSuccess) {
                            // Actualizado correctamente
                        } else {
                            Toast.makeText(
                                this@VerificarEmailActivity,
                                "⚠️ Email verificado pero error al actualizar base de datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    "trainer" -> {
                        val result = trainerRepository.updateEmailVerified(userId, true)
                        if (result.isSuccess) {
                            // Actualizado correctamente
                        } else {
                            Toast.makeText(
                                this@VerificarEmailActivity,
                                "⚠️ Email verificado pero error al actualizar base de datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else -> {
                        // Tipo desconocido, intentar actualizar en ambas colecciones
                        clienteRepository.updateEmailVerified(userId, true)
                        trainerRepository.updateEmailVerified(userId, true)
                    }
                }
            } catch (e: Exception) {
                // Error al actualizar, pero el email ya está verificado en Auth
                Toast.makeText(
                    this@VerificarEmailActivity,
                    "⚠️ Email verificado pero error al actualizar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Reenviar email de verificación
     */
    private fun reenviarEmailVerificacion() {
        progressBar.visibility = View.VISIBLE
        btnReenviar.isEnabled = false
        tvEstado.text = "📧 Enviando email..."

        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnReenviar.isEnabled = true
                tvEstado.text = "✅ Email reenviado"

                Toast.makeText(
                    this,
                    "✅ Email de verificación reenviado a ${user.email}",
                    Toast.LENGTH_LONG
                ).show()

                // Reactivar verificación automática
                handler.postDelayed({ iniciarVerificacionAutomatica() }, 3000)
            }
            ?.addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnReenviar.isEnabled = true
                tvEstado.text = "❌ Error al enviar"

                Toast.makeText(
                    this,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Mostrar que el email está verificado
     */
    private fun mostrarVerificado() {
        verificando = false

        tvMensaje.text = "✅ Email verificado correctamente"
        tvEstado.text = "✅ Verificado"

        btnReenviar.visibility = View.GONE
        btnContinuar.text = "CONTINUAR"
        btnContinuar.visibility = View.VISIBLE

        Toast.makeText(
            this,
            "✅ Email verificado. Tu perfil será revisado en 24-48h",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        verificando = false
        handler.removeCallbacksAndMessages(null)
    }
}
