package com.example.gimnasiopro.presentation.progreso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.data.EjercicioConRecord
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisEjerciciosScreen(
    modoEliminar: Boolean = false,
    viewModel: MisEjerciciosViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(modoEliminar) {
        viewModel.setModoEliminar(modoEliminar)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.modoEliminar) "Eliminar ejercicios" else "Mis Ejercicios",
                        fontWeight = FontWeight.Bold,
                        color = if (state.modoEliminar) MaterialTheme.colorScheme.error else AccentBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    if (!state.modoEliminar) {
                        IconButton(onClick = { viewModel.load() }) {
                            Icon(Icons.Default.Refresh, "Actualizar", tint = AccentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (state.modoEliminar && state.seleccionados.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.confirmarEliminar() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Delete, null) },
                    text = { Text("Eliminar ${state.seleccionados.size}", fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when {
            state.isLoading || state.isDeleting -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) { Text("Reintentar") }
                    }
                }
            }
            state.ejercicios.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no has registrado ningún entrenamiento.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                if (state.modoEliminar) {
                    Text(
                        "Selecciona los ejercicios que quieres eliminar",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .padding(padding)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(
                        top = if (state.modoEliminar) 40.dp else 16.dp,
                        bottom = if (state.modoEliminar) 96.dp else 16.dp
                    )
                ) {
                    val grupos = state.ejercicios.groupBy { it.grupoMuscular }
                    grupos.forEach { (grupo, ejercicios) ->
                        item {
                            Text(
                                grupo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        item {
                            Surface(
                                color = CardDark,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    ejercicios.forEachIndexed { index, ejercicio ->
                                        EjercicioHistorialRow(
                                            ejercicio    = ejercicio,
                                            modoEliminar = state.modoEliminar,
                                            seleccionado = ejercicio.ejercicioId in state.seleccionados,
                                            onToggle     = { viewModel.toggleSeleccion(ejercicio.ejercicioId) }
                                        )
                                        if (index < ejercicios.lastIndex) {
                                            HorizontalDivider(
                                                color = DividerColor,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showConfirmacion) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarEliminar() },
            title = { Text("¿Confirmar borrado?") },
            text = {
                Text(
                    "Se eliminarán los registros de ${state.seleccionados.size} ejercicio(s). " +
                    "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.ejecutarEliminar() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarEliminar() }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EjercicioHistorialRow(
    ejercicio: EjercicioConRecord,
    modoEliminar: Boolean = false,
    seleccionado: Boolean = false,
    onToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (modoEliminar) Modifier.clickable { onToggle() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (modoEliminar) {
            Checkbox(
                checked = seleccionado,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
            )
        }
        Text(
            ejercicio.nombre,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seleccionado && modoEliminar) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (!modoEliminar) {
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
}
