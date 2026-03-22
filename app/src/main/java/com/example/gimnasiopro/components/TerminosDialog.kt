package com.example.gimnasiopro.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gimnasiopro.R

/**
 * Reusable dialog that shows the app's Terms & Conditions from raw resource.
 *
 * @param onAccept Called when the user taps "Acepto".
 * @param onDismiss Called when the user taps "No acepto" or dismisses.
 */
@Composable
fun TerminosDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val text = remember {
        runCatching {
            context.resources.openRawResource(R.raw.terminos_condiciones)
                .bufferedReader().use { it.readText() }
        }.getOrDefault("Error al cargar los términos y condiciones.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Condiciones") },
        text = {
            Column(
                modifier = Modifier
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = text, fontSize = 13.sp, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Acepto") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No acepto") }
        }
    )
}
