package com.example.gimnasiopro.presentation.conversacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.data.local.MensajeLocal
import com.example.gimnasiopro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversacionScreen(
    viewModel: ConversacionViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Scroll al último mensaje cuando llegan nuevos
    LaunchedEffect(state.mensajes.size) {
        if (state.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(state.mensajes.size - 1)
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Mark as read when screen is visible
    LaunchedEffect(Unit) { viewModel.marcarLeidos() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val (subtitulo, subtituloColor) = when (val dias = state.diasRestantes) {
                null  -> "Los mensajes expiran en 7 días" to TextSecondary
                -1    -> "Conversación expirada" to RedDelete
                0     -> "Expira hoy" to RedDelete
                1     -> "Queda 1 día" to Color(0xFFFF9800)
                2     -> "Quedan 2 días" to Color(0xFFFF9800)
                else  -> "Quedan $dias días" to TextSecondary
            }
            TopAppBar(
                title = {
                    Column {
                        Text(state.otroNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentBlue)
                        Text(subtitulo, fontSize = 11.sp, color = subtituloColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (state.diasRestantes == -1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Esta conversación ha expirado", color = RedDelete, fontSize = 13.sp)
                    }
                }
            } else {
                InputBar(
                    texto = state.texto,
                    onTextoChange = viewModel::onTextoChange,
                    onEnviar = viewModel::enviarMensaje,
                    isSending = state.isSending
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            return@Scaffold
        }

        if (state.mensajes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay mensajes. ¡Empieza la conversación!",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.mensajes, key = { it.id }) { mensaje ->
                MensajeBubble(
                    mensaje = mensaje,
                    currentUserNombre = state.currentUserNombre,
                    otroNombre = state.otroNombre,
                    diasRestantes = state.diasRestantes
                )
            }
        }
    }
}

@Composable
private fun MensajeBubble(
    mensaje: MensajeLocal,
    currentUserNombre: String,
    otroNombre: String,
    diasRestantes: Int? = null
) {
    val isMe = mensaje.esRemitente
    val bubbleColor  = if (isMe) AccentBlue else CardDark
    val textColor    = if (isMe) TextPrimary else MaterialTheme.colorScheme.onSurface
    val alignment    = if (isMe) Alignment.End else Alignment.Start
    val bubbleShape  = if (isMe)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Sender name (only for received messages)
        if (!isMe) {
            Text(
                otroNombre,
                fontSize = 11.sp,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(mensaje.texto, color = textColor, fontSize = 14.sp)
                val metaColor = if (isMe) TextPrimary.copy(alpha = 0.7f) else TextSecondary
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(mensaje.fechaEnvio)),
                    fontSize = 10.sp,
                    color = metaColor,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
                val textoExpiracion = when (diasRestantes) {
                    null, 7 -> "Se borrará en 7 días"
                    0       -> "Se borra hoy"
                    1       -> "Se borra mañana"
                    -1      -> null  // conversación ya eliminada, no se muestra
                    else    -> "Se borrará en $diasRestantes días"
                }
                if (textoExpiracion != null) {
                    Text(
                        textoExpiracion,
                        fontSize = 9.sp,
                        color = when (diasRestantes) {
                            0, 1 -> RedDelete.copy(alpha = 0.8f)
                            2    -> Color(0xFFFF9800).copy(alpha = 0.9f)
                            else -> metaColor.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    texto: String,
    onTextoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    isSending: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                placeholder = { Text("Escribe un mensaje...", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DividerColor
                ),
                trailingIcon = {
                    Text(
                        "${texto.length}/200",
                        fontSize = 10.sp,
                        color = if (texto.length > 180) OrangeAccent else TextSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onEnviar,
                enabled = texto.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (texto.isNotBlank()) AccentBlue else CardDark,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, "Enviar", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
