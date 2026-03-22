package com.example.gimnasiopro.presentation.cliente

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
fun RegisterClienteScreen(
    viewModel: RegisterClienteViewModel = viewModel(),
    onRegistroExitoso: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Form state
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var nombre          by remember { mutableStateOf("") }
    var telefono        by remember { mutableStateOf("") }
    var peso            by remember { mutableStateOf("") }
    var altura          by remember { mutableStateOf("") }
    var edad            by remember { mutableStateOf("") }
    var genero          by remember { mutableStateOf("Masculino") }
    var objetivo        by remember { mutableStateOf("Pérdida de peso") }
    var nivel           by remember { mutableStateOf("Principiante") }
    var fotoUri         by remember { mutableStateOf<Uri?>(null) }
    var terminosChecked by remember { mutableStateOf(false) }

    var showTerminosDialog by remember { mutableStateOf(false) }
    var passwordMismatch   by remember { mutableStateOf(false) }
    var terminosError      by remember { mutableStateOf(false) }

    // Launchers
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { fotoUri = it }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(uiState) {
        if (uiState is RegisterClienteUiState.Success) onRegistroExitoso()
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
            onDismiss = {
                terminosChecked = false
                showTerminosDialog = false
            }
        )
    }

    val isLoading = uiState is RegisterClienteUiState.Loading

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Registro Cliente", fontWeight = FontWeight.Bold) },
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

            // Photo
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

            // --- Auth fields ---
            FormLabel("Email")
            FormField(value = email, onValueChange = { email = it }, hint = "ejemplo@gmail.com", keyboardType = KeyboardType.Email, enabled = !isLoading)
            FormLabel("Contraseña")
            FormField(value = password, onValueChange = { password = it }, hint = "Mínimo 6 caracteres", keyboardType = KeyboardType.Password, password = true, enabled = !isLoading)
            FormLabel("Confirmar contraseña")
            FormField(value = passwordConfirm, onValueChange = { passwordConfirm = it; passwordMismatch = false }, hint = "Repite la contraseña", keyboardType = KeyboardType.Password, password = true, enabled = !isLoading,
                isError = passwordMismatch, errorText = "Las contraseñas no coinciden")

            // --- Personal data ---
            FormLabel("Nombre")
            FormField(value = nombre, onValueChange = { nombre = it }, hint = "Tu nombre completo", enabled = !isLoading)
            FormLabel("Teléfono (sin prefijo +34)")
            FormField(value = telefono, onValueChange = { telefono = it }, hint = "612345678", keyboardType = KeyboardType.Phone, enabled = !isLoading)
            FormLabel("Peso (kg) — opcional")
            FormField(value = peso, onValueChange = { peso = it }, hint = "70.0", keyboardType = KeyboardType.Decimal, enabled = !isLoading)
            FormLabel("Altura (cm) — opcional")
            FormField(value = altura, onValueChange = { altura = it }, hint = "170", keyboardType = KeyboardType.Decimal, enabled = !isLoading)
            FormLabel("Edad — opcional")
            FormField(value = edad, onValueChange = { edad = it }, hint = "25", keyboardType = KeyboardType.Number, enabled = !isLoading)

            // --- Dropdowns ---
            FormDropdown(
                label = "Género",
                selected = genero,
                options = listOf("Masculino", "Femenino", "Otro", "Prefiero no decir"),
                onSelect = { genero = it },
                enabled = !isLoading
            )
            FormDropdown(
                label = "Objetivo",
                selected = objetivo,
                options = listOf("Pérdida de peso", "Ganancia muscular", "Tonificación", "Mejorar resistencia", "Mantenimiento", "Flexibilidad", "Preparación deportiva", "Rehabilitación"),
                onSelect = { objetivo = it },
                enabled = !isLoading
            )
            FormDropdown(
                label = "Nivel",
                selected = nivel,
                options = listOf("Principiante", "Intermedio", "Avanzado"),
                onSelect = { nivel = it },
                enabled = !isLoading
            )

            Spacer(Modifier.height(8.dp))

            // --- Terms checkbox ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = terminosChecked,
                    onCheckedChange = { terminosChecked = it; terminosError = false },
                    enabled = !isLoading
                )
                Text(
                    text = "Acepto los ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Términos y Condiciones",
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

            // Error
            if (uiState is RegisterClienteUiState.Error) {
                Text(
                    text = (uiState as RegisterClienteUiState.Error).message,
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
                            email = email,
                            password = password,
                            nombre = nombre,
                            telefonoSinPrefijo = telefono,
                            peso = peso.toDoubleOrNull() ?: 0.0,
                            altura = altura.toDoubleOrNull() ?: 0.0,
                            edad = edad.toIntOrNull() ?: 0,
                            genero = genero,
                            objetivo = objetivo,
                            nivel = nivel,
                            fotoUri = fotoUri
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

// ─── Reusable helpers ────────────────────────────────────────────────────────

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        placeholder = { Text(hint, color = TextHint) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError && errorText.isNotBlank()) {{ Text(errorText) }} else null,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = DividerColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    FormLabel(label)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DividerColor
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    GimnasioProTheme {
        RegisterClienteScreen(onRegistroExitoso = {}, onNavigateBack = {})
    }
}
