package com.example.gimnasiopro.presentation.progreso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import com.example.gimnasiopro.data.EjercicioConRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    viewModel: ProgresoViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onMisEjercicios: () -> Unit = {},
    onEliminarEjercicios: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.titulo, fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { viewModel.toggleMenu() }) {
                            Icon(Icons.Default.MoreVert, "Opciones", tint = AccentBlue)
                        }
                        DropdownMenu(
                            expanded = state.showMenuOpciones,
                            onDismissRequest = { viewModel.toggleMenu() }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Actualizar") },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                onClick = { viewModel.toggleMenu(); viewModel.load() }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar estadísticas", color =
                                    MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint =
                                    MaterialTheme.colorScheme.error) },
                                onClick = { viewModel.mostrarDialogEliminar() }
                            )
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Estadísticas Generales ────────────────────────────────────────
            Text(
                "Estadísticas Generales",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Tiempo Entrenamiento Hoy", state.tiempoHoy, AccentBlue, Modifier.weight(1f))
                StatCard("Tiempo Total del Mes", state.tiempoMes, AccentBlue, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Entrenamientos del Mes", state.entrenamientosMes.toString(), AccentBlue, Modifier.weight(1f))
                StatCard("Racha Actual", "${state.racha} días", OrangeAccent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Peso Total Movido ─────────────────────────────────────────────
            Text(
                "Peso Total Movido",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Peso Movido Hoy", state.pesoHoy, OrangeAccent, Modifier.weight(1f))
                StatCard("Récord Personal", state.recordPeso, OrangeAccent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Equilibrio Muscular ───────────────────────────────────────────
            Text(
                "Equilibrio Muscular",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val gruposDefault = listOf(
                "Pectorales", "Espalda", "Hombros", "Bíceps y Antebrazo",
                "Tríceps", "Abdominales", "Piernas", "Glúteos", "Gemelos"
            )
            val gruposToShow = if (state.equilibrio.isNotEmpty()) {
                gruposDefault.map { it to (state.equilibrio[it] ?: 0) }
            } else {
                gruposDefault.map { it to 0 }
            }

            Surface(
                color = CardDark,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    gruposToShow.forEachIndexed { index, (grupo, pct) ->
                        MuscleRow(grupo, pct)
                        if (index < gruposToShow.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (state.historialEjercicios.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = CardDark,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMisEjercicios() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Mis Ejercicios",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AccentBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showDialogEliminar) {
        DialogEliminarEstadisticas(
            isDeleting = state.isDeleting,
            onDismiss = { viewModel.ocultarDialogEliminar() },
            onEliminar = { seccion -> viewModel.confirmarEliminar(seccion) },
            onEliminarMisEjercicios = {
                viewModel.ocultarDialogEliminar()
                onEliminarEjercicios()
            }
        )
    }

    if (state.seccionConfirmando != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarConfirmacion() },
            title = { Text("¿Confirmar borrado?") },
            text = {
                Text("Se eliminarán los datos de ${nombreSeccion(state.seccionConfirmando!!)}. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.ejecutarEliminar(state.seccionConfirmando!!) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarConfirmacion() }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, accentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun MuscleRow(grupo: String, pct: Int) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(grupo.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("$pct%", fontSize = 13.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = AccentBlue,
            trackColor = DividerColor
        )
    }
}

@Composable
private fun EjercicioRecordRow(ejercicio: EjercicioConRecord) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ejercicio.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(ejercicio.grupoMuscular, fontSize = 11.sp, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            val pesoStr = if (ejercicio.mejorPeso >= 1000)
                String.format("%.1f t", ejercicio.mejorPeso / 1000)
            else
                String.format("%.0f kg", ejercicio.mejorPeso)
            Text(pesoStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrangeAccent)
            Text("${ejercicio.mejorReps} reps", fontSize = 11.sp, color = TextSecondary)
        }
    }
}

private fun nombreSeccion(seccion: String) = when (seccion) {
    "tiempo_hoy"         -> "Tiempo de hoy"
    "tiempo_mes"         -> "Tiempo del mes"
    "entrenamientos_mes" -> "Entrenamientos del mes"
    "racha"              -> "Racha actual"
    "peso_movido"        -> "Peso movido"
    "todo"               -> "todas las estadísticas"
    else                 -> seccion
}

@Composable
private fun DialogEliminarEstadisticas(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onEliminar: (String) -> Unit,
    onEliminarMisEjercicios: () -> Unit
) {
    val secciones = listOf(
        "tiempo_hoy"         to "Tiempo de hoy",
        "tiempo_mes"         to "Tiempo del mes",
        "entrenamientos_mes" to "Entrenamientos del mes",
        "racha"              to "Racha actual",
        "peso_movido"        to "Peso movido",
        "equilibrio"         to "Equilibrio muscular",
        "todo"               to "Borrar todo"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar estadísticas") },
        text = {
            if (isDeleting) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                Column {
                    Text("Selecciona qué quieres borrar:", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    secciones.forEachIndexed { index, (clave, nombre) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(nombre, fontSize = 14.sp)
                            TextButton(
                                onClick = { onEliminar(clave) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (clave == "todo") MaterialTheme.colorScheme.error else OrangeAccent
                                )
                            ) { Text("Borrar") }
                        }
                        if (index < secciones.lastIndex) {
                            HorizontalDivider(color = DividerColor)
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mis Ejercicios", fontSize = 14.sp)
                        TextButton(
                            onClick = onEliminarMisEjercicios,
                            colors = ButtonDefaults.textButtonColors(contentColor = OrangeAccent)
                        ) { Text("Seleccionar") }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
