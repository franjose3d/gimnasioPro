package com.example.gimnasiopro.presentation.listaejercictos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEjerciciosScreen(
    viewModel: ListaEjerciciosViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onBuscar: () -> Unit,
    onRequestLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Dialogs
    var showAgregarDialog by remember { mutableStateOf(false) }
    var agregarGrupo by remember { mutableStateOf(state.grupoMuscular) }
    var agregarNombre by remember { mutableStateOf("") }
    var agregarDesc by remember { mutableStateOf("") }
    var agregarGrupoSecundario by remember { mutableStateOf("") }
    var eliminarTarget by remember { mutableStateOf<Ejercicio?>(null) }
    var showRutinaDialog by remember { mutableStateOf(false) }
    var rutinasInfo by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    // Back with selection warning
    var showBackDialog by remember { mutableStateOf(false) }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("Cancelar selección") },
            text = { Text("¿Deseas cancelar la selección actual?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSelection()
                    showBackDialog = false
                    onNavigateBack()
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) { Text("No") }
            }
        )
    }

    // Add exercise dialog (trainer only)
    if (showAgregarDialog) {
        AlertDialog(
            onDismissRequest = { showAgregarDialog = false },
            title = { Text("Agregar Ejercicio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var grupoExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = grupoExpanded,
                        onExpandedChange = { grupoExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = agregarGrupo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grupo muscular") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = grupoExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = grupoExpanded, onDismissRequest = { grupoExpanded = false }) {
                            ListaEjerciciosViewModel.GRUPOS_MUSCULARES.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = { agregarGrupo = g; grupoExpanded = false }
                                )
                            }
                        }
                    }
                    // Grupo muscular secundario (opcional)
                    var grupoSecExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = grupoSecExpanded,
                        onExpandedChange = { grupoSecExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = agregarGrupoSecundario,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grupo secundario (opcional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = grupoSecExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = grupoSecExpanded, onDismissRequest = { grupoSecExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("— Ninguno —") },
                                onClick = { agregarGrupoSecundario = ""; grupoSecExpanded = false }
                            )
                            ListaEjerciciosViewModel.GRUPOS_MUSCULARES.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = { agregarGrupoSecundario = g; grupoSecExpanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = agregarNombre,
                        onValueChange = { agregarNombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = agregarDesc,
                        onValueChange = { if (it.length <= 200) agregarDesc = it },
                        label = { Text("Descripción") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.agregarEjercicio(
                        agregarGrupo,
                        agregarNombre.trim(),
                        agregarDesc.trim(),
                        agregarGrupoSecundario.ifBlank { null }
                    )
                    agregarNombre = ""; agregarDesc = ""; agregarGrupoSecundario = ""
                    showAgregarDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAgregarDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Delete local dialog
    eliminarTarget?.let { ej ->
        AlertDialog(
            onDismissRequest = { eliminarTarget = null },
            title = { Text("Eliminar ejercicio") },
            text = { Text("¿Eliminar \"${ej.nombre}\" de este dispositivo?\n\nSolo se borra en local. Puedes volver a cargarlo desde la nube cuando quieras.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarLocal(ej)
                    eliminarTarget = null
                }) { Text("Eliminar", color = RedDelete) }
            },
            dismissButton = {
                TextButton(onClick = { eliminarTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Select rutina dialog
    if (showRutinaDialog) {
        AlertDialog(
            onDismissRequest = { showRutinaDialog = false },
            title = { Text("Selecciona una rutina") },
            text = {
                LazyColumn {
                    items(rutinasInfo) { (num, count) ->
                        val estado = when {
                            count >= 10 -> " (LLENA)"
                            count > 0   -> " ($count/10 - ${10 - count} disponibles)"
                            else        -> " (vacía)"
                        }
                        TextButton(
                            onClick = {
                                viewModel.guardarEnRutina(num)
                                showRutinaDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rutina $num$estado", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRutinaDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.grupoMuscular, fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedIds.isNotEmpty()) showBackDialog = true else onNavigateBack()
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    IconButton(onClick = onBuscar) {
                        Icon(Icons.Default.Search, "Buscar ejercicios", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (uid == null) onRequestLogin() else viewModel.cargarDesdeFirebase()
                    },
                    enabled = !state.isCargandoFirebase,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isCargandoFirebase) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cargar ejercicios", fontSize = 13.sp)
                    }
                }
                if (state.isTrainer) {
                    Button(
                        onClick = { showAgregarDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Nuevo", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }

                state.ejercicios.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay ejercicios disponibles.\nPulsa \"Cargar ejercicios\" para obtenerlos.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                else -> {
                    // Selection bar
                    if (state.selectedIds.isNotEmpty()) {
                        Surface(color = AccentGreen.copy(alpha = 0.15f)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${state.selectedIds.size}/10 seleccionados",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (viewModel.numRutina > 0) {
                                            viewModel.guardarEnRutina(viewModel.numRutina)
                                        } else {
                                            scope.launch {
                                                rutinasInfo = viewModel.getRutinaInfo()
                                                showRutinaDialog = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 0.dp)
                                ) {
                                    Text(
                                        "Guardar",
                                        color = TextPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                            EjercicioListaItem(
                                ejercicio = ejercicio,
                                selected = ejercicio.id in state.selectedIds,
                                onToggle = { viewModel.toggleSelection(ejercicio.id) },
                                onDelete = { eliminarTarget = ejercicio }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EjercicioListaItem(
    ejercicio: Ejercicio,
    selected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = if (selected) AccentGreen.copy(alpha = 0.15f) else CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onDelete)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ejercicio.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (ejercicio.descripcion.isNotBlank()) {
                    Text(
                        ejercicio.descripcion,
                        fontSize = 12.sp,
                        color = TextSecondary,
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
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar ejercicio",
                    tint = RedDelete.copy(alpha = 0.7f)
                )
            }
        }
    }
}
