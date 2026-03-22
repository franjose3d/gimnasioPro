package com.example.gimnasiopro.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.*

@Composable
fun VerificarEmailScreen(
    viewModel: VerificarEmailViewModel = viewModel(),
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email = viewModel.userEmail

    LaunchedEffect(uiState) {
        if (uiState is VerificarEmailUiState.Verified) onVerified()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email",
            modifier = Modifier.size(80.dp),
            tint = AccentBlue
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Verifica tu email",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Hemos enviado un enlace de verificación a:",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = email,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Status indicator
        val (statusText, statusColor) = when (uiState) {
            is VerificarEmailUiState.Checking      -> "Comprobando..." to AccentBlue
            is VerificarEmailUiState.Verified      -> "Email verificado" to AccentGreen
            is VerificarEmailUiState.ResendSuccess -> "Email reenviado" to AccentGreen
            is VerificarEmailUiState.Error         -> (uiState as VerificarEmailUiState.Error).message to RedDelete
            else                                   -> "Esperando verificación..." to TextSecondary
        }
        Text(
            text = statusText,
            fontSize = 14.sp,
            color = statusColor,
            textAlign = TextAlign.Center
        )

        if (uiState is VerificarEmailUiState.Checking) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AccentBlue)
        }

        Spacer(Modifier.height(32.dp))

        // Manual check button
        Button(
            onClick = { viewModel.checkVerification() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is VerificarEmailUiState.Checking,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("YA VERIFIQUÉ MI EMAIL", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // Resend button
        OutlinedButton(
            onClick = { viewModel.resendVerification() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("REENVIAR EMAIL DE VERIFICACIÓN")
        }

        Spacer(Modifier.height(16.dp))

        // Sign out
        TextButton(onClick = {
            viewModel.signOut()
            onSignOut()
        }) {
            Text("Cerrar sesión", color = TextSecondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    GimnasioProTheme {
        VerificarEmailScreen(onVerified = {}, onSignOut = {})
    }
}
