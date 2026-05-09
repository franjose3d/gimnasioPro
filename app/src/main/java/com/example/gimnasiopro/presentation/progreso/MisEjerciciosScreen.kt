package com.example.gimnasiopro.presentation.progreso

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    viewModel: MisEjerciciosViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Ejercicios", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when {
            state.isLoading -> {
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
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
                                        EjercicioHistorialRow(ejercicio)
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
}

@Composable
private fun EjercicioHistorialRow(ejercicio: EjercicioConRecord) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            ejercicio.nombre,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
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
