package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvRachaTitle: TextView
    private lateinit var tvDayL: TextView
    private lateinit var tvDayM: TextView
    private lateinit var tvDayX: TextView
    private lateinit var cardRegistrate: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupViews()
        setupCards()
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos del usuario cuando vuelve de otra pantalla
        loadUserData()
    }

    private fun setupViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvRachaTitle = findViewById(R.id.tvRachaTitle)
        tvDayL = findViewById(R.id.tvDayL)
        tvDayM = findViewById(R.id.tvDayM)
        tvDayX = findViewById(R.id.tvDayX)
        cardRegistrate = findViewById(R.id.cardRegistrate)
    }

    private fun setupCards() {
        // Card GYM
        findViewById<LinearLayout>(R.id.cardGym).setOnClickListener {
            navigateToGim()
        }

        // Card Ejercicios
        findViewById<LinearLayout>(R.id.cardEjercicios).setOnClickListener {
            navigateToEjercicios()
        }

        // Card Rutinas
        findViewById<LinearLayout>(R.id.cardRutinas).setOnClickListener {
            navigateToRutinas()
        }

        // Card Progreso
        findViewById<LinearLayout>(R.id.cardProgreso).setOnClickListener {
            navigateToProgreso()
        }

        // Card Regístrate / Mi Perfil
        cardRegistrate.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Ya está logueado - mostrar opciones de perfil
                mostrarOpcionesPerfil()
            } else {
                // No está logueado - ir a PersonalTrainerActivity que tiene todas las opciones
                val intent = Intent(this, PersonalTrainerActivity::class.java)
                startActivity(intent)
            }
        }

        // Botón de notificaciones
        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            // TODO: Implementar pantalla de notificaciones
        }
    }


    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Usuario logueado - cargar nombre
            cargarNombreUsuario(currentUser.uid, currentUser.displayName, currentUser.email)
        } else {
            // Usuario no logueado
            tvUserName.text = getString(R.string.default_user_name)
        }

        // Cargar racha actual
        lifecycleScope.launch {
            try {
                val app = application as GimnasioproApplication
                val racha = app.estadisticaRepository.getRachaActual()
                tvRachaTitle.text = getString(R.string.racha_title, racha)

                // Actualizar indicadores de días según la racha
                actualizarIndicadoresDias(racha)
            } catch (e: Exception) {
                tvRachaTitle.text = getString(R.string.racha_title, 0)
            }
        }
    }

    private fun cargarNombreUsuario(uid: String, displayName: String?, email: String?) {
        // Primero intentar con displayName de Firebase Auth
        if (!displayName.isNullOrBlank()) {
            tvUserName.text = displayName.uppercase()
            return
        }

        // Buscar en clientes primero
        firestore.collection("clientes").document(uid).get()
            .addOnSuccessListener { clienteDoc ->
                if (clienteDoc.exists()) {
                    val nombre = clienteDoc.getString("nombre")
                    if (!nombre.isNullOrBlank()) {
                        tvUserName.text = nombre.uppercase()
                        return@addOnSuccessListener
                    }
                }

                // Si no está en clientes, buscar en trainers
                firestore.collection("trainers").document(uid).get()
                    .addOnSuccessListener { trainerDoc ->
                        if (trainerDoc.exists()) {
                            val nombre = trainerDoc.getString("nombre")
                            if (!nombre.isNullOrBlank()) {
                                tvUserName.text = nombre.uppercase()
                                return@addOnSuccessListener
                            }
                        }

                        // Fallback: usar email
                        tvUserName.text = email?.substringBefore("@")?.uppercase() ?: "USUARIO"
                    }
                    .addOnFailureListener {
                        tvUserName.text = email?.substringBefore("@")?.uppercase() ?: "USUARIO"
                    }
            }
            .addOnFailureListener {
                // Error, usar email como fallback
                tvUserName.text = email?.substringBefore("@")?.uppercase() ?: "USUARIO"
            }
    }

    private fun mostrarOpcionesPerfil() {
        val currentUser = auth.currentUser ?: return

        val opciones = arrayOf("Ver perfil", "Cerrar sesión")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mi cuenta")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        // Ver perfil - abrir PerfilActivity
                        val intent = Intent(this, PerfilActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        // Cerrar sesión
                        auth.signOut()
                        loadUserData() // Recargar para mostrar "USUARIO"
                        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun actualizarIndicadoresDias(racha: Int) {
        val diaActual = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

        // Resetear todos a inactivo
        tvDayL.setBackgroundResource(R.drawable.bg_day_inactive)
        tvDayM.setBackgroundResource(R.drawable.bg_day_inactive)
        tvDayX.setBackgroundResource(R.drawable.bg_day_inactive)

        // Activar según el día de la semana (solo si hay racha)
        if (racha > 0) {
            when (diaActual) {
                java.util.Calendar.MONDAY -> tvDayL.setBackgroundResource(R.drawable.bg_day_active)
                java.util.Calendar.TUESDAY -> tvDayM.setBackgroundResource(R.drawable.bg_day_active)
                java.util.Calendar.WEDNESDAY -> tvDayX.setBackgroundResource(R.drawable.bg_day_active)
            }
        }
    }

    private fun navigateToGim() {
        val intent = Intent(this, GimActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToEjercicios() {
        val intent = Intent(this, EjerciciosActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRutinas() {
        val intent = Intent(this, RutinasActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProgreso() {
        val intent = Intent(this, ProgresoActivity::class.java)
        startActivity(intent)
    }
}