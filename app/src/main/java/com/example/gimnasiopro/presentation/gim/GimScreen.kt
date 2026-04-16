package com.example.gimnasiopro.presentation.gim

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gimnasiopro.components.CalendarView
import com.example.gimnasiopro.data.RutinaDiaSemana
import com.example.gimnasiopro.ui.theme.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GimScreen(
    viewModel: GimViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateDetalle: (numeroRutina: Int) -> Unit,
    onNavigateRutinas: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(state.navigateToDetalle) {
        state.navigateToDetalle?.let { num ->
            viewModel.clearNavigateToDetalle()
            onNavigateDetalle(num)
        }
    }

    LaunchedEffect(state.navigateToRutinas) {
        if (state.navigateToRutinas) {
            viewModel.clearNavigateToRutinas()
            onNavigateRutinas()
        }
    }

    // QR Scanner overlay (fullscreen)
    if (state.showQrScanner) {
        QrScannerOverlay(
            onCodigoDetectado = { codigo -> viewModel.onCodigoQrDetectado(codigo) },
            onDismiss = { viewModel.onDismissQrScanner() }
        )
        return
    }

    // Vincular gimnasio dialog
    if (state.showVincularDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissVincularDialog() },
            title = { Text("Vincular a tu gimnasio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Introduce el código que te dará la recepción del gimnasio.",
                        fontSize = 14.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = state.codigoInput,
                        onValueChange = { viewModel.onCodigoChange(it) },
                        label = { Text("Código de acceso") },
                        singleLine = true,
                        isError = state.errorCodigo != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.errorCodigo != null) {
                        Text(state.errorCodigo!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.onDismissVincularDialog(); viewModel.onMostrarQrScanner() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear QR")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.vincularPorCodigo() },
                    enabled = !state.isVinculando
                ) {
                    if (state.isVinculando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Vincular")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissVincularDialog() }) { Text("Cancelar") }
            }
        )
    }

    // Rutina picker dialog
    state.showRutinaPickerDia?.let { diaSemana ->
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        AlertDialog(
            onDismissRequest = { viewModel.dismissRutinaPicker() },
            title = { Text("Elige una rutina para los $nombreDia") },
            text = {
                LazyColumn {
                    items(state.rutinasDisponibles) { rutina ->
                        val estado = if (rutina.ejercicioIds.isNotEmpty()) " ✓" else " (vacía)"
                        TextButton(
                            onClick = { viewModel.onRutinaSeleccionadaParaDia(diaSemana, rutina.numeroRutina) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${rutina.nombre}$estado",
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider()
                    }
                    item {
                        TextButton(
                            onClick = { viewModel.dismissRutinaPicker(); onNavigateRutinas() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➕ Crear nueva rutina", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRutinaPicker() }) { Text("Cancelar") }
            }
        )
    }

    // Options dialog (long press)
    state.showOpcionesDia?.let { diaSemana ->
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)
        AlertDialog(
            onDismissRequest = { viewModel.dismissOpcionesDia() },
            title = { Text("Rutina de los $nombreDia") },
            text = {
                Column {
                    TextButton(
                        onClick = { viewModel.cambiarRutinaDia(diaSemana) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("✏️ Cambiar rutina", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = { viewModel.eliminarRutinaDia(diaSemana) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🗑️ Eliminar rutina", color = RedDelete, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOpcionesDia() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GYM", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentBlue)
                        val gymNombre = state.gimnasio?.nombre
                        if (!gymNombre.isNullOrBlank()) {
                            Text(gymNombre, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Volver",
                            tint = AccentGreen, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                expandedHeight = 48.dp
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar via AndroidView
            AndroidCalendarView(
                diasConRutina = state.diasConRutina,
                onDateSelected = { y, m, d -> viewModel.onDiaSeleccionado(y, m, d) },
                onDateLongPress = { _, _, _, diaSemana -> viewModel.onDiaLongPress(diaSemana) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.verificarRutinaDelDia() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "MI-RUTINA",
                        color = TextOnButton,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { viewModel.onNuevaRutina() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "NUEVA-RUTINA",
                        color = TextOnButton,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Gimnasio section ─────────────────────────────────────────────
            GimnasioCard(
                gimnasio = state.gimnasio,
                onVincular = { viewModel.onMostrarVincularDialog() },
                onDesvincular = { viewModel.desvincularGimnasio() }
            )

            Spacer(Modifier.weight(1f))

            // Banner AdMob
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

// ── Gimnasio card ─────────────────────────────────────────────────────────────

@Composable
private fun GimnasioCard(
    gimnasio: com.example.gimnasiopro.data.firestore.GimnasioFirestore?,
    onVincular: () -> Unit,
    onDesvincular: () -> Unit
) {
    val context = LocalContext.current
    var showOpciones by remember { mutableStateOf(false) }
    var showConfirmDesvincular by remember { mutableStateOf(false) }

    // Dialog opciones del gimnasio
    if (showOpciones && gimnasio != null) {
        AlertDialog(
            onDismissRequest = { showOpciones = false },
            title = { Text(gimnasio.nombre, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (gimnasio.webUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                showOpciones = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gimnasio.webUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.util.Log.e("GimScreen", "Error abriendo web: ${e.message}")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌐  Página web", textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider()
                    }
                    if (gimnasio.direccion.isNotBlank() || gimnasio.mapsQuery.isNotBlank()) {
                        TextButton(
                            onClick = {
                                showOpciones = false
                                val query = gimnasio.mapsQuery.ifBlank { gimnasio.direccion }
                                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📍  Cómo llegar", textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider()
                    }
                    TextButton(
                        onClick = { showOpciones = false; showConfirmDesvincular = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔓  Desvincularme", color = RedDelete,
                            textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOpciones = false }) { Text("Cerrar") }
            }
        )
    }

    // Confirm desvincular
    if (showConfirmDesvincular) {
        AlertDialog(
            onDismissRequest = { showConfirmDesvincular = false },
            title = { Text("Desvincular gimnasio") },
            text = { Text("¿Seguro que quieres desvincular ${gimnasio?.nombre ?: "este gimnasio"}?") },
            confirmButton = {
                Button(
                    onClick = { showConfirmDesvincular = false; onDesvincular() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDelete)
                ) { Text("Desvincular") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDesvincular = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(
        color = CardDark,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (gimnasio != null) showOpciones = true }
    ) {
        if (gimnasio != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                if (gimnasio.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = gimnasio.logoUrl,
                        contentDescription = "Logo ${gimnasio.nombre}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        gimnasio.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (gimnasio.direccion.isNotBlank()) {
                        Text(
                            gimnasio.direccion,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                // Flecha indicando que es clickable
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
            }
        } else {
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sin gimnasio vinculado", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onVincular,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Vincular a mi gimnasio", fontSize = 13.sp, color = TextOnButton)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "¿Quieres que tu gimnasio aparezca en la app?",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:gimnasiopro1973@gmail.com"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "gimnasiopro1973@gmail.com",
                        fontSize = 12.sp,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}

// ── QR Scanner overlay ────────────────────────────────────────────────────────

@Composable
private fun QrScannerOverlay(
    onCodigoDetectado: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }
            val scanned = remember { AtomicBoolean(false) }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                var cameraProvider: ProcessCameraProvider? = null

                cameraProviderFuture.addListener({
                    cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        @OptIn(ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !scanned.get()) {
                            val image = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (!scanned.getAndSet(true) && barcodes.isNotEmpty()) {
                                        val raw = barcodes.first().rawValue ?: ""
                                        if (raw.isNotBlank()) onCodigoDetectado(raw)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider?.unbindAll()
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Error cámara: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(context))

                onDispose { cameraProvider?.unbindAll() }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Botón cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // Marco guía QR
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            )

            Text(
                "Apunta al código QR del gimnasio",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp)
            )

        } else {
            // Sin permiso
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Se necesita permiso de cámara", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Conceder permiso")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color.White)
                }
            }
        }
    }
}

// ── Privados ──────────────────────────────────────────────────────────────────

@Composable
private fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-2121593613571802/9823659590"
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AndroidCalendarView(
    diasConRutina: Set<Int>,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDateLongPress: (Int, Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            CalendarView(context).apply {
                setOnDateSelectedListener(object : CalendarView.OnDateSelectedListener {
                    override fun onDateSelected(year: Int, month: Int, day: Int) {
                        onDateSelected(year, month, day)
                    }
                })
                setOnDateLongPressListener(object : CalendarView.OnDateLongPressListener {
                    override fun onDateLongPress(year: Int, month: Int, day: Int, diaSemana: Int) {
                        onDateLongPress(year, month, day, diaSemana)
                    }
                })
            }
        },
        update = { calendarView ->
            calendarView.setDiasConRutina(diasConRutina)
        },
        modifier = modifier
    )
}
