package com.example.gimnasiopro.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasiopro.ui.theme.AccentBlue
import com.example.gimnasiopro.ui.theme.DividerColor
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import com.example.gimnasiopro.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetDialog     by remember { mutableStateOf(false) }
    var showNotVerifiedDialog by remember { mutableStateOf<String?>(null) }

    // React to state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.EmailNotVerified -> showNotVerifiedDialog = state.email
            else -> Unit
        }
    }

    // ── Password reset dialog ─────────────────────────────────────────────
    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                viewModel.resetState()
            },
            title = { Text("Recuperar contraseña") },
            text = {
                Column {
                    Text("Ingresa tu email y te enviaremos un enlace para restablecer tu contraseña.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (uiState is LoginUiState.PasswordResetSent) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Email enviado. Revisa tu bandeja.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.sendPasswordReset(resetEmail) }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetState()
                }) { Text("Cancelar") }
            }
        )
    }

    // ── Email not verified dialog ─────────────────────────────────────────
    showNotVerifiedDialog?.let { notVerifiedEmail ->
        AlertDialog(
            onDismissRequest = {
                showNotVerifiedDialog = null
                viewModel.resetState()
            },
            title = { Text("Email no verificado") },
            text = {
                Text(
                    "Debes verificar tu email antes de iniciar sesión.\n\n" +
                    "Revisa tu correo ($notVerifiedEmail) y haz click en el enlace de verificación.\n\n" +
                    "¿Quieres que te reenviemos el email?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotVerifiedDialog = null
                    viewModel.resendVerificationEmail()
                }) { Text("Reenviar email") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotVerifiedDialog = null
                    viewModel.resetState()
                }) { Text("Cancelar") }
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────
    val isLoading = uiState is LoginUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Login",
            modifier = Modifier.size(120.dp),
            tint = AccentBlue
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Iniciar Sesión",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Bienvenido de nuevo",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Email field
        Text(
            text = "Email",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ejemplo@gmail.com") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DividerColor
            )
        )

        Spacer(Modifier.height(16.dp))

        // Password field
        Text(
            text = "Contraseña",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tu contraseña") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (!isLoading) viewModel.login(email, password)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DividerColor
            )
        )

        // Forgot password
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.End),
            enabled = !isLoading
        ) {
            Text("¿Olvidaste tu contraseña?", color = AccentBlue, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        // Error message
        if (uiState is LoginUiState.Error) {
            Text(
                text = (uiState as LoginUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 8.dp),
                color = AccentBlue
            )
            Spacer(Modifier.height(8.dp))
        }

        // Login button
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.login(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("INICIAR SESIÓN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(24.dp))

        // Register row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿No tienes cuenta? ", fontSize = 14.sp, color = TextSecondary)
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    "Regístrate",
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Al iniciar sesión aceptas nuestros términos y condiciones",
            fontSize = 11.sp,
            color = TextSecondary,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    GimnasioProTheme {
        LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {})
    }
}
