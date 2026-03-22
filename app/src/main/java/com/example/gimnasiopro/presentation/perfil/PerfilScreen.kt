package com.example.gimnasiopro.presentation.perfil

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gimnasiopro.R
import com.example.gimnasiopro.ui.theme.*
import com.example.gimnasiopro.utils.UserPhotoManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onCuentaEliminada: () -> Unit
) {
    val state    by viewModel.state.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val userId   = viewModel.userId

    // Photo state
    var photoKey         by remember { mutableIntStateOf(0) }
    var cameraUri        by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAcct   by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            UserPhotoManager.guardarFotoLocal(context, userId, it)
            photoKey++
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let {
            UserPhotoManager.guardarFotoLocal(context, userId, it)
            photoKey++
        }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // React to state
    LaunchedEffect(state.cuentaEliminada) { if (state.cuentaEliminada) onCuentaEliminada() }
    LaunchedEffect(state.saveSuccess)     { if (state.saveSuccess) viewModel.clearSaveSuccess() }
    LaunchedEffect(state.needsReauth)     { if (state.needsReauth) showReauthDialog = true }

    // Photo options dialog
    if (showPhotoOptions) {
        val prefs    = context.getSharedPreferences("user_photo_prefs", android.content.Context.MODE_PRIVATE)
        val tieneFoto= prefs.getString("foto_local_$userId", null) != null ||
                       prefs.getString("foto_url_$userId", null)  != null
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Foto de perfil") },
            text  = {
                Column {
                    TextButton(onClick = { galleryLauncher.launch("image/*"); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth()) { Text("Galería") }
                    TextButton(onClick = { cameraPermLauncher.launch(Manifest.permission.CAMERA); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth()) { Text("Cámara") }
                    if (tieneFoto) TextButton(onClick = { showDeleteDialog = true; showPhotoOptions = false }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar foto", color = RedDelete) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPhotoOptions = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar foto") },
            text  = { Text("¿Eliminar tu foto de perfil?") },
            confirmButton = {
                TextButton(onClick = {
                    val prefs = context.getSharedPreferences("user_photo_prefs", android.content.Context.MODE_PRIVATE)
                    val path  = prefs.getString("foto_local_$userId", null)
                    if (path != null) File(path).delete()
                    prefs.edit().remove("foto_local_$userId").remove("foto_url_$userId").apply()
                    photoKey++
                    showDeleteDialog = false
                }) { Text("Eliminar", color = RedDelete) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteAcct) {
        AlertDialog(
            onDismissRequest = { showDeleteAcct = false },
            title = { Text("Eliminar cuenta") },
            text  = { Text("Esta acción es IRREVERSIBLE. Se eliminarán tu perfil, datos de entrenamiento y cuenta de acceso.") },
            confirmButton = { TextButton(onClick = { viewModel.eliminarCuenta(); showDeleteAcct = false }) { Text("ELIMINAR", color = RedDelete) } },
            dismissButton = { TextButton(onClick = { showDeleteAcct = false }) { Text("Cancelar") } }
        )
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            title = { Text("Reautenticación necesaria") },
            text  = { Text("Tus datos ya fueron eliminados.\n\nPara eliminar tu cuenta de autenticación, cierra sesión, vuelve a iniciar sesión y repite el proceso.") },
            confirmButton = { TextButton(onClick = { showReauthDialog = false; onCuentaEliminada() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { showReauthDialog = false }) { Text("Continuar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = AccentBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading && state.tipoUsuario.isBlank()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Profile photo
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(100.dp),
                onClick = { showPhotoOptions = true }
            ) {
                val localPath = remember(photoKey) {
                    context.getSharedPreferences("user_photo_prefs", android.content.Context.MODE_PRIVATE)
                        .getString("foto_local_$userId", null)
                }
                if (localPath != null) {
                    AsyncImage(model = File(localPath), contentDescription = "Foto", modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, "Foto", tint = TextSecondary, modifier = Modifier.fillMaxSize().padding(20.dp))
                }
            }
            Text("Mantén pulsado para cambiar", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))

            Surface(shape = MaterialTheme.shapes.small, color = CardDark, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                Text(state.tipoUsuario.uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }

            // Email (read-only)
            Text("Email", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Surface(color = CardDark, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(state.email, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
            }

            // Nombre
            PField("Nombre", state.nombre, viewModel::onNombreChange)
            PField("Teléfono", state.telefono, viewModel::onTelefonoChange, KeyboardType.Phone)

            // Trainer fields
            if (state.tipoUsuario == "trainer") {
                PField("DNI / NIE", state.dni, viewModel::onDniChange)
                PField("Población", state.poblacion, viewModel::onPoblacionChange)
                PField("Municipio", state.municipio, viewModel::onMunicipioChange)
                Text("Sobre mí", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
                OutlinedTextField(value = state.sobreMi, onValueChange = viewModel::onSobreMiChange,
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor))
                PField("Tarifa por sesión (€)", state.tarifa, viewModel::onTarifaChange, KeyboardType.Decimal)
            }

            // Cliente fields
            if (state.tipoUsuario == "cliente") {
                PField("Peso (kg)", state.peso, viewModel::onPesoChange, KeyboardType.Decimal)
                PField("Altura (cm)", state.altura, viewModel::onAlturaChange, KeyboardType.Decimal)
                PField("Edad", state.edad, viewModel::onEdadChange, KeyboardType.Number)
                PDropdown("Objetivo", state.objetivo,
                    listOf("Pérdida de peso","Ganancia muscular","Mantenimiento","Tonificación","Resistencia"),
                    viewModel::onObjetivoChange)
                PDropdown("Nivel", state.nivel,
                    listOf("Principiante","Intermedio","Avanzado"),
                    viewModel::onNivelChange)
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (state.saveSuccess) {
                Text("Perfil actualizado correctamente", color = AccentGreen, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(
                onClick = { viewModel.guardarCambios() },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextOnButton, strokeWidth = 2.dp)
                else Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
            Text("Zona peligrosa", color = RedDelete, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))

            Button(
                onClick = { showDeleteAcct = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedDelete)
            ) {
                Text("ELIMINAR MI CUENTA", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text) {
    Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}
