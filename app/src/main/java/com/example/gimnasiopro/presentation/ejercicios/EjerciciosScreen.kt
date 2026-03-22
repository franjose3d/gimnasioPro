package com.example.gimnasiopro.presentation.ejercicios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.gimnasiopro.R
import com.example.gimnasiopro.ui.theme.*

data class GrupoMuscular(val nombre: String, val rawRes: Int, val color: Color)

private val GRUPOS = listOf(
    GrupoMuscular("Pectorales",         R.raw.pectorales,   Color(0xFFC62828)),
    GrupoMuscular("Espalda",            R.raw.espalda,      Color(0xFF1565C0)),
    GrupoMuscular("Hombros",            R.raw.hombros,      Color(0xFFF9A825)),
    GrupoMuscular("Bíceps y Antebrazo", R.raw.biceps,       Color(0xFF2E7D32)),
    GrupoMuscular("Tríceps",            R.raw.triceps,      Color(0xFF0097A7)),
    GrupoMuscular("Abdominales",        R.raw.abdominales,  Color(0xFFE65100)),
    GrupoMuscular("Piernas",            R.raw.piernas,      Color(0xFF1A237E)),
    GrupoMuscular("Glúteos",            R.raw.gluteos,      Color(0xFFAD1457)),
    GrupoMuscular("Gemelos",            R.raw.gemelos,      Color(0xFF6A1B9A))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosScreen(
    numeroRutina: Int = -1,
    onNavigateBack: () -> Unit,
    onBuscar: () -> Unit,
    onGrupoSeleccionado: (grupoMuscular: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Grupos Musculares",
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
                    IconButton(onClick = onBuscar) {
                        Icon(Icons.Default.Search, "Buscar", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GRUPOS) { grupo ->
                MuscleCard(
                    grupo = grupo,
                    onClick = { onGrupoSeleccionado(grupo.nombre) }
                )
            }
        }
    }
}

@Composable
private fun MuscleCard(
    grupo: GrupoMuscular,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = grupo.color,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            onClick = onClick
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("android.resource://${context.packageName}/${grupo.rawRes}")
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = grupo.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.1.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            grupo.nombre,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}
