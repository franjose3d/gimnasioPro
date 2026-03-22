package com.example.gimnasiopro.presentation.entrenamiento

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.gimnasiopro.ads.InterstitialAdManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.*
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoScreen(
    viewModel: EntrenamientoViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity

    // Pre-cargar el interstitial al entrar a la pantalla
    LaunchedEffect(Unit) {
        InterstitialAdManager.preload(context)
    }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) {
            viewModel.clearNavigateBack()
            if (activity != null) {
                InterstitialAdManager.showIfReady(activity) { onNavigateBack() }
            } else {
                onNavigateBack()
            }
        }
    }

    if (state.showConfirmSalir) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmSalir() },
            title = { Text("Volver") },
            text = { Text("¿Seguro que quieres salir? Se perderá el progreso no guardado.") },
            confirmButton = {
                TextButton(onClick = { viewModel.salirSinGuardar() }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmSalir() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.confirmarSalir() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
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

            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(state.ejercicios) { index, ej ->
                            EjercicioEntrenamientoCard(
                                item = ej,
                                ejercicioIndex = index,
                                viewModel = viewModel,
                                onAbrirVideo = { nombre ->
                                    try {
                                        val query = URLEncoder.encode("$nombre ejercicio gym", "UTF-8")
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                                        )
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }

                    // Finalize button
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Button(
                            onClick = { viewModel.finalizar() },
                            enabled = !state.isFinalizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            if (state.isFinalizing) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Finalizar entrenamiento", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EjercicioEntrenamientoCard(
    item: EjercicioEntrenamientoState,
    ejercicioIndex: Int,
    viewModel: EntrenamientoViewModel,
    onAbrirVideo: (String) -> Unit
) {
    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (item.completado) 0.6f else 1f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            // Header row: checkbox (left) + name + video button (right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = item.completado,
                    onCheckedChange = { viewModel.toggleCompletado(ejercicioIndex) },
                    colors = CheckboxDefaults.colors(checkedColor = AccentGreen, uncheckedColor = AccentGreen),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = item.ejercicio.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AccentBlue,
                    textDecoration = if (item.completado) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = { onAbrirVideo(item.ejercicio.nombre) },
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

            HorizontalDivider(color = AccentGreen, modifier = Modifier.padding(vertical = 4.dp))

            // Column headers
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Serie", fontSize = 11.sp, color = AccentBlue, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(28.dp)) // − reps
                Text("Rep.", fontSize = 11.sp, color = AccentBlue, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(28.dp)) // + reps
                Spacer(Modifier.width(8.dp))  // separador
                Spacer(Modifier.width(28.dp)) // − kg
                Text("Kg.", fontSize = 11.sp, color = AccentBlue, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(28.dp)) // + kg
                Spacer(Modifier.width(24.dp)) // delete
            }

            // Series rows
            for (si in 0 until item.seriesVisibles) {
                SerieRow(
                    serieIndex = si,
                    serie = item.series.getOrElse(si) { SerieState() },
                    ejercicioIndex = ejercicioIndex,
                    isLast = si == item.seriesVisibles - 1,
                    canRemove = item.seriesVisibles > 1,
                    viewModel = viewModel
                )
            }

            // Add serie button
            if (item.seriesVisibles < 6) {
                TextButton(
                    onClick = { viewModel.addSerie(ejercicioIndex) },
                    modifier = Modifier.align(Alignment.End).height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("+ Serie", color = AccentBlue, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SerieRow(
    serieIndex: Int,
    serie: SerieState,
    ejercicioIndex: Int,
    isLast: Boolean,
    canRemove: Boolean,
    viewModel: EntrenamientoViewModel
) {
    var showPesoDialog by remember(ejercicioIndex, serieIndex) { mutableStateOf(false) }
    var showRepsDialog by remember(ejercicioIndex, serieIndex) { mutableStateOf(false) }

    if (showPesoDialog) {
        var inputVal by remember { mutableStateOf(
            if (serie.pesoKg % 1 == 0f) serie.pesoKg.toInt().toString() else serie.pesoKg.toString()
        )}
        AlertDialog(
            onDismissRequest = { showPesoDialog = false },
            title = { Text("Ingresar peso") },
            text = {
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    label = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePeso(ejercicioIndex, serieIndex, inputVal.toFloatOrNull() ?: serie.pesoKg)
                    showPesoDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showPesoDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showRepsDialog) {
        var inputVal by remember { mutableStateOf(serie.repeticiones.toString()) }
        AlertDialog(
            onDismissRequest = { showRepsDialog = false },
            title = { Text("Repeticiones") },
            text = {
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateReps(ejercicioIndex, serieIndex, inputVal.toIntOrNull() ?: serie.repeticiones)
                    showRepsDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showRepsDialog = false }) { Text("Cancelar") } }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Serie number
        Text(
            "${serieIndex + 1}",
            fontSize = 12.sp,
            color = AccentBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(30.dp)
        )

        // Reps controls (flat)
        SmallIconButton("-") { viewModel.decrementReps(ejercicioIndex, serieIndex) }
        TextButton(
            onClick = { showRepsDialog = true },
            modifier = Modifier.width(28.dp).requiredHeight(28.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("${serie.repeticiones}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        SmallIconButton("+") { viewModel.incrementReps(ejercicioIndex, serieIndex) }

        Spacer(Modifier.width(8.dp))

        // Peso controls (flat)
        SmallIconButton("-") { viewModel.decrementPeso(ejercicioIndex, serieIndex) }
        TextButton(
            onClick = { showPesoDialog = true },
            modifier = Modifier.width(36.dp).requiredHeight(30.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            val pesoStr = if (serie.pesoKg % 1 == 0f) serie.pesoKg.toInt().toString()
                          else String.format("%.1f", serie.pesoKg)
            Text(pesoStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        SmallIconButton("+") { viewModel.incrementPeso(ejercicioIndex, serieIndex) }

        // Remove serie button (only on last row and if canRemove)
        if (isLast && canRemove) {
            IconButton(
                onClick = { viewModel.removeSerie(ejercicioIndex) },
                modifier = Modifier.requiredSize(30.dp)
            ) {
                Text("−", fontSize = 16.sp, color = RedDelete)
            }
        } else {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun SmallIconButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.requiredSize(28.dp),
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
