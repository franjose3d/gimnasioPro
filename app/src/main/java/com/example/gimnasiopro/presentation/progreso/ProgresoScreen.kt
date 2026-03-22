package com.example.gimnasiopro.presentation.progreso

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    viewModel: ProgresoViewModel = viewModel(),
    onNavigateBack: () -> Unit
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
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = AccentBlue)
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

            Spacer(Modifier.height(32.dp))
        }
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
