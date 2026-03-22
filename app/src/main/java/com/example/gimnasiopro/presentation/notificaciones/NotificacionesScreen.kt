package com.example.gimnasiopro.presentation.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.data.firestore.Notificacion
import com.example.gimnasiopro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: NotificacionesViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChat: ((userId: String, userName: String?) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    if (state.notificaciones.isNotEmpty()) {
                        TextButton(onClick = { viewModel.marcarTodasLeidas() }) {
                            Text("Leer todas", color = AccentBlue, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
            return@Scaffold
        }

        if (state.notificaciones.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes notificaciones", color = TextSecondary, fontSize = 15.sp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.notificaciones, key = { it.id }) { notif ->
                NotificacionItem(
                    notif = notif,
                    onAceptar = { viewModel.aceptar(notif) },
                    onRechazar = { viewModel.rechazar(notif) },
                    onEliminar = { viewModel.eliminar(notif) },
                    onMarcarLeida = { viewModel.marcarComoLeida(notif) },
                    onAbrirChat = if (notif.tipo == Notificacion.TIPO_MENSAJE && notif.remitenteId.isNotBlank()) {
                        {
                            viewModel.marcarComoLeida(notif)
                            onNavigateToChat?.invoke(notif.remitenteId, notif.remitenteNombre.takeIf { it.isNotBlank() })
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun NotificacionItem(
    notif: Notificacion,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onEliminar: () -> Unit,
    onMarcarLeida: () -> Unit,
    onAbrirChat: (() -> Unit)? = null
) {
    val isSolicitud = notif.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                      notif.tipo == Notificacion.TIPO_INVITACION_TRAINER
    val isPendiente = isSolicitud && !notif.procesada

    val bgColor = if (!notif.leida) CardDark.copy(alpha = 0.9f) else CardDark.copy(alpha = 0.5f)

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onAbrirChat != null) Modifier.clickable(onClick = onAbrirChat)
                else Modifier
            )
    ) {
        Column(Modifier.padding(14.dp)) {

            // Unread indicator + title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!notif.leida && !isPendiente) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(AccentBlue, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    notif.titulo.ifBlank { tipoLabel(notif.tipo) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // Delete button (only for non-pending solicitudes or processed/other types)
                if (!isPendiente) {
                    IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = RedDelete, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (notif.remitenteNombre.isNotBlank()) {
                Text(notif.remitenteNombre, fontSize = 12.sp, color = AccentBlue,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
            }

            if (notif.mensaje.isNotBlank()) {
                Text(notif.mensaje, fontSize = 13.sp, color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp))
            }

            Text(
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(notif.fechaCreacion),
                fontSize = 11.sp, color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )

            // Accept / Reject buttons for pending solicitudes
            if (isPendiente) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAceptar,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) { Text("Aceptar", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDelete),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDelete)
                    ) { Text("Rechazar", fontSize = 13.sp) }
                }
            } else if (!notif.leida) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onMarcarLeida,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Marcar como leída", fontSize = 12.sp, color = AccentBlue)
                }
            }
        }
    }
}

private fun tipoLabel(tipo: String) = when (tipo) {
    Notificacion.TIPO_SOLICITUD_CONEXION  -> "Solicitud de conexión"
    Notificacion.TIPO_INVITACION_TRAINER  -> "Invitación de trainer"
    Notificacion.TIPO_MENSAJE             -> "Mensaje"
    Notificacion.TIPO_RUTINA_ACTUALIZADA  -> "Rutina actualizada"
    Notificacion.TIPO_CONEXION_ACEPTADA   -> "Conexión aceptada"
    Notificacion.TIPO_CONEXION_RECHAZADA  -> "Conexión rechazada"
    Notificacion.TIPO_SISTEMA             -> "Sistema"
    else                                  -> "Notificación"
}
