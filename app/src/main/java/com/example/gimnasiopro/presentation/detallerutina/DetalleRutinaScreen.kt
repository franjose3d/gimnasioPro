package com.example.gimnasiopro.presentation.detallerutina

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.ui.theme.*
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRutinaScreen(
    viewModel: DetalleRutinaViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onAgregarEjercicios: () -> Unit,
    onIniciarEntrenamiento: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    var showLimpiarDialog by remember { mutableStateOf(false) }
    var showEliminarDialog by remember { mutableStateOf(false) }

    if (showLimpiarDialog) {
        AlertDialog(
            onDismissRequest = { showLimpiarDialog = false },
            title = { Text("Limpiar rutina") },
            text = { Text("¿Quieres eliminar todos los ejercicios de esta rutina?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.limpiarRutina()
                    showLimpiarDialog = false
                }) { Text("Limpiar") }
            },
            dismissButton = {
                TextButton(onClick = { showLimpiarDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEliminarDialog) {
        AlertDialog(
            onDismissRequest = { showEliminarDialog = false },
            title = { Text("Borrar ejercicios") },
            text = { Text("¿Borrar los ${state.selectedIds.size} ejercicio(s) seleccionado(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarSeleccionados()
                    showEliminarDialog = false
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { showEliminarDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AccentBlue)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }

            state.ejercicios.isEmpty() -> {
                EmptyRutinaContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onAgregar = onAgregarEjercicios
                )
            }

            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Selection info bar
                    if (state.selectedIds.isNotEmpty()) {
                        Surface(color = AccentBlue.copy(alpha = 0.15f)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${state.selectedIds.size} seleccionado(s)",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(onClick = { viewModel.clearSelection() }) {
                                    Text("Cancelar", color = AccentBlue)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.ejercicios, key = { it.id }) { ejercicio ->
                            EjercicioDetalleItem(
                                ejercicio = ejercicio,
                                selected = ejercicio.id in state.selectedIds,
                                onToggle = { viewModel.toggleSelection(ejercicio.id) }
                            )
                        }
                    }

                    // Bottom action buttons
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onAgregarEjercicios,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                ) {
                                    Text("Añadir", color = TextOnButton, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { if (state.selectedIds.isNotEmpty()) showEliminarDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedDelete),
                                    enabled = state.selectedIds.isNotEmpty()
                                ) {
                                    Text("Borrar", color = TextOnButton, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!viewModel.esModoTrainer) {
                                Button(
                                    onClick = onIniciarEntrenamiento,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                                ) {
                                    Text("Iniciar entrenamiento", color = TextOnButton, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRutinaContent(modifier: Modifier, onAgregar: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Esta rutina no tiene ejercicios",
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAgregar,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Text("Agregar ejercicios", color = TextPrimary)
        }
    }
}

@Composable
private fun EjercicioDetalleItem(
    ejercicio: Ejercicio,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.15f) else CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AccentGreen, uncheckedColor = AccentGreen)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ejercicio.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentBlue
                )
                if (ejercicio.descripcion.isNotBlank()) {
                    Text(
                        ejercicio.descripcion,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Surface(
                onClick = {
                    try {
                        val query = URLEncoder.encode("${ejercicio.nombre} ejercicio gym", "UTF-8")
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                        )
                    } catch (_: Exception) {}
                },
                color = RedDelete,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "▶",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
