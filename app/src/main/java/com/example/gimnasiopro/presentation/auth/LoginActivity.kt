package com.example.gimnasiopro.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gimnasiopro.MainActivity
import com.example.gimnasiopro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity de Login.
 *
 * FUNCIONALIDADES:
 * - Login con email y contraseña
 * - Recuperar contraseña por email
 * - Detectar tipo de usuario (trainer/cliente)
 * - Navegar a pantalla correcta
 * - Validaciones completas
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRecuperarPassword: TextView
    private lateinit var btnRegistrarse: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()

        // Si ya está logueado, ir directo a MainActivity
        checkIfAlreadyLoggedIn()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRecuperarPassword = findViewById(R.id.btnRecuperarPassword)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        // Login
        btnLogin.setOnClickListener {
            validarYLogin()
        }

        // Recuperar contraseña
        btnRecuperarPassword.setOnClickListener {
            mostrarDialogoRecuperarPassword()
        }

        // Ir a registro
        btnRegistrarse.setOnClickListener {
            finish() // Volver a PersonalTrainerActivity
        }
    }

    /**
     * Verificar si ya hay sesión iniciada
     */
    private fun checkIfAlreadyLoggedIn() {
        val user = auth.currentUser
        if (user != null) {
            // Ya está logueado, ir a MainActivity
            irAMainActivity()
        }
    }

    /**
     * VALIDAR Y HACER LOGIN
     */
    private fun validarYLogin() {
        limpiarErrores()

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        var hayError = false

        // Validar email
        if (email.isBlank()) {
            etEmail.error = "Email obligatorio"
            hayError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            hayError = true
        }

        // Validar password
        if (password.isBlank()) {
            etPassword.error = "Contraseña obligatoria"
            hayError = true
        }

        if (hayError) return

        // Hacer login
        hacerLogin(email, password)
    }

    /**
     * LOGIN CON FIREBASE AUTH
     */
    private fun hacerLogin(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                if (user != null) {
                    // Verificar si el email está verificado
                    if (!user.isEmailVerified) {
                        showLoading(false)
                        mostrarEmailNoVerificado(user.email ?: "")
                        return@addOnSuccessListener
                    }

                    // Login exitoso, ir a MainActivity
                    Toast.makeText(
                        this,
                        "✅ Bienvenido ${user.email}",
                        Toast.LENGTH_SHORT
                    ).show()

                    irAMainActivity()
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "❌ Error al obtener usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)

                val mensaje = when {
                    e.message?.contains("no user record") == true ||
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                        "Email o contraseña incorrectos"

                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"

                    e.message?.contains("too many requests") == true ->
                        "Demasiados intentos. Espera un momento"

                    else ->
                        "Error: ${e.message}"
                }

                Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * MOSTRAR DIÁLOGO - Email no verificado
     */
    private fun mostrarEmailNoVerificado(email: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Email no verificado")
            .setMessage(
                "Debes verificar tu email antes de iniciar sesión.\n\n" +
                        "Revisa tu correo ($email) y haz click en el enlace de verificación.\n\n" +
                        "¿Quieres que te reenviemos el email?"
            )
            .setPositiveButton("Reenviar email") { _, _ ->
                reenviarEmailVerificacion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * REENVIAR EMAIL DE VERIFICACIÓN
     */
    private fun reenviarEmailVerificacion() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "❌ Error: No hay usuario",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        showLoading(true)

        user.sendEmailVerification()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    "✅ Email reenviado a ${user.email}",
                    Toast.LENGTH_LONG
                ).show()

                // Cerrar sesión para que no intente entrar sin verificar
                auth.signOut()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "❌ Error al reenviar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * MOSTRAR DIÁLOGO - Recuperar contraseña
     */
    private fun mostrarDialogoRecuperarPassword() {
        val emailActual = etEmail.text.toString().trim()

        // Crear campo de email
        val input = EditText(this)
        input.hint = "Email"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        if (emailActual.isNotBlank()) {
            input.setText(emailActual)
        }

        // Padding para el EditText
        val padding = 50
        input.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("🔑 Recuperar contraseña")
            .setMessage("Ingresa tu email y te enviaremos un enlace para restablecer tu contraseña.")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                enviarEmailRecuperacion(email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * ENVIAR EMAIL DE RECUPERACIÓN
     */
    private fun enviarEmailRecuperacion(email: String) {
        // Validar email
        if (email.isBlank()) {
            Toast.makeText(
                this,
                "❌ Email obligatorio",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(
                this,
                "❌ Email inválido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)

                AlertDialog.Builder(this)
                    .setTitle("✅ Email enviado")
                    .setMessage(
                        "Hemos enviado un email a $email con instrucciones para restablecer tu contraseña.\n\n" +
                                "Revisa tu bandeja de entrada y spam."
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
            }
            .addOnFailureListener { e ->
                showLoading(false)

                val mensaje = when {
                    e.message?.contains("no user record") == true ->
                        "No existe una cuenta con este email"

                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"

                    else ->
                        "Error: ${e.message}"
                }

                Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * IR A MAINACTIVITY
     */
    private fun irAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * LIMPIAR ERRORES
     */
    private fun limpiarErrores() {
        etEmail.error = null
        etPassword.error = null
    }

    /**
     * LOADING
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnRecuperarPassword.isEnabled = !show
        btnRegistrarse.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
    }
}
