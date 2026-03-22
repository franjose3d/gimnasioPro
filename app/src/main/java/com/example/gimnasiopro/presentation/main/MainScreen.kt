package com.example.gimnasiopro.presentation.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gimnasiopro.ui.theme.*
import java.io.File
import java.util.Calendar

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onGim: () -> Unit,
    onEjercicios: () -> Unit,
    onRutinas: () -> Unit,
    onProgreso: () -> Unit,
    onContacta: () -> Unit,
    onNotificaciones: () -> Unit,
    onPerfil: () -> Unit,
    onLogin: () -> Unit
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Profile options dialog
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Mi cuenta") },
            text = {
                Column {
                    TextButton(
                        onClick = { showProfileDialog = false; onPerfil() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ver perfil") }
                    TextButton(
                        onClick = { showProfileDialog = false; viewModel.cerrarSesion() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cerrar sesión", color = RedDelete) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User photo
            UserAvatar(
                userId = state.userId,
                context = context,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        if (state.estaLogueado) showProfileDialog = true else onLogin()
                    }
            )

            Spacer(Modifier.width(12.dp))

            // Name
            Column(
                Modifier
                    .weight(1f)
                    .clickable {
                        if (state.estaLogueado) showProfileDialog = true else onLogin()
                    }
            ) {
                val displayName = if (state.estaLogueado) state.userName else "GUEST"
                Text(
                    text = "HOLA, $displayName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "¡A DARLE!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AccentGreen
                )
            }

            // Notifications bell with badge
            Box {
                Surface(
                    shape = CircleShape,
                    color = AccentBlue,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { if (state.estaLogueado) onNotificaciones() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Notifications, "Notificaciones",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (state.notifCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(18.dp)
                            .background(RedDelete, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.notifCount > 99) "99+" else state.notifCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // ── Racha strip ───────────────────────────────────────────────────────
        Surface(
            color = CardDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    rachaTitle(state.racha),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                DayIndicators(racha = state.racha)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Navigation grid ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavCard("GYM", Icons.Default.FitnessCenter, AccentBlue, Modifier.fillMaxWidth().weight(1f), onGim)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard("EJERCICIOS", Icons.Default.Search, AccentPurple, Modifier.weight(1f).fillMaxHeight(), onEjercicios, subtitle = "BIBLIOTECA")
                NavCard("RUTINAS", Icons.Default.FormatListBulleted, AccentGreen, Modifier.weight(1f).fillMaxHeight(), onRutinas, subtitle = "MIS PLANES")
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard("PROGRESO", Icons.Default.TrendingUp, OrangeAccent, Modifier.weight(1f).fillMaxHeight(), onProgreso, subtitle = "Estadísticas")
                NavCard("CONTACTA", Icons.Default.PersonAdd, AccentBlue, Modifier.weight(1f).fillMaxHeight(), onContacta, subtitle = "CONECTAR")
            }
        }
    }
}

// ── User avatar ───────────────────────────────────────────────────────────────

@Composable
private fun UserAvatar(userId: String, context: Context, modifier: Modifier = Modifier) {
    val localPath = remember(userId) {
        if (userId.isBlank()) null
        else context.getSharedPreferences("user_photo_prefs", Context.MODE_PRIVATE)
            .getString("foto_local_$userId", null)
    }

    Surface(
        shape = CircleShape,
        color = CardDark,
        modifier = modifier
    ) {
        if (localPath != null && File(localPath).exists()) {
            AsyncImage(
                model = File(localPath),
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.Person, "Foto",
                tint = TextSecondary,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }
    }
}

// ── Racha day indicators ──────────────────────────────────────────────────────

@Composable
private fun DayIndicators(racha: Int) {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val days = listOf(
        Calendar.MONDAY    to "L",
        Calendar.TUESDAY   to "M",
        Calendar.WEDNESDAY to "X",
        Calendar.THURSDAY  to "J",
        Calendar.FRIDAY    to "V",
        Calendar.SATURDAY  to "S",
        Calendar.SUNDAY    to "D"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dayConst, label) ->
            val isActive = racha > 0 && dayConst == today
            Surface(
                shape = CircleShape,
                color = if (isActive) AccentBlue else DividerColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (isActive) TextPrimary else TextSecondary)
                }
            }
        }
    }
}

// ── Navigation card ───────────────────────────────────────────────────────────

@Composable
private fun NavCard(
    label: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Surface(
        color = CardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                        color = TextSecondary)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun rachaTitle(racha: Int) = when (racha) {
    0    -> "Sin racha aún"
    1    -> "1 día de racha"
    else -> "$racha días de racha"
}
