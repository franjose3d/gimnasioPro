package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.firestore.MensajeRepository
import com.example.gimnasiopro.data.firestore.MensajesSyncService
import com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import com.example.gimnasiopro.utils.BadgeHelper

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvRachaTitle: TextView
    private lateinit var tvDayL: TextView
    private lateinit var tvDayM: TextView
    private lateinit var tvDayX: TextView
    private lateinit var ivUserPhoto: ImageView
    private lateinit var cardRegistrate: LinearLayout
    private lateinit var tvBadgeNotifications: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Cache del nombre de usuario para evitar lecturas repetidas a Firebase
    @Volatile private var cachedUserName: String? = null
    @Volatile private var cachedUserId: String? = null

    // Control para sincronizar rutinas solo una vez por sesión
    @Volatile private var rutinasSincronizadas = false

    // ====== NUEVO - Sincronización de mensajes ======
    private var mensajesSync: MensajesSyncService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupViews()
        setupCards()
        loadUserData()

        // ====== NUEVO - Inicializar sincronización de mensajes ======
        inicializarSincronizacionMensajes()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos del usuario cuando vuelve de otra pantalla
        loadUserData()
    }

    // ====== NUEVO ======
    override fun onDestroy() {
        super.onDestroy()
        // Detener escucha de mensajes al cerrar la app
        mensajesSync?.detenerEscucha()
    }

    private fun setupViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvRachaTitle = findViewById(R.id.tvRachaTitle)
        tvDayL = findViewById(R.id.tvDayL)
        tvDayM = findViewById(R.id.tvDayM)
        tvDayX = findViewById(R.id.tvDayX)
        ivUserPhoto = findViewById(R.id.ivUserPhoto)
        cardRegistrate = findViewById(R.id.cardRegistrate)
        tvBadgeNotifications = findViewById(R.id.tvBadgeNotifications)
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

        // Imagen de perfil del usuario - acceso a registro/perfil
        ivUserPhoto.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                mostrarOpcionesPerfil()
            } else {
                val intent = Intent(this, PersonalTrainerActivity::class.java)
                startActivity(intent)
            }
        }

        // Texto del nombre de usuario
        tvUserName.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                mostrarOpcionesPerfil()
            } else {
                val intent = Intent(this, PersonalTrainerActivity::class.java)
                startActivity(intent)
            }
        }

        // Card Contacta
        cardRegistrate.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(this, ContactaActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Inicia sesión para acceder a contactos", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PersonalTrainerActivity::class.java)
                startActivity(intent)
            }
        }

        // Botón de notificaciones
        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(this, NotificacionesActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Inicia sesión para ver notificaciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Cargar nombre (con cache)
            if (cachedUserId == currentUser.uid && cachedUserName != null) {
                tvUserName.text = cachedUserName
            } else {
                cargarNombreUsuario(currentUser.uid, currentUser.displayName, currentUser.email)
            }

            // Sincronizar rutinas desde Firebase solo una vez por sesión
            if (!rutinasSincronizadas) {
                lifecycleScope.launch {
                    try {
                        val app = application as GimnasioproApplication
                        val prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
                        val ultimaSync = prefs.getLong("ultima_sync_rutinas", 0L)
                        val ahora = System.currentTimeMillis()
                        val cincuentaMinutos = 50 * 60 * 1000L // 50 minutos para evitar consumo excesivo

                        // Solo sincronizar si pasó más de 50 minutos desde la última vez
                        if (ahora - ultimaSync > cincuentaMinutos) {
                            android.util.Log.d("MainActivity", "🔄 Iniciando sincronización bidireccional de rutinas...")
                            val result = app.rutinaRepositoryHibrido.sincronizarRutinasConFirebase()
                            result.onSuccess { cantidadSincronizada ->
                                prefs.edit().putLong("ultima_sync_rutinas", ahora).apply()
                                if (cantidadSincronizada > 0) {
                                    android.util.Log.d("MainActivity", "✅ $cantidadSincronizada rutinas sincronizadas")
                                } else {
                                    android.util.Log.d("MainActivity", "✅ Rutinas sincronizadas (sin cambios)")
                                }
                            }.onFailure { error ->
                                android.util.Log.w("MainActivity", "⚠️ Error sincronizando rutinas: ${error.message}")
                            }
                        } else {
                            val minutosRestantes = (maxOf(0L, cincuentaMinutos - (ahora - ultimaSync)) / (60 * 1000)).toInt()
                            android.util.Log.d("MainActivity", "⏭️ Sync omitida, datos recientes (próxima sync en $minutosRestantes min)")
                        }
                        rutinasSincronizadas = true
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Error sincronizando rutinas: ${e.message}")
                    }
                }
            }
            observarMensajesNoLeidos()

        } else {
            // Usuario no logueado - resetear cache y flag de sincronización
            cachedUserName = null
            cachedUserId = null
            rutinasSincronizadas = false
            tvUserName.text = getString(R.string.default_user_name)
            tvBadgeNotifications.visibility = View.GONE
        }

        // Cargar racha actual
        lifecycleScope.launch {
            try {
                val app = application as GimnasioproApplication
                val racha = app.estadisticaRepository.getRachaActual()
                tvRachaTitle.text = resources.getQuantityString(R.plurals.racha_title, racha, racha)
                actualizarIndicadoresDias(racha)
            } catch (e: Exception) {
                tvRachaTitle.text = resources.getQuantityString(R.plurals.racha_title, 0, 0)
            }
        }
    }

    private fun cargarNombreUsuario(uid: String, displayName: String?, email: String?) {
        if (!displayName.isNullOrBlank()) {
            tvUserName.text = displayName.uppercase()
            cachedUserName = displayName.uppercase()
            cachedUserId = uid
            return
        }

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val tipoGuardado = prefs.getString("tipo_usuario_$uid", null)

        // Si ya sabemos el tipo, solo hacemos 1 lectura
        if (tipoGuardado != null) {
            val coleccion = if (tipoGuardado == "trainer") "trainers" else "clientes"
            firestore.collection(coleccion).document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val nombre = doc.getString("nombre")
                        if (!nombre.isNullOrBlank()) {
                            tvUserName.text = nombre.uppercase()
                            cachedUserName = nombre.uppercase()
                            cachedUserId = uid
                        }
                    }
                }
            return
        }

        // No sabemos el tipo - buscar en TRAINERS primero (tiene prioridad)
        firestore.collection("trainers").document(uid).get()
            .addOnSuccessListener { trainerDoc ->
                if (trainerDoc.exists()) {
                    val nombre = trainerDoc.getString("nombre")
                    if (!nombre.isNullOrBlank()) {
                        tvUserName.text = nombre.uppercase()
                        cachedUserName = nombre.uppercase()
                        cachedUserId = uid
                        // Guardar tipo para la próxima vez
                        prefs.edit().putString("tipo_usuario_$uid", "trainer").apply()
                        return@addOnSuccessListener
                    }
                }

                // Si no está en trainers o el documento está vacío, buscar en clientes
                firestore.collection("clientes").document(uid).get()
                    .addOnSuccessListener { clienteDoc ->
                        val nombre = clienteDoc.getString("nombre")
                            ?: email?.substringBefore("@") ?: "USUARIO"
                        tvUserName.text = nombre.uppercase()
                        cachedUserName = nombre.uppercase()
                        cachedUserId = uid
                        if (clienteDoc.exists() && !clienteDoc.getString("nombre").isNullOrBlank()) {
                            prefs.edit().putString("tipo_usuario_$uid", "cliente").apply()
                        }
                    }
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
                        val intent = Intent(this, PerfilActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        // ====== MODIFICADO ======
                        auth.signOut()
                        rutinasSincronizadas = false
                        mensajesSync?.detenerEscucha()  // Detener sincronización de mensajes
                        mensajesSync = null

                        // Limpiar badge de notificaciones
                        com.example.gimnasiopro.utils.NotificationBadgeManager.limpiarTodo(this)

                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }
            .show()
    }

    private fun actualizarIndicadoresDias(racha: Int) {
        val diaActual = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

        tvDayL.setBackgroundResource(R.drawable.bg_day_inactive)
        tvDayM.setBackgroundResource(R.drawable.bg_day_inactive)
        tvDayX.setBackgroundResource(R.drawable.bg_day_inactive)

        if (racha > 0) {
            when (diaActual) {
                java.util.Calendar.MONDAY -> tvDayL.setBackgroundResource(R.drawable.bg_day_active)
                java.util.Calendar.TUESDAY -> tvDayM.setBackgroundResource(R.drawable.bg_day_active)
                java.util.Calendar.WEDNESDAY -> tvDayX.setBackgroundResource(R.drawable.bg_day_active)
            }
        }
    }

    private fun navigateToGim() {
        startActivity(Intent(this, GimActivity::class.java))
    }

    private fun navigateToEjercicios() {
        startActivity(Intent(this, EjerciciosActivity::class.java))
    }

    private fun navigateToRutinas() {
        startActivity(Intent(this, RutinasActivity::class.java))
    }

    private fun navigateToProgreso() {
        startActivity(Intent(this, ProgresoActivity::class.java))
    }

    // ====== NUEVO - Funciones de sincronización de mensajes ======

    /**
     * Inicializa la sincronización de mensajes si el usuario está autenticado.
     */
    private fun inicializarSincronizacionMensajes() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Inicializar repositorio y servicio de sincronización
            val db = GymDatabase.getDatabase(this)
            val mensajeRepository = MensajeRepository(db.mensajeDao())

            mensajesSync = MensajesSyncService(mensajeRepository)
            mensajesSync?.iniciarEscucha()

            // Obtener y guardar FCM token
            obtenerYGuardarFCMToken()

            android.util.Log.d("MainActivity", "✅ Sincronización de mensajes iniciada")
        } else {
            android.util.Log.d("MainActivity", "⏭️ Usuario no autenticado, sync omitida")
        }
    }

    /**
     * Obtiene el token de FCM y lo guarda en Firestore.
     */
    private fun obtenerYGuardarFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                guardarFCMTokenEnFirestore(token)
            } else {
                android.util.Log.e("MainActivity", "Error obteniendo FCM token: ${task.exception?.message}")
            }
        }
    }

    /**
     * Guarda el token FCM en Firestore para poder enviar notificaciones.
     */
    private fun guardarFCMTokenEnFirestore(token: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .set(
                hashMapOf("fcmToken" to token),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                android.util.Log.d("MainActivity", "✅ FCM Token guardado en Firestore")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "❌ Error guardando FCM token: ${e.message}")
            }
    }
    private fun observarMensajesNoLeidos() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacionRepository = com.example.gimnasiopro.data.firestore.NotificacionRepository()

        // Variable para detectar nuevas notificaciones (evitar notificar las existentes al inicio)
        var primeraVez = true
        var notificacionesAnteriores = emptyList<com.example.gimnasiopro.data.firestore.Notificacion>()

        lifecycleScope.launch {
            // Observar notificaciones no leídas desde Firestore (incluye solicitudes, invitaciones y mensajes)
            notificacionRepository.getNotificacionesFlow(userId).collect { notificaciones ->
                val count = notificaciones.count { !it.leida }
                actualizarBadgeNotificaciones(count)

                // Sincronizar badge del ícono de la app
                com.example.gimnasiopro.utils.NotificationBadgeManager.sincronizarConFirestore(this@MainActivity, count)

                android.util.Log.d("MainActivity", "🔔 Badge actualizado: $count notificaciones no leídas")

                // Detectar NUEVAS notificaciones (solo después de la carga inicial)
                if (!primeraVez) {
                    val nuevasNotificaciones = notificaciones.filter { notif ->
                        !notif.leida && notificacionesAnteriores.none { it.id == notif.id }
                    }

                    // Generar notificaciones locales para las nuevas
                    nuevasNotificaciones.forEach { notif ->
                        android.util.Log.d("MainActivity", "🔔 Nueva notificación detectada: ${notif.titulo}")
                        com.example.gimnasiopro.presentation.NotificacionLocalService.mostrarNotificacion(
                            this@MainActivity,
                            notif
                        )
                    }
                }

                // Actualizar lista de notificaciones anteriores
                notificacionesAnteriores = notificaciones
                primeraVez = false

                // DIAGNÓSTICO: Ejecutar solo una vez al inicio
                if (count > 0 && primeraVez) {
                    com.example.gimnasiopro.utils.NotificationBadgeManager.diagnosticar(this@MainActivity)
                }
            }
        }
    }

    private fun actualizarBadgeNotificaciones(count: Int) {
        tvBadgeNotifications.visibility = BadgeHelper.obtenerVisibilidad(count)
        tvBadgeNotifications.text = BadgeHelper.obtenerTextoBadge(count)
    }
}
