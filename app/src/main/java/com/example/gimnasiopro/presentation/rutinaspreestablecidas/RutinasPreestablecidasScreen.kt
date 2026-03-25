package com.example.gimnasiopro.presentation.rutinaspreestablecidas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasPreestablecidasScreen(
    viewModel: RutinasPreestablecidasViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Diálogo de selección de slot
    var rutinaParaCargar by remember { mutableStateOf<RutinaPreestablecida?>(null) }
    var numeroSlots by remember { mutableIntStateOf(1) }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Cuando el usuario pulsa "Usar rutina" obtenemos el nº de slots disponibles
    LaunchedEffect(rutinaParaCargar) {
        if (rutinaParaCargar != null) {
            viewModel.obtenerNumeroSlots { slots -> numeroSlots = slots }
        }
    }

    // ── Diálogo: elegir slot ──────────────────────────────────────────────
    rutinaParaCargar?.let { plantilla ->
        SlotPickerDialog(
            plantilla    = plantilla,
            totalSlots   = numeroSlots,
            onConfirm    = { slot ->
                viewModel.cargarEnRutina(plantilla, slot)
                rutinaParaCargar = null
            },
            onDismiss    = { rutinaParaCargar = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rutinas Preestablecidas",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
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
        ) {

            // ── Filtros ───────────────────────────────────────────────────
            FiltrosBar(
                filtroNivel    = state.filtroNivel,
                filtroObjetivo = state.filtroObjetivo,
                onNivel        = { viewModel.setFiltroNivel(it) },
                onObjetivo     = { viewModel.setFiltroObjetivo(it) },
                onLimpiar      = { viewModel.limpiarFiltros() }
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (state.rutinas.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay rutinas con esos filtros",
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rutinas, key = { it.id }) { rutina ->
                        RutinaPreestablecidaCard(
                            rutina    = rutina,
                            yaUsada   = rutina.id in state.rutinasCargadas,
                            onUsar    = { rutinaParaCargar = rutina }
                        )
                    }
                }
            }
        }
    }
}

// ── Filtros horizontales ──────────────────────────────────────────────────────

@Composable
private fun FiltrosBar(
    filtroNivel: Nivel?,
    filtroObjetivo: Objetivo?,
    onNivel: (Nivel?) -> Unit,
    onObjetivo: (Objetivo?) -> Unit,
    onLimpiar: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        Text("Nivel", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Nivel.entries) { nivel ->
                val selected = filtroNivel == nivel
                FilterChip(
                    selected = selected,
                    onClick  = { onNivel(if (selected) null else nivel) },
                    label    = { Text(nivel.etiqueta, fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor     = AccentGreen,
                        selectedLabelColor         = TextOnButton
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Objetivo", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Objetivo.entries) { obj ->
                val selected = filtroObjetivo == obj
                FilterChip(
                    selected = selected,
                    onClick  = { onObjetivo(if (selected) null else obj) },
                    label    = { Text(obj.etiqueta, fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor     = TextOnButton
                    )
                )
            }
        }

        if (filtroNivel != null || filtroObjetivo != null) {
            TextButton(onClick = onLimpiar) {
                Text("✕ Limpiar filtros", color = RedDelete, fontSize = 12.sp)
            }
        }
    }
}

// ── Tarjeta de rutina ─────────────────────────────────────────────────────────

@Composable
private fun RutinaPreestablecidaCard(
    rutina:  RutinaPreestablecida,
    yaUsada: Boolean,
    onUsar:  () -> Unit
) {
    Surface(
        color  = CardDark,
        shape  = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    rutina.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = AccentBlue,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                NivelBadge(rutina.nivel)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                rutina.descripcion,
                fontSize = 13.sp,
                color    = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("🎯 ${rutina.objetivo.etiqueta}")
                InfoChip("📅 ${rutina.diasSemana} días/semana")
                InfoChip("💪 ${rutina.ejercicioIds.size} ejercicios")
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUsar,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (yaUsada) AccentBlue else AccentGreen
                ),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (yaUsada) "Volver a usar" else "Usar esta rutina",
                    color      = TextOnButton,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NivelBadge(nivel: Nivel) {
    val color = when (nivel) {
        Nivel.PRINCIPIANTE -> AccentGreen
        Nivel.INTERMEDIO   -> AccentBlue
        Nivel.AVANZADO     -> AccentPurple
        Nivel.ESPECIAL     -> RedDelete
    }
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
        Text(
            nivel.etiqueta,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize  = 11.sp,
            color     = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            text,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize  = 11.sp,
            color     = TextSecondary
        )
    }
}

// ── Diálogo selector de slot ──────────────────────────────────────────────────

@Composable
private fun SlotPickerDialog(
    plantilla:  RutinaPreestablecida,
    totalSlots: Int,
    onConfirm:  (Int) -> Unit,
    onDismiss:  () -> Unit
) {
    var slotSeleccionado by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿En qué rutina cargarla?", fontWeight = FontWeight.Bold, color = AccentBlue) },
        text  = {
            Column {
                Text(
                    "\"${plantilla.nombre}\" se cargará en el slot que elijas.\n⚠️ Los ejercicios actuales de ese slot serán reemplazados.",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Text("Selecciona la rutina destino:", fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..totalSlots).toList()) { slot ->
                        val selected = slotSeleccionado == slot
                        FilterChip(
                            selected = selected,
                            onClick  = { slotSeleccionado = slot },
                            label    = { Text("Rutina $slot") },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor     = TextOnButton
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(slotSeleccionado) },
                colors  = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Cargar", color = TextOnButton, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
