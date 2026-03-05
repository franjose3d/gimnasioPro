package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.NotificacionAdapter
import com.example.gimnasiopro.data.firestore.ConexionRepository
import com.example.gimnasiopro.data.firestore.Notificacion
import com.example.gimnasiopro.data.firestore.NotificacionRepository
import com.example.gimnasiopro.data.firestore.UserHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Activity para mostrar y gestionar notificaciones.
 *
 * Funcionalidades:
 * - Ver solicitudes de conexión (trainer ↔ cliente)
 * - Ver mensajes (expiran en 7 días)
 * - Aceptar/Rechazar solicitudes
 * - Marcar como leídas
 */
class NotificacionesActivity : AppCompatActivity() {

    private lateinit var recyclerNotificaciones: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutAvisoMensajes: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnMarcarLeidas: ImageButton

    private lateinit var adapter: NotificacionAdapter
    private val notificacionRepository = NotificacionRepository()
    private val conexionRepository = ConexionRepository()

    private val auth = FirebaseAuth.getInstance()
    private var currentUserId: String? = null
    private var currentUserTipo: String? = null
    private var currentUserNombre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupAdapter()

        // Cargar info del usuario antes de observar notificaciones
        lifecycleScope.launch {
            loadUserInfo()
            observeNotificaciones()
        }
    }

    private fun setupViews() {
        recyclerNotificaciones = findViewById(R.id.recyclerNotificaciones)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutAvisoMensajes = findViewById(R.id.layoutAvisoMensajes)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnMarcarLeidas = findViewById(R.id.btnMarcarLeidas)

        btnBack.setOnClickListener { finish() }

        btnMarcarLeidas.setOnClickListener {
            marcarTodasComoLeidas()
        }

        recyclerNotificaciones.layoutManager = LinearLayoutManager(this)
    }

    private fun setupAdapter() {
        adapter = NotificacionAdapter(
            onNotificacionClick = { notificacion ->
                handleNotificacionClick(notificacion)
            },
            onAceptarClick = { notificacion ->
                handleAceptar(notificacion)
            },
            onRechazarClick = { notificacion ->
                handleRechazar(notificacion)
            },
            onEliminarClick = { notificacion ->
                handleEliminar(notificacion)
            }
        )
        recyclerNotificaciones.adapter = adapter
    }

    private suspend fun loadUserInfo() {
        currentUserId?.let { userId ->
            try {
                val userInfo = UserHelper.getUserInfo(userId)
                currentUserTipo = userInfo?.tipo
                currentUserNombre = userInfo?.nombre
                android.util.Log.d("NotificacionesActivity", "User info cargada: tipo=$currentUserTipo, nombre=$currentUserNombre")
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "Error cargando user info: ${e.message}")
            }
        }
    }

    private fun observeNotificaciones() {
        val userId = currentUserId ?: return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                notificacionRepository.getNotificacionesFlow(userId).collectLatest { notificaciones ->
                    progressBar.visibility = View.GONE

                    // ← NUEVO: limpiar expiradas de las que ya llegaron (sin lecturas extra)
                    val ahora = Date()
                    val notificacionesVigentes = notificaciones.filter { notificacion ->
                        val expirada = notificacion.fechaExpiracion?.before(ahora) == true
                        if (expirada) {
                            lifecycleScope.launch {
                                notificacionRepository.eliminarNotificacion(notificacion.id)
                            }
                        }
                        !expirada
                    }
                    // ← FIN NUEVO

                    if (notificacionesVigentes.isEmpty()) {
                        recyclerNotificaciones.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                        layoutAvisoMensajes.visibility = View.GONE
                    } else {
                        recyclerNotificaciones.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE

                        val hayMensajes = notificacionesVigentes.any { it.tipo == Notificacion.TIPO_MENSAJE }
                        layoutAvisoMensajes.visibility = if (hayMensajes) View.VISIBLE else View.GONE

                        adapter.submitList(notificacionesVigentes.toList())
                    }
                }
            } catch (e: Exception) {
                // ... resto del catch que ya tienes
            }
        }
    }

    private fun handleNotificacionClick(notificacion: Notificacion) {
        // NO marcar como leída si es una solicitud/invitación pendiente de aceptar/rechazar
        val esSolicitudPendiente = !notificacion.leida && (
            notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
            notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER
        )

        if (!esSolicitudPendiente) {
            // Solo marcar como leída si NO es una solicitud pendiente
            lifecycleScope.launch {
                notificacionRepository.marcarComoLeida(notificacion.id)
            }
        }

        // Acción según tipo
        when (notificacion.tipo) {
            Notificacion.TIPO_SOLICITUD_CONEXION, Notificacion.TIPO_INVITACION_TRAINER -> {
                // No hacer nada al click, el usuario debe usar los botones Aceptar/Rechazar
                if (!notificacion.leida) {
                    Toast.makeText(this, "Usa los botones Aceptar o Rechazar", Toast.LENGTH_SHORT).show()
                }
            }
            Notificacion.TIPO_MENSAJE -> {
                // Abrir chat con el remitente
                abrirConversacion(notificacion.remitenteId, notificacion.remitenteNombre)
            }
            Notificacion.TIPO_RUTINA_ACTUALIZADA -> {
                // Ir a rutinas
                startActivity(Intent(this, RutinasActivity::class.java))
            }
            Notificacion.TIPO_CONEXION_ACEPTADA -> {
                // Mostrar info
                Toast.makeText(this, "¡Conexión establecida!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAceptar(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "🔘 handleAceptar: tipo=${notificacion.tipo}, id=${notificacion.id}, datosExtra=${notificacion.datosExtra}")

        AlertDialog.Builder(this)
            .setTitle("Aceptar solicitud")
            .setMessage("¿Quieres aceptar esta solicitud de ${notificacion.remitenteNombre}?")
            .setPositiveButton("Aceptar") { _, _ ->
                when (notificacion.tipo) {
                    Notificacion.TIPO_SOLICITUD_CONEXION -> {
                        // Trainer acepta solicitud de cliente
                        aceptarSolicitudCliente(notificacion)
                    }
                    Notificacion.TIPO_INVITACION_TRAINER -> {
                        // Cliente acepta invitación de trainer
                        aceptarInvitacionTrainer(notificacion)
                    }
                    else -> {
                        android.util.Log.w("NotificacionesActivity", "⚠️ Tipo no manejado en aceptar: ${notificacion.tipo}")
                        Toast.makeText(this, "Tipo de notificación no soportado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleRechazar(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "🔘 handleRechazar: tipo=${notificacion.tipo}, id=${notificacion.id}")

        AlertDialog.Builder(this)
            .setTitle("Rechazar solicitud")
            .setMessage("¿Estás seguro de que quieres rechazar esta solicitud de ${notificacion.remitenteNombre}?")
            .setPositiveButton("Rechazar") { _, _ ->
                when (notificacion.tipo) {
                    Notificacion.TIPO_SOLICITUD_CONEXION -> {
                        rechazarSolicitudCliente(notificacion)
                    }
                    Notificacion.TIPO_INVITACION_TRAINER -> {
                        rechazarInvitacionTrainer(notificacion)
                    }
                    else -> {
                        android.util.Log.w("NotificacionesActivity", "⚠️ Tipo no manejado en rechazar: ${notificacion.tipo}")
                        Toast.makeText(this, "Tipo de notificación no soportado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aceptarSolicitudCliente(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "📩 aceptarSolicitudCliente: datosExtra=${notificacion.datosExtra}, remitenteId=${notificacion.remitenteId}")

        lifecycleScope.launch {
            try {
                val conexionId = notificacion.datosExtra["conexionId"]
                val clienteId = notificacion.datosExtra["clienteId"] ?: notificacion.remitenteId

                android.util.Log.d("NotificacionesActivity", "  conexionId=$conexionId, clienteId=$clienteId, currentUserId=$currentUserId")

                if (conexionId != null && conexionId.isNotEmpty()) {
                    // Tenemos el ID directo de la conexión
                    aceptarConexion(conexionId, notificacion)
                } else {
                    // Buscar la conexión pendiente por clienteId
                    android.util.Log.d("NotificacionesActivity", "  Buscando conexión pendiente para trainer=$currentUserId, cliente=$clienteId")
                    val solicitudes = conexionRepository.getSolicitudesPendientes(currentUserId!!).first()
                    android.util.Log.d("NotificacionesActivity", "  Solicitudes encontradas: ${solicitudes.size}")
                    solicitudes.forEach { s ->
                        android.util.Log.d("NotificacionesActivity", "    → id=${s.id}, clienteId=${s.clienteId}, trainerId=${s.trainerId}, estado=${s.estado}")
                    }

                    val conexion = solicitudes.find { it.clienteId == clienteId }
                    if (conexion != null) {
                        aceptarConexion(conexion.id, notificacion)
                    } else {
                        // Último intento: buscar por remitenteId directamente
                        val conexionPorRemitente = solicitudes.find { it.clienteId == notificacion.remitenteId }
                        if (conexionPorRemitente != null) {
                            aceptarConexion(conexionPorRemitente.id, notificacion)
                        } else {
                            android.util.Log.e("NotificacionesActivity", "❌ No se encontró conexión pendiente")
                            Toast.makeText(this@NotificacionesActivity, "Solicitud no encontrada. Puede haber expirado.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Error aceptando solicitud: ${e.message}", e)
                Toast.makeText(this@NotificacionesActivity, "Error al aceptar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun aceptarConexion(conexionId: String, notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "✅ aceptarConexion: conexionId=$conexionId")

        lifecycleScope.launch {
            try {
                val result = conexionRepository.aceptarSolicitud(conexionId)
                if (result.isSuccess) {
                    android.util.Log.d("NotificacionesActivity", "✅ Conexión aceptada exitosamente")

                    // Notificar al solicitante
                    notificacionRepository.notificarConexionAceptada(
                        destinatarioId = notificacion.remitenteId,
                        aceptadorId = currentUserId!!,
                        aceptadorNombre = currentUserNombre ?: "Usuario",
                        aceptadorTipo = currentUserTipo ?: "trainer"
                    )

                    // Marcar como procesada (oculta botones aceptar/rechazar)
                    notificacionRepository.marcarComoProcesada(notificacion.id)

                    Toast.makeText(this@NotificacionesActivity, "¡Conexión aceptada!", Toast.LENGTH_SHORT).show()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    android.util.Log.e("NotificacionesActivity", "❌ Error al aceptar conexión: $error")
                    Toast.makeText(this@NotificacionesActivity, "Error al aceptar: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Excepción aceptando conexión: ${e.message}", e)
                Toast.makeText(this@NotificacionesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun aceptarInvitacionTrainer(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "📩 aceptarInvitacionTrainer: datosExtra=${notificacion.datosExtra}, remitenteId=${notificacion.remitenteId}")

        lifecycleScope.launch {
            try {
                val trainerId = notificacion.datosExtra["trainerId"] ?: notificacion.remitenteId
                val conexionIdDirecto = notificacion.datosExtra["conexionId"]
                android.util.Log.d("NotificacionesActivity", "  trainerId=$trainerId, clienteId(yo)=$currentUserId, conexionIdDirecto=$conexionIdDirecto")

                // 1. Si tenemos conexionId directo en datosExtra, usarlo
                if (!conexionIdDirecto.isNullOrEmpty()) {
                    android.util.Log.d("NotificacionesActivity", "  Usando conexionId directo: $conexionIdDirecto")
                    val resultAceptar = conexionRepository.aceptarSolicitud(conexionIdDirecto)
                    if (resultAceptar.isSuccess) {
                        notificacionRepository.notificarConexionAceptada(
                            destinatarioId = trainerId,
                            aceptadorId = currentUserId!!,
                            aceptadorNombre = currentUserNombre ?: "Usuario",
                            aceptadorTipo = "cliente"
                        )
                        notificacionRepository.marcarComoProcesada(notificacion.id)
                        Toast.makeText(this@NotificacionesActivity, "¡Invitación aceptada!", Toast.LENGTH_SHORT).show()
                        return@launch
                    } else {
                        android.util.Log.w("NotificacionesActivity", "⚠️ conexionId directo falló, intentando alternativas...")
                    }
                }

                // 2. Buscar conexión pendiente existente
                val conexionExistente = conexionRepository.getConexionPendienteEntreUsuarios(
                    clienteId = currentUserId!!,
                    trainerId = trainerId
                )

                if (conexionExistente != null) {
                    // Ya existe una conexión pendiente, aceptarla directamente
                    android.util.Log.d("NotificacionesActivity", "  Conexión existente encontrada: ${conexionExistente.id}")
                    val resultAceptar = conexionRepository.aceptarSolicitud(conexionExistente.id)
                    if (resultAceptar.isSuccess) {
                        notificacionRepository.notificarConexionAceptada(
                            destinatarioId = trainerId,
                            aceptadorId = currentUserId!!,
                            aceptadorNombre = currentUserNombre ?: "Usuario",
                            aceptadorTipo = "cliente"
                        )
                        notificacionRepository.marcarComoProcesada(notificacion.id)
                        Toast.makeText(this@NotificacionesActivity, "¡Invitación aceptada!", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = resultAceptar.exceptionOrNull()?.message ?: "Error desconocido"
                        android.util.Log.e("NotificacionesActivity", "❌ Error aceptando conexión existente: $error")
                        Toast.makeText(this@NotificacionesActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Crear nueva conexión y aceptarla inmediatamente
                    android.util.Log.d("NotificacionesActivity", "  Creando nueva conexión...")
                    val result = conexionRepository.crearYAceptarConexion(
                        clienteId = currentUserId!!,
                        trainerId = trainerId
                    )

                    if (result.isSuccess) {
                        android.util.Log.d("NotificacionesActivity", "✅ Conexión creada y aceptada")

                        // Notificar al trainer
                        notificacionRepository.notificarConexionAceptada(
                            destinatarioId = trainerId,
                            aceptadorId = currentUserId!!,
                            aceptadorNombre = currentUserNombre ?: "Usuario",
                            aceptadorTipo = "cliente"
                        )

                        notificacionRepository.marcarComoProcesada(notificacion.id)
                        Toast.makeText(this@NotificacionesActivity, "¡Invitación aceptada!", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                        android.util.Log.e("NotificacionesActivity", "❌ Error creando conexión: $error")
                        Toast.makeText(this@NotificacionesActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Excepción aceptando invitación: ${e.message}", e)
                Toast.makeText(this@NotificacionesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rechazarSolicitudCliente(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "❌ rechazarSolicitudCliente: datosExtra=${notificacion.datosExtra}")

        lifecycleScope.launch {
            try {
                val conexionId = notificacion.datosExtra["conexionId"]
                val clienteId = notificacion.datosExtra["clienteId"] ?: notificacion.remitenteId

                if (conexionId != null && conexionId.isNotEmpty()) {
                    // Rechazar directamente por ID
                    conexionRepository.rechazarSolicitud(conexionId)
                } else {
                    // Buscar la conexión pendiente
                    val solicitudes = conexionRepository.getSolicitudesPendientes(currentUserId!!).first()
                    val conexion = solicitudes.find { it.clienteId == clienteId }
                        ?: solicitudes.find { it.clienteId == notificacion.remitenteId }

                    if (conexion != null) {
                        conexionRepository.rechazarSolicitud(conexion.id)
                    } else {
                        android.util.Log.w("NotificacionesActivity", "⚠️ No se encontró conexión para rechazar, solo marcamos como procesada")
                    }
                }

                // Notificar al solicitante
                notificacionRepository.notificarConexionRechazada(
                    destinatarioId = clienteId,
                    rechazadorId = currentUserId!!,
                    rechazadorNombre = currentUserNombre ?: "Usuario",
                    rechazadorTipo = currentUserTipo ?: "trainer"
                )

                notificacionRepository.marcarComoProcesada(notificacion.id)
                Toast.makeText(this@NotificacionesActivity, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Error rechazando solicitud: ${e.message}", e)
                Toast.makeText(this@NotificacionesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rechazarInvitacionTrainer(notificacion: Notificacion) {
        android.util.Log.d("NotificacionesActivity", "❌ rechazarInvitacionTrainer: datosExtra=${notificacion.datosExtra}")

        lifecycleScope.launch {
            try {
                val trainerId = notificacion.datosExtra["trainerId"] ?: notificacion.remitenteId

                // Buscar si hay conexión pendiente para rechazarla
                val conexionExistente = conexionRepository.getConexionPendienteEntreUsuarios(
                    clienteId = currentUserId!!,
                    trainerId = trainerId
                )
                if (conexionExistente != null) {
                    conexionRepository.rechazarSolicitud(conexionExistente.id)
                }

                // Notificar al trainer
                notificacionRepository.notificarConexionRechazada(
                    destinatarioId = trainerId,
                    rechazadorId = currentUserId!!,
                    rechazadorNombre = currentUserNombre ?: "Usuario",
                    rechazadorTipo = "cliente"
                )

                notificacionRepository.marcarComoProcesada(notificacion.id)
                Toast.makeText(this@NotificacionesActivity, "Invitación rechazada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Error rechazando invitación: ${e.message}", e)
                Toast.makeText(this@NotificacionesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleEliminar(notificacion: Notificacion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar notificación")
            .setMessage("¿Quieres eliminar esta notificación?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        android.util.Log.d("NotificacionesActivity",
                            "🗑️ Eliminando notificación: id=${notificacion.id}, tipo=${notificacion.tipo}")

                        if (notificacion.id.isEmpty()) {
                            android.util.Log.e("NotificacionesActivity", "❌ ID de notificación vacío")
                            Toast.makeText(this@NotificacionesActivity, "Error: notificación sin ID", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val result = notificacionRepository.eliminarNotificacion(notificacion.id)
                        if (result.isSuccess) {
                            // Cancelar notificación del sistema
                            com.example.gimnasiopro.presentation.NotificacionLocalService.cancelarNotificacion(
                                this@NotificacionesActivity,
                                notificacion.id
                            )

                            // Si era una notificación no leída, decrementar el badge
                            if (!notificacion.leida) {
                                com.example.gimnasiopro.utils.NotificationBadgeManager.decrementarBadge(this@NotificacionesActivity)
                            }

                            android.util.Log.d("NotificacionesActivity", "✅ Notificación eliminada correctamente")
                            Toast.makeText(this@NotificacionesActivity, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                            android.util.Log.e("NotificacionesActivity", "❌ Error al eliminar: $error", result.exceptionOrNull())
                            Toast.makeText(this@NotificacionesActivity, "Error al eliminar: $error", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NotificacionesActivity", "❌ Excepción eliminando notificación: ${e.message}", e)
                        Toast.makeText(this@NotificacionesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirConversacion(userId: String, userName: String) {
        val intent = Intent(this, ConversacionActivity::class.java).apply {
            putExtra("OTRO_USUARIO_ID", userId)
            putExtra("OTRO_USUARIO_NOMBRE", userName)
        }
        startActivity(intent)
    }

    private fun marcarTodasComoLeidas() {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                // 1. Marcar notificaciones de Firestore como leídas
                val result = notificacionRepository.marcarTodasComoLeidas(userId)

                // 2. Marcar TODOS los mensajes de Room como leídos (esto actualiza el badge)
                val db = com.example.gimnasiopro.data.GymDatabase.getDatabase(this@NotificacionesActivity)
                val mensajeRepository = com.example.gimnasiopro.data.firestore.MensajeRepository(db.mensajeDao())
                mensajeRepository.marcarTodosLosMensajesComoLeidos()

                // 3. Cancelar todas las notificaciones del sistema
                com.example.gimnasiopro.presentation.NotificacionLocalService.cancelarTodas(this@NotificacionesActivity)

                // 4. Limpiar badge del ícono de la app
                com.example.gimnasiopro.utils.NotificationBadgeManager.limpiarBadge(this@NotificacionesActivity)

                android.util.Log.d("NotificacionesActivity", "✅ Notificaciones y mensajes marcados como leídos, badge limpiado")

                if (result.isSuccess) {
                    Toast.makeText(
                        this@NotificacionesActivity,
                        "Todas marcadas como leídas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesActivity", "❌ Error marcando como leídas: ${e.message}")
                Toast.makeText(
                    this@NotificacionesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

