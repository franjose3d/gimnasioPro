package com.example.gimnasiopro.presentation.trainer

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gimnasiopro.components.TerminosDialog
import com.example.gimnasiopro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTrainerScreen(
    viewModel: RegisterTrainerViewModel = viewModel(),
    onRegistroExitoso: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Form state
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var nombre          by remember { mutableStateOf("") }
    var dni             by remember { mutableStateOf("") }
    var poblacion       by remember { mutableStateOf("") }
    var municipio       by remember { mutableStateOf("") }
    var telefono        by remember { mutableStateOf("") }
    var sobreMi         by remember { mutableStateOf("") }
    var tarifa          by remember { mutableStateOf("") }
    var fotoUri         by remember { mutableStateOf<Uri?>(null) }
    var certUri         by remember { mutableStateOf<Uri?>(null) }
    var certNombre      by remember { mutableStateOf("") }
    var terminosChecked by remember { mutableStateOf(false) }

    var showTerminosDialog by remember { mutableStateOf(false) }
    var passwordMismatch   by remember { mutableStateOf(false) }
    var terminosError      by remember { mutableStateOf(false) }

    // Launchers
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { fotoUri = it }
    }
    val certLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            certUri = it
            certNombre = it.lastPathSegment ?: "Archivo seleccionado"
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(uiState) {
        if (uiState is RegisterTrainerUiState.Success) onRegistroExitoso()
    }

    if (showTerminosDialog) {
        TerminosDialog(
            onAccept = {
                terminosChecked = true
                terminosError = false
                showTerminosDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { terminosChecked = false; showTerminosDialog = false }
        )
    }

    val isLoading = uiState is RegisterTrainerUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro Entrenador", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Photo picker
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .clickable { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(fotoUri),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, "Foto", tint = TextSecondary, modifier = Modifier.size(48.dp))
                }
            }
            Text("Toca para seleccionar foto", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

            // Auth
            FormLabel("Email")
            FormField(email, { email = it }, "ejemplo@gmail.com", KeyboardType.Email, enabled = !isLoading)
            FormLabel("Contraseña")
            FormField(password, { password = it }, "Mínimo 6 caracteres", KeyboardType.Password, password = true, enabled = !isLoading)
            FormLabel("Confirmar contraseña")
            FormField(passwordConfirm, { passwordConfirm = it; passwordMismatch = false }, "Repite la contraseña", KeyboardType.Password, password = true, enabled = !isLoading,
                isError = passwordMismatch, errorText = "Las contraseñas no coinciden")

            // Personal
            FormLabel("Nombre completo")
            FormField(nombre, { nombre = it }, "Tu nombre", enabled = !isLoading)
            FormLabel("DNI / NIE")
            FormField(dni, { dni = it }, "12345678A", enabled = !isLoading)
            FormLabel("Población")
            FormField(poblacion, { poblacion = it }, "Tu población", enabled = !isLoading)
            FormLabel("Municipio")
            FormField(municipio, { municipio = it }, "Tu municipio", enabled = !isLoading)
            FormLabel("Teléfono (sin prefijo +34)")
            FormField(telefono, { telefono = it }, "612345678", KeyboardType.Phone, enabled = !isLoading)
            FormLabel("Sobre mí")
            OutlinedTextField(
                value = sobreMi,
                onValueChange = { sobreMi = it },
                modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 12.dp),
                placeholder = { Text("Cuéntanos sobre ti...", color = TextHint) },
                maxLines = 4,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor)
            )
            FormLabel("Tarifa por sesión (€)")
            FormField(tarifa, { tarifa = it }, "0.00", KeyboardType.Decimal, enabled = !isLoading)

            // Certificate picker
            FormLabel("Certificado / Titulación (imagen o PDF)")
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { certLauncher.launch("*/*") },
                    enabled = !isLoading
                ) { Text("Seleccionar") }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (certNombre.isNotBlank()) "📄 $certNombre" else "Sin archivo",
                    fontSize = 13.sp,
                    color = if (certNombre.isNotBlank()) AccentBlue else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Terms
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = terminosChecked, onCheckedChange = { terminosChecked = it; terminosError = false }, enabled = !isLoading)
                Text("Acepto los ", color = TextSecondary, fontSize = 14.sp)
                Text(
                    "Términos y Condiciones",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showTerminosDialog = true }
                )
            }
            if (terminosError) {
                Text("Debes aceptar los Términos y Condiciones", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            if (uiState is RegisterTrainerUiState.Error) {
                Text(
                    (uiState as RegisterTrainerUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = AccentBlue)
            }

            Button(
                onClick = {
                    passwordMismatch = password != passwordConfirm
                    terminosError = !terminosChecked
                    if (!passwordMismatch && !terminosError) {
                        viewModel.register(
                            email = email, password = password,
                            nombre = nombre, dni = dni,
                            poblacion = poblacion, municipio = municipio,
                            telefonoSinPrefijo = telefono,
                            sobreMi = sobreMi, tarifaStr = tarifa,
                            fotoUri = fotoUri, certificadoUri = certUri
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("REGISTRARME", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text = text, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
}

@Composable
private fun FormField(
    value: String, onValueChange: (String) -> Unit, hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false, enabled: Boolean = true,
    isError: Boolean = false, errorText: String = ""
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        placeholder = { Text(hint, color = TextHint) },
        singleLine = true, enabled = enabled, isError = isError,
        supportingText = if (isError && errorText.isNotBlank()) {{ Text(errorText) }} else null,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor)
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    GimnasioProTheme {
        RegisterTrainerScreen(onRegistroExitoso = {}, onNavigateBack = {})
    }
}
