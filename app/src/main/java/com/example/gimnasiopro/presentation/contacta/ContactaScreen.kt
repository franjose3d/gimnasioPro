package com.example.gimnasiopro.presentation.contacta

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactaScreen(
    viewModel: ContactaViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onBuscarTrainer: () -> Unit,
    onMisClientes: () -> Unit,
    onChatConTrainer: (trainerId: String, trainerNombre: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Invite client dialog state
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteInput by remember { mutableStateOf("") }

    // Trainer options dialog (for cliente with trainer)
    var showTrainerOptions by remember { mutableStateOf(false) }

    // Desvincular confirm dialog
    var showDesvinculaDialog by remember { mutableStateOf(false) }

    // Buscar otro trainer info dialog
    var showBuscarOtroDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; inviteInput = "" },
            title = { Text("Invitar Cliente") },
            text = {
                Column {
                    Text(
                        "Introduce el email o número de teléfono del cliente.\nSi es teléfono, se añadirá +34 automáticamente.",
                        fontSize = 13.sp, color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it },
                        label = { Text("Email o teléfono") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DividerColor
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteInput.isNotBlank()) {
                        viewModel.enviarInvitacion(inviteInput)
                        showInviteDialog = false
                        inviteInput = ""
                    }
                }) { Text("Enviar invitación", color = AccentBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false; inviteInput = "" }) { Text("Cancelar") }
            }
        )
    }

    if (showTrainerOptions) {
        AlertDialog(
            onDismissRequest = { showTrainerOptions = false },
            title = { Text("Opciones de Trainer") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showTrainerOptions = false
                            onChatConTrainer(state.trainerId, state.trainerNombre)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Chat con mi Trainer") }
                    TextButton(
                        onClick = { showTrainerOptions = false; showBuscarOtroDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Buscar Otro Trainer") }
                    TextButton(
                        onClick = { showTrainerOptions = false; showDesvinculaDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Desvincularme", color = RedDelete) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTrainerOptions = false }) { Text("Cancelar") } }
        )
    }

    if (showBuscarOtroDialog) {
        AlertDialog(
            onDismissRequest = { showBuscarOtroDialog = false },
            title = { Text("Buscar Otro Trainer") },
            text = { Text("Puedes consultar otros trainers sin perder tu conexión actual.\n\nSi deseas cambiar de trainer, primero debes desvincularte del actual.") },
            confirmButton = {
                TextButton(onClick = { showBuscarOtroDialog = false; onBuscarTrainer() }) {
                    Text("Ver Trainers", color = AccentBlue)
                }
            },
            dismissButton = { TextButton(onClick = { showBuscarOtroDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDesvinculaDialog) {
        AlertDialog(
            onDismissRequest = { showDesvinculaDialog = false },
            title = { Text("Desvincular Trainer") },
            text = {
                Text(
                    "¿Estás seguro de que quieres desvincularte de tu trainer?\n\n" +
                    "Se perderá:\n• Rutinas asignadas por el trainer\n" +
                    "• Acceso del trainer a tu progreso\n• Historial de chat (local)\n\n" +
                    "Podrás buscar otro trainer después."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.desvincularTrainer(); showDesvinculaDialog = false }) {
                    Text("Sí, Desvincularme", color = RedDelete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDesvinculaDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Contacta", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state.tipoUsuario) {
                "cliente" -> ClienteContent(
                    state = state,
                    onBuscarTrainer = onBuscarTrainer,
                    onMiTrainer = { showTrainerOptions = true }
                )
                "trainer" -> TrainerContent(
                    state = state,
                    onMisClientes = onMisClientes,
                    onInvitarCliente = { showInviteDialog = true }
                )
            }
        }
    }
}

// ── Cliente section ───────────────────────────────────────────────────────────

@Composable
private fun ClienteContent(
    state: ContactaUiState,
    onBuscarTrainer: () -> Unit,
    onMiTrainer: () -> Unit
) {
    Text("Área Cliente", fontSize = 13.sp, color = TextSecondary,
        modifier = Modifier.padding(bottom = 4.dp))

    if (!state.tieneTrainer && !state.tieneSolicitudPendiente) {
        ContactCard(
            title = "Buscar Trainer",
            subtitle = "Encuentra un entrenador personal para ti",
            accentColor = AccentBlue,
            onClick = onBuscarTrainer
        )
    } else {
        ContactCard(
            title = state.trainerNombre.ifBlank { "Mi Trainer" },
            subtitle = state.trainerEstado,
            accentColor = if (state.tieneTrainer) AccentGreen else OrangeAccent,
            onClick = if (state.tieneTrainer) onMiTrainer else null
        )
    }
}

// ── Trainer section ───────────────────────────────────────────────────────────

@Composable
private fun TrainerContent(
    state: ContactaUiState,
    onMisClientes: () -> Unit,
    onInvitarCliente: () -> Unit
) {
    Text("Área Trainer", fontSize = 13.sp, color = TextSecondary,
        modifier = Modifier.padding(bottom = 4.dp))

    ContactCard(
        title = "Mis Clientes",
        subtitle = "${state.numClientes} cliente${if (state.numClientes != 1) "s" else ""} conectado${if (state.numClientes != 1) "s" else ""}",
        accentColor = AccentBlue,
        onClick = onMisClientes
    )
    ContactCard(
        title = "Invitar Cliente",
        subtitle = "Envía una invitación por email o teléfono",
        accentColor = AccentPurple,
        onClick = onInvitarCliente
    )
}

// ── Reusable card ─────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth()
    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp))
            }
            if (onClick != null) {
                Icon(Icons.Default.KeyboardArrowRight, "Ir", tint = TextSecondary)
            }
        }
    }
}
