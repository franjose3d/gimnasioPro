package com.example.gimnasiopro.presentation.listaclientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
fun ListaClientesScreen(
    viewModel: ListaClientesViewModel = viewModel(),
    onVerRutinas: (clienteId: String, clienteNombre: String) -> Unit,
    onVerProgreso: (clienteId: String, clienteNombre: String) -> Unit,
    onEnviarMensaje: (clienteId: String, clienteNombre: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast  by viewModel.toast.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Confirm finalize dialog
    var clienteAFinalizar by remember { mutableStateOf<ClienteConConexion?>(null) }
    if (clienteAFinalizar != null) {
        AlertDialog(
            onDismissRequest = { clienteAFinalizar = null },
            title = { Text("Finalizar conexión") },
            text  = { Text("¿Finalizar la conexión con ${clienteAFinalizar!!.cliente.nombre ?: "este cliente"}? No podrás ver sus datos hasta volver a conectar.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.finalizarConexion(clienteAFinalizar!!)
                    clienteAFinalizar = null
                }) { Text("Finalizar", color = RedDelete, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { clienteAFinalizar = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Clientes", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadClientes() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when (val s = state) {
            is ListaClientesUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }

            is ListaClientesUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadClientes() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) { Text("Reintentar") }
                    }
                }
            }

            is ListaClientesUiState.Success -> {
                if (s.clientes.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No tienes clientes conectados", color = TextSecondary, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.clientes, key = { it.conexion.id }) { item ->
                            ClienteCard(
                                item = item,
                                onVerRutinas  = { onVerRutinas(item.cliente.userId, item.cliente.nombre ?: "") },
                                onVerProgreso = { onVerProgreso(item.cliente.userId, item.cliente.nombre ?: "") },
                                onMensaje     = { onEnviarMensaje(item.cliente.userId, item.cliente.nombre ?: "") },
                                onFinalizar   = { clienteAFinalizar = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClienteCard(
    item: ClienteConConexion,
    onVerRutinas: () -> Unit,
    onVerProgreso: () -> Unit,
    onMensaje: () -> Unit,
    onFinalizar: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar initial
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = AccentBlue.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        (item.cliente.nombre?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    item.cliente.nombre ?: "Cliente",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.cliente.email.isNullOrBlank()) {
                    Text(item.cliente.email!!, fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Options menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Opciones", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver rutinas") },
                        onClick = { menuExpanded = false; onVerRutinas() }
                    )
                    DropdownMenuItem(
                        text = { Text("Ver progreso") },
                        onClick = { menuExpanded = false; onVerProgreso() }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar mensaje") },
                        onClick = { menuExpanded = false; onMensaje() }
                    )
                    HorizontalDivider(color = DividerColor)
                    DropdownMenuItem(
                        text = { Text("Finalizar conexión", color = RedDelete) },
                        onClick = { menuExpanded = false; onFinalizar() }
                    )
                }
            }
        }
    }
}
