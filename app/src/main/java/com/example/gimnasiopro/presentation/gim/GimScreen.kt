package com.example.gimnasiopro.presentation.gim

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.components.CalendarView
import com.example.gimnasiopro.data.RutinaDiaSemana
import com.example.gimnasiopro.ui.theme.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GimScreen(
    viewModel: GimViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateDetalle: (numeroRutina: Int) -> Unit,
    onNavigateRutinas: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(state.navigateToDetalle) {
        state.navigateToDetalle?.let { num ->
            viewModel.clearNavigateToDetalle()
            onNavigateDetalle(num)
        }
    }

    LaunchedEffect(state.navigateToRutinas) {
        if (state.navigateToRutinas) {
            viewModel.clearNavigateToRutinas()
            onNavigateRutinas()
        }
    }

    // Rutina picker dialog
    state.showRutinaPickerDia?.let { diaSemana ->
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        AlertDialog(
            onDismissRequest = { viewModel.dismissRutinaPicker() },
            title = { Text("Elige una rutina para los $nombreDia") },
            text = {
                LazyColumn {
                    items(state.rutinasDisponibles) { rutina ->
                        val estado = if (rutina.ejercicioIds.isNotEmpty()) " ✓" else " (vacía)"
                        TextButton(
                            onClick = { viewModel.onRutinaSeleccionadaParaDia(diaSemana, rutina.numeroRutina) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${rutina.nombre}$estado",
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider()
                    }
                    item {
                        TextButton(
                            onClick = { viewModel.dismissRutinaPicker(); onNavigateRutinas() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➕ Crear nueva rutina", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRutinaPicker() }) { Text("Cancelar") }
            }
        )
    }

    // Options dialog (long press)
    state.showOpcionesDia?.let { diaSemana ->
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        AlertDialog(
            onDismissRequest = { viewModel.dismissOpcionesDia() },
            title = { Text("Rutina de los $nombreDia") },
            text = {
                Column {
                    TextButton(
                        onClick = { viewModel.cambiarRutinaDia(diaSemana) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("✏️ Cambiar rutina", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = { viewModel.eliminarRutinaDia(diaSemana) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🗑️ Eliminar rutina", color = RedDelete, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOpcionesDia() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GYM", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar via AndroidView
            AndroidCalendarView(
                diasConRutina = state.diasConRutina,
                onDateSelected = { y, m, d -> viewModel.onDiaSeleccionado(y, m, d) },
                onDateLongPress = { _, _, _, diaSemana -> viewModel.onDiaLongPress(diaSemana) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.verificarRutinaDelDia() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text(
                        "MI-RUTINA",
                        color = TextOnButton,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = { viewModel.onNuevaRutina() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text(
                        "NUEVA-RUTINA",
                        color = TextOnButton,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Banner AdMob
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-2121593613571802/9823659590"
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AndroidCalendarView(
    diasConRutina: Set<Int>,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDateLongPress: (Int, Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            CalendarView(context).apply {
                setOnDateSelectedListener(object : CalendarView.OnDateSelectedListener {
                    override fun onDateSelected(year: Int, month: Int, day: Int) {
                        onDateSelected(year, month, day)
                    }
                })
                setOnDateLongPressListener(object : CalendarView.OnDateLongPressListener {
                    override fun onDateLongPress(year: Int, month: Int, day: Int, diaSemana: Int) {
                        onDateLongPress(year, month, day, diaSemana)
                    }
                })
            }
        },
        update = { calendarView ->
            calendarView.setDiasConRutina(diasConRutina)
        },
        modifier = modifier
    )
}
