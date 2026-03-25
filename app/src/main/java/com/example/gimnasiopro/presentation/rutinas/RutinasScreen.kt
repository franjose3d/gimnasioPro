package com.example.gimnasiopro.presentation.rutinas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    viewModel: RutinasViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onRutinaSelected: (numero: Int) -> Unit,
    onRutinasClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Dialogs state
    var renombraTarget by remember { mutableStateOf<Int?>(null) }
    var renombraTexto by remember { mutableStateOf("") }
    var pegarTarget by remember { mutableStateOf<Int?>(null) }
    var menuTarget by remember { mutableStateOf<Int?>(null) }
    var decrementarConfirm by remember { mutableStateOf(false) }

    // Rename dialog
    renombraTarget?.let { numero ->
        AlertDialog(
            onDismissRequest = { renombraTarget = null },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = renombraTexto,
                    onValueChange = { renombraTexto = it },
                    label = { Text("Nombre de la rutina") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renombrar(numero, renombraTexto.trim())
                    renombraTarget = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renombraTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Paste confirmation dialog
    pegarTarget?.let { numero ->
        AlertDialog(
            onDismissRequest = { pegarTarget = null },
            title = { Text("⚠️ Confirmar pegado") },
            text = { Text("¿Pegar en Rutina $numero?\n\nEsto reemplazará los ejercicios actuales.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pegarRutina(numero)
                    pegarTarget = null
                }) { Text("Pegar rutina") }
            },
            dismissButton = {
                TextButton(onClick = { pegarTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Decrement confirmation dialog (normal mode)
    if (decrementarConfirm) {
        AlertDialog(
            onDismissRequest = { decrementarConfirm = false },
            title = { Text("Eliminar rutina") },
            text = { Text("¿Estás seguro de que quieres eliminar la última rutina? Se perderán todos sus ejercicios.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.decrementar()
                    decrementarConfirm = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { decrementarConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.titulo,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    Button(
                        onClick = onRutinasClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Rutinas",
                            color = TextOnButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (state.error != null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Error: ${state.error}",
                            color = RedDelete,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.load() }) { Text("Reintentar") }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.rutinas, key = { it.numero }) { rutinaItem ->
                        RutinaButton(
                            item = rutinaItem,
                            onClick = { onRutinaSelected(rutinaItem.numero) },
                            onLongClick = {
                                renombraTexto = rutinaItem.nombre
                                menuTarget = rutinaItem.numero
                            }
                        )

                        // Options dropdown
                        if (menuTarget == rutinaItem.numero) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { menuTarget = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✏️ Cambiar nombre") },
                                    onClick = {
                                        menuTarget = null
                                        renombraTarget = rutinaItem.numero
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📋 Copiar rutina") },
                                    onClick = {
                                        menuTarget = null
                                        viewModel.copiarRutina(rutinaItem.numero)
                                    }
                                )
                                if (state.tieneRutinaCopiada) {
                                    DropdownMenuItem(
                                        text = { Text("📥 Pegar rutina") },
                                        onClick = {
                                            menuTarget = null
                                            pegarTarget = rutinaItem.numero
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("🗑️ Limpiar ejercicios", color = RedDelete) },
                                    onClick = {
                                        menuTarget = null
                                        viewModel.limpiarRutina(rutinaItem.numero)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Counter row at bottom
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (viewModel.esModoTrainer) {
                            viewModel.decrementar()
                        } else {
                            decrementarConfirm = true
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextOnButton)
                }
                Text(
                    text = state.cantidadRutinas.toString(),
                    modifier = Modifier.padding(horizontal = 28.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Button(
                    onClick = { viewModel.incrementar() },
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextOnButton)
                }
            }
        }
    }

    // Refresh clipboard state when resuming
    LaunchedEffect(Unit) {
        viewModel.actualizarRutinaCopiada()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RutinaButton(
    item: RutinaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isNamed = !item.nombre.matches(Regex("^Rutina \\d+$", RegexOption.IGNORE_CASE))
    val bgColor = when {
        isNamed           -> AccentBlue
        else              -> AccentGreen
    }
    val textColor = TextOnButton

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = item.nombre.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
