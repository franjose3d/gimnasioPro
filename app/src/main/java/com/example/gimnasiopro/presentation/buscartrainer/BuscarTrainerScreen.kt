package com.example.gimnasiopro.presentation.buscartrainer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.gimnasiopro.data.firestore.UserHelper
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscarTrainerScreen(
    viewModel: BuscarTrainerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Dialog state
    var selectedTrainer by remember { mutableStateOf<UserHelper.UserInfo?>(null) }
    var mensajeSolicitud by remember { mutableStateOf("") }

    // Success dialog
    var solicitudNombre by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is BuscarTrainerUiState.SolicitudEnviada) {
            solicitudNombre = (state as BuscarTrainerUiState.SolicitudEnviada).trainerNombre
        }
    }

    // Send request dialog
    if (selectedTrainer != null) {
        AlertDialog(
            onDismissRequest = { selectedTrainer = null; mensajeSolicitud = "" },
            title = { Text("Solicitar conexión") },
            text = {
                Column {
                    Text(
                        "Enviar solicitud a ${selectedTrainer!!.nombre ?: "Trainer"}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = mensajeSolicitud,
                        onValueChange = { if (it.length <= 200) mensajeSolicitud = it },
                        label = { Text("Mensaje (opcional)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DividerColor
                        )
                    )
                    Text(
                        "${mensajeSolicitud.length}/200",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enviarSolicitud(selectedTrainer!!, mensajeSolicitud)
                    selectedTrainer = null
                    mensajeSolicitud = ""
                }) { Text("Enviar", color = AccentBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { selectedTrainer = null; mensajeSolicitud = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Success dialog
    if (solicitudNombre != null) {
        AlertDialog(
            onDismissRequest = { solicitudNombre = null; viewModel.loadTrainers() },
            title = { Text("¡Solicitud enviada!") },
            text = { Text("Tu solicitud a $solicitudNombre ha sido enviada. Espera su respuesta.") },
            confirmButton = {
                TextButton(onClick = { solicitudNombre = null; viewModel.loadTrainers() }) {
                    Text("Aceptar", color = AccentBlue)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Trainer", fontWeight = FontWeight.Bold, color = AccentBlue) },
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

        when (val s = state) {
            is BuscarTrainerUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }

            is BuscarTrainerUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadTrainers() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) { Text("Reintentar") }
                    }
                }
            }

            is BuscarTrainerUiState.Success -> {
                if (s.trainers.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No hay trainers disponibles", color = TextSecondary, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.trainers, key = { it.userId }) { trainer ->
                            TrainerCard(trainer = trainer, onSolicitar = { selectedTrainer = trainer })
                        }
                    }
                }
            }

            is BuscarTrainerUiState.SolicitudEnviada -> {
                // Handled by dialog above; show loading while transitioning
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
        }
    }
}

@Composable
private fun TrainerCard(trainer: UserHelper.UserInfo, onSolicitar: () -> Unit) {
    val doc       = trainer.documento
    val poblacion = doc?.getString("poblacion")
    val municipio = doc?.getString("municipio")
    val sobreMi   = doc?.getString("sobreMi")
    val tarifa    = doc?.getString("tarifa")

    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    trainer.nombre ?: "Trainer",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!poblacion.isNullOrBlank() || !municipio.isNullOrBlank()) {
                    val ubicacion = listOfNotNull(poblacion, municipio).joinToString(", ")
                    Text(ubicacion, fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp))
                }
                if (!sobreMi.isNullOrBlank()) {
                    Text(
                        sobreMi.take(80) + if (sobreMi.length > 80) "…" else "",
                        fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (!tarifa.isNullOrBlank()) {
                    Text("$tarifa €/sesión", fontSize = 12.sp, color = AccentGreen,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onSolicitar,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("Conectar", fontSize = 13.sp) }
        }
    }
}
