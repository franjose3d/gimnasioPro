package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.gimnasiopro.presentation.auth.LoginActivity
import com.example.gimnasiopro.presentation.cliente.RegisterClienteActivity
import com.example.gimnasiopro.presentation.trainer.RegisterTrainerActivity

/**
 * Activity principal de Personal Trainer.
 *
 * 3 OPCIONES:
 * 1. Iniciar sesión (si ya tienes cuenta)
 * 2. Registrarse como usuario/cliente
 * 3. Registrarse como entrenador
 */
class PersonalTrainerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnLogin: Button
    private lateinit var btnRegistrarseUsuario: Button
    private lateinit var btnRegistrarseTrainer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_trainer)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistrarseUsuario = findViewById(R.id.btnRegistrarseUsuario)
        btnRegistrarseTrainer = findViewById(R.id.btnRegistrarseTrainer)
    }

    private fun setupClickListeners() {
        // Volver atrás
        btnBack.setOnClickListener {
            finish()
        }

        // BOTÓN 0: Iniciar sesión (NUEVO)
        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // BOTÓN 1: Registrarse como usuario/cliente
        btnRegistrarseUsuario.setOnClickListener {
            val intent = Intent(this, RegisterClienteActivity::class.java)
            startActivity(intent)
        }

        // BOTÓN 2: Registrarse como entrenador
        btnRegistrarseTrainer.setOnClickListener {
            val intent = Intent(this, RegisterTrainerActivity::class.java)
            startActivity(intent)
        }
    }
}