package com.example.gimnasiopro

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.firestore.RutinaFirestore
import com.example.gimnasiopro.data.firestore.RutinaFirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RutinasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODO_TRAINER = "MODO_TRAINER"
        const val EXTRA_CLIENTE_ID = "CLIENTE_ID"
        const val EXTRA_CLIENTE_NOMBRE = "CLIENTE_NOMBRE"
        private const val TAG = "RutinasActivity"
    }

    private lateinit var rutinaRepository: RutinaRepository
    private lateinit var allRutinaButtons: List<Triple<Button, Int, LinearLayout?>>
    private lateinit var tvContadorRutinas: TextView

    private var cantidadRutinas = 10
    private val minRutinas = 1
    private val maxRutinas = 20

    // Modo trainer: acceso directo a Firestore del cliente
    private var modoTrainer = false
    private var clienteId: String? = null
    private var clienteNombre: String? = null
    private var rutinaFirestoreRepo: RutinaFirestoreRepository? = null

    // Cache de rutinas Firestore del cliente (para modo trainer)
    private var rutinasFirestoreCache: Map<Int, RutinaFirestore> = emptyMap()

    // Control para saber si el cache fue modificado desde DetalleRutinaActivity
    private var rutinasCacheModificadas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutinas)

        modoTrainer = intent.getBooleanExtra(EXTRA_MODO_TRAINER, false)
        clienteId = intent.getStringExtra(EXTRA_CLIENTE_ID)
        clienteNombre = intent.getStringExtra(EXTRA_CLIENTE_NOMBRE)

        if (modoTrainer && clienteId != null) {
            Log.d(TAG, "📋 Modo TRAINER: gestionando rutinas del cliente $clienteNombre ($clienteId)")
            rutinaFirestoreRepo = RutinaFirestoreRepository(clienteId!!, "cliente")
        }

        val database = GymDatabase.getDatabase(this)
        rutinaRepository = RutinaRepository(database.rutinaDao())

        tvContadorRutinas = findViewById(R.id.tvContadorRutinas)

        setupBackButton()
        setupRutinaButtons()
        setupIncrementDecrementButtons()
        cargarCantidadRutinas()
    }

    override fun onResume() {
        super.onResume()
        if (modoTrainer && clienteId != null) {
            // Solo recargar desde Firestore si el cache está vacío o hubo cambios
            if (rutinasFirestoreCache.isEmpty() || rutinasCacheModificadas) {
                lifecycleScope.launch {
                    cargarRutinasFirestoreCliente()
                    rutinasCacheModificadas = false
                }
            } else {
                // Redibujar desde cache sin leer Firestore
                actualizarRutinasDesdeFirestore()
            }
        } else {
            // Modo normal: sincronizar con Firebase antes de cargar
            lifecycleScope.launch {
                sincronizarRutinasYActualizar()
            }
        }
    }

    /**
     * Sincroniza rutinas con Firebase y actualiza la UI.
     * SOLO para modo normal (cliente/trainer viendo sus propias rutinas).
     */
    private suspend fun sincronizarRutinasYActualizar() {
        try {
            val app = application as GimnasioproApplication
            val rutinaHibridoRepo = app.rutinaRepositoryHibrido

            Log.d(TAG, "🔄 Sincronizando rutinas con Firebase...")
            val resultado = rutinaHibridoRepo.sincronizarRutinasConFirebase()

            resultado.onSuccess { cantidadSincronizada ->
                if (cantidadSincronizada > 0) {
                    Log.d(TAG, "✅ $cantidadSincronizada rutinas sincronizadas")
                }
            }.onFailure { error ->
                Log.w(TAG, "⚠️ Error en sincronización: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando rutinas: ${e.message}", e)
        } finally {
            // Siempre actualizar UI (aunque falle la sincronización)
            runOnUiThread { actualizarRutinas() }
        }
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        if (modoTrainer && clienteNombre != null) {
            try {
                val tvTitulo = findViewById<TextView>(R.id.tvTituloRutinas)
                tvTitulo?.text = "Rutinas de $clienteNombre"
            } catch (_: Exception) {}
        }
    }

    private fun setupRutinaButtons() {
        allRutinaButtons = listOf(
            Triple(findViewById(R.id.btnRutina1), 1, null),
            Triple(findViewById(R.id.btnRutina2), 2, null),
            Triple(findViewById(R.id.btnRutina3), 3, null),
            Triple(findViewById(R.id.btnRutina4), 4, null),
            Triple(findViewById(R.id.btnRutina5), 5, null),
            Triple(findViewById(R.id.btnRutina6), 6, null),
            Triple(findViewById(R.id.btnRutina7), 7, null),
            Triple(findViewById(R.id.btnRutina8), 8, null),
            Triple(findViewById(R.id.btnRutina9), 9, null),
            Triple(findViewById(R.id.btnRutina10), 10, null),
            Triple(findViewById(R.id.btnRutina11), 11, findViewById(R.id.rowRutinas11_12)),
            Triple(findViewById(R.id.btnRutina12), 12, findViewById(R.id.rowRutinas11_12)),
            Triple(findViewById(R.id.btnRutina13), 13, findViewById(R.id.rowRutinas13_14)),
            Triple(findViewById(R.id.btnRutina14), 14, findViewById(R.id.rowRutinas13_14)),
            Triple(findViewById(R.id.btnRutina15), 15, findViewById(R.id.rowRutinas15_16)),
            Triple(findViewById(R.id.btnRutina16), 16, findViewById(R.id.rowRutinas15_16)),
            Triple(findViewById(R.id.btnRutina17), 17, findViewById(R.id.rowRutinas17_18)),
            Triple(findViewById(R.id.btnRutina18), 18, findViewById(R.id.rowRutinas17_18)),
            Triple(findViewById(R.id.btnRutina19), 19, findViewById(R.id.rowRutinas19_20)),
            Triple(findViewById(R.id.btnRutina20), 20, findViewById(R.id.rowRutinas19_20))
        )

        allRutinaButtons.forEach { (button, numero, _) ->
            button.setOnClickListener {
                onRutinaSelected(numero)
            }
            // Long press → menú de opciones
            button.setOnLongClickListener {
                mostrarOpcionesRutina(button, numero)
                true
            }
        }

        actualizarRutinas()
    }

    private fun setupIncrementDecrementButtons() {
        findViewById<Button>(R.id.btnIncrementarRutinas).setOnClickListener {
            incrementarRutinas()
        }
        findViewById<Button>(R.id.btnDecrementarRutinas).setOnClickListener {
            decrementarRutinas()
        }
    }

    private fun cargarCantidadRutinas() {
        lifecycleScope.launch {
            if (modoTrainer && clienteId != null) {
                cargarRutinasFirestoreCliente()
            } else {
                cantidadRutinas = rutinaRepository.getCountRutinas()
                if (cantidadRutinas < minRutinas) {
                    cantidadRutinas = 10
                    rutinaRepository.initializeRutinasIfNeeded()
                }
                runOnUiThread { actualizarUIRutinas() }
            }
        }
    }

    // ==================== MENÚ DE OPCIONES (LONG PRESS) ====================

    /**
     * Muestra el menú de opciones al hacer long press en una rutina.
     * Opciones: Cambiar nombre, Eliminar, Cargar rutina guardada.
     */
    private fun mostrarOpcionesRutina(button: Button, numeroRutina: Int) {
        val opciones = arrayOf("✏️ Cambiar nombre", "🗑️ Eliminar rutina", "📥 Cargar rutina guardada")

        AlertDialog.Builder(this)
            .setTitle("Rutina $numeroRutina")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoCambiarNombre(button, numeroRutina)
                    1 -> confirmarEliminarRutina(button, numeroRutina)
                    2 -> mostrarRutinasGuardadas(numeroRutina)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== ELIMINAR RUTINA ====================

    /**
     * Muestra dialogo de confirmación para eliminar una rutina.
     * Opciones: borrar ejercicios y ocultar, o solo ocultar visualmente.
     */
    private fun confirmarEliminarRutina(button: Button, numeroRutina: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Rutina $numeroRutina")
            .setMessage("¿Quieres borrar todos los ejercicios de esta rutina?")
            .setPositiveButton("Borrar ejercicios") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val app = application as GimnasioproApplication
                        if (modoTrainer && clienteId != null) {
                            val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                            app.rutinaRepositoryHibrido.limpiarEjerciciosDeRutinaCliente(
                                clienteId!!, trainerId, numeroRutina
                            )
                            rutinasCacheModificadas = true
                            rutinasFirestoreCache = rutinasFirestoreCache.toMutableMap().apply {
                                remove(numeroRutina)
                            }
                        } else {
                            app.rutinaRepositoryHibrido.limpiarEjerciciosDeRutina(numeroRutina)
                        }
                        runOnUiThread {
                            button.text = "Rutina $numeroRutina"
                            button.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(this@RutinasActivity, R.color.rutina_vacia)
                            )
                            Toast.makeText(this@RutinasActivity, "Ejercicios eliminados", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@RutinasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // ==================== CARGAR RUTINA GUARDADA ====================

    /**
     * Muestra la lista de rutinas guardadas en Firestore para cargar en la rutina destino.
     * Permite duplicar una rutina existente.
     */
    private fun mostrarRutinasGuardadas(numeroRutinaDestino: Int) {
        lifecycleScope.launch {
            try {
                val rutinasDisponibles = if (modoTrainer && clienteId != null) {
                    // Trainer: usa cache de rutinas del cliente (ya cargadas, sin lectura extra)
                    rutinasFirestoreCache.values.toList()
                } else {
                    // Cliente: carga desde Firestore propio
                    val app = application as GimnasioproApplication
                    app.rutinaRepositoryHibrido.getRutinasFromFirebase().first()
                }

                val rutinasConEjercicios = rutinasDisponibles
                    .filter { it.ejercicioIds.isNotEmpty() }
                    .sortedBy { it.numeroRutina }

                if (rutinasConEjercicios.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RutinasActivity,
                            "No hay rutinas guardadas con ejercicios",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val nombres = rutinasConEjercicios.map { rutina ->
                    val esLaMisma = rutina.numeroRutina == numeroRutinaDestino
                    val prefijo = if (esLaMisma) "🔄 " else ""
                    "${prefijo}Rutina ${rutina.numeroRutina}: ${rutina.nombre} (${rutina.ejercicioIds.size} ejercicios)"
                }.toTypedArray()

                runOnUiThread {
                    AlertDialog.Builder(this@RutinasActivity)
                        .setTitle("Cargar en Rutina $numeroRutinaDestino\nSelecciona una rutina:")

                        .setItems(nombres) { _, index ->
                            val rutinaOrigen = rutinasConEjercicios[index]
                            confirmarCargarRutina(rutinaOrigen, numeroRutinaDestino)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando rutinas guardadas: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@RutinasActivity,
                        "Error cargando rutinas: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Confirma y ejecuta la copia de ejercicios de una rutina origen a la rutina destino.
     */
    private fun confirmarCargarRutina(rutinaOrigen: RutinaFirestore, numeroRutinaDestino: Int) {
        val esDuplicado = rutinaOrigen.numeroRutina == numeroRutinaDestino
        val mensaje = if (esDuplicado) {
            "Vas a recargar la Rutina ${rutinaOrigen.numeroRutina} con sus datos actuales.\n\nLos ejercicios serán los mismos."
        } else {
            "Vas a copiar los ejercicios de '${rutinaOrigen.nombre}' (Rutina ${rutinaOrigen.numeroRutina}) en la Rutina $numeroRutinaDestino.\n\n⚠️ Se sobrescribirán los ejercicios actuales de la Rutina $numeroRutinaDestino."
        }

        AlertDialog.Builder(this)
            .setTitle("¿Confirmar carga?")
            .setMessage(mensaje)
            .setPositiveButton("Cargar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val app = application as GimnasioproApplication
                        val nombreDestino = if (esDuplicado) {
                            rutinaOrigen.nombre
                        } else {
                            "Copia de ${rutinaOrigen.nombre}"
                        }

                        if (modoTrainer && clienteId != null) {
                            val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                            app.rutinaRepositoryHibrido.crearRutinaParaCliente(
                                trainerId = trainerId,
                                clienteId = clienteId!!,
                                numeroRutina = numeroRutinaDestino,
                                nombre = nombreDestino,
                                ejercicioIds = rutinaOrigen.ejercicioIds
                            )
                            rutinasCacheModificadas = true
                            // Recargar cache desde Firestore para reflejar los cambios
                            cargarRutinasFirestoreCliente()
                        } else {
                            app.rutinaRepositoryHibrido.actualizarEjerciciosDeRutina(
                                numeroRutina = numeroRutinaDestino,
                                ejercicioIds = rutinaOrigen.ejercicioIds.mapNotNull { it.toIntOrNull() }
                            )
                            // Actualizar nombre en Room
                            rutinaRepository.actualizarNombreRutina(numeroRutinaDestino, nombreDestino)
                            runOnUiThread { actualizarRutinas() }
                        }

                        runOnUiThread {
                            Toast.makeText(
                                this@RutinasActivity,
                                "✅ Rutina cargada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error cargando rutina: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@RutinasActivity,
                                "Error al cargar: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== CAMBIAR NOMBRE ====================

    private fun mostrarDialogoCambiarNombre(button: Button, numeroRutina: Int) {
        val editText = EditText(this).apply {
            hint = getString(R.string.hint_nombre_rutina)
            setText(button.text)
            setSelectAllOnFocus(true)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.titulo_cambiar_nombre_rutina)
            .setView(editText)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val nuevoNombre = editText.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    guardarNombreRutina(button, numeroRutina, nuevoNombre)
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        editText.requestFocus()
    }

    private fun guardarNombreRutina(button: Button, numeroRutina: Int, nuevoNombre: String) {
        lifecycleScope.launch {
            try {
                if (modoTrainer && clienteId != null) {
                    val documentId = RutinaFirestore.generarDocumentId(numeroRutina)
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("clientes")
                        .document(clienteId!!)
                        .collection("rutinas")
                        .document(documentId)
                        .update(
                            "nombre", nuevoNombre,
                            "fechaModificacion", com.google.firebase.Timestamp.now()
                        )
                        .addOnSuccessListener {
                            // Actualizar cache local sin releer Firestore
                            val rutinaExistente = rutinasFirestoreCache[numeroRutina]
                            if (rutinaExistente != null) {
                                rutinasFirestoreCache = rutinasFirestoreCache.toMutableMap().apply {
                                    put(numeroRutina, rutinaExistente.copy(nombre = nuevoNombre))
                                }
                            }
                            button.text = nuevoNombre
                            Toast.makeText(
                                this@RutinasActivity,
                                R.string.nombre_rutina_actualizado,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(
                                this@RutinasActivity,
                                "Error: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    rutinaRepository.actualizarNombreRutina(numeroRutina, nuevoNombre)
                    runOnUiThread {
                        button.text = nuevoNombre
                        Toast.makeText(
                            this@RutinasActivity,
                            R.string.nombre_rutina_actualizado,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@RutinasActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ==================== INCREMENTAR / DECREMENTAR ====================

    private fun incrementarRutinas() {
        if (cantidadRutinas >= maxRutinas) {
            Toast.makeText(this, "Máximo $maxRutinas rutinas", Toast.LENGTH_SHORT).show()
            return
        }

        if (modoTrainer && clienteId != null) {
            cantidadRutinas++
            runOnUiThread {
                actualizarUIRutinas()
                Toast.makeText(this@RutinasActivity, "Rutina $cantidadRutinas disponible", Toast.LENGTH_SHORT).show()
            }
            return
        }

        lifecycleScope.launch {
            val nuevoNumero = rutinaRepository.agregarNuevaRutina(maxRutinas)
            if (nuevoNumero != null) {
                cantidadRutinas++
                runOnUiThread {
                    actualizarUIRutinas()
                    Toast.makeText(this@RutinasActivity, "Rutina $nuevoNumero añadida", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@RutinasActivity, "No se pudo añadir la rutina", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun decrementarRutinas() {
        if (cantidadRutinas <= minRutinas) {
            Toast.makeText(this, "Mínimo $minRutinas rutina", Toast.LENGTH_SHORT).show()
            return
        }

        if (modoTrainer && clienteId != null) {
            AlertDialog.Builder(this)
                .setTitle("Ocultar rutina")
                .setMessage("¿Quieres ocultar la Rutina $cantidadRutinas del cliente?")
                .setPositiveButton("Ocultar") { _, _ ->
                    cantidadRutinas--
                    runOnUiThread {
                        actualizarUIRutinas()
                        Toast.makeText(this@RutinasActivity, "Rutina ocultada", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar rutina")
            .setMessage("¿Estás seguro de que quieres eliminar la última rutina? Se perderán todos sus ejercicios.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    val eliminada = rutinaRepository.eliminarUltimaRutina(minRutinas)
                    if (eliminada) {
                        cantidadRutinas--
                        runOnUiThread {
                            actualizarUIRutinas()
                            Toast.makeText(this@RutinasActivity, "Rutina eliminada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== UI ====================

    private fun actualizarUIRutinas() {
        tvContadorRutinas.text = cantidadRutinas.toString()

        allRutinaButtons.forEach { (button, numero, row) ->
            if (numero <= 10) {
                button.visibility = if (numero <= cantidadRutinas) View.VISIBLE else View.INVISIBLE
                button.isEnabled = numero <= cantidadRutinas
            } else {
                row?.visibility = when {
                    numero % 2 == 1 -> {
                        if (numero <= cantidadRutinas || (numero + 1) <= cantidadRutinas) View.VISIBLE else View.GONE
                    }
                    else -> row.visibility
                }
                button.visibility = if (numero <= cantidadRutinas) View.VISIBLE else View.INVISIBLE
                button.isEnabled = numero <= cantidadRutinas
            }
        }

        actualizarRutinas()
    }

    private fun actualizarRutinas() {
        lifecycleScope.launch {
            if (modoTrainer) {
                actualizarRutinasDesdeFirestore()
            } else {
                actualizarRutinasDesdeRoom()
            }
        }
    }

    /**
     * Actualizar botones desde Room local (modo cliente normal).
     * UNA sola query para todas las rutinas en lugar de una por botón.
     */
    private suspend fun actualizarRutinasDesdeRoom() {
        val todasLasRutinas = rutinaRepository.getAllRutinas().first()
            .associateBy { it.numeroRutina }

        allRutinaButtons.forEach { (button, numero, _) ->
            if (numero <= cantidadRutinas) {
                val rutina = todasLasRutinas[numero]
                val tieneEjercicios = rutina?.ejercicioIds?.isNotEmpty() == true
                val nombreRutina = rutina?.nombre ?: "Rutina $numero"

                runOnUiThread {
                    button.text = nombreRutina
                    val colorRes = if (tieneEjercicios) R.color.rutina_asignada else R.color.rutina_vacia
                    button.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@RutinasActivity, colorRes)
                    )
                }
            }
        }
    }

    /**
     * Actualizar botones desde cache de Firestore (modo trainer).
     * No hace lecturas a Firestore, usa el cache en memoria.
     */
    private fun actualizarRutinasDesdeFirestore() {
        allRutinaButtons.forEach { (button, numero, _) ->
            if (numero <= cantidadRutinas) {
                val rutinaFs = rutinasFirestoreCache[numero]
                val tieneEjercicios = rutinaFs?.ejercicioIds?.isNotEmpty() == true
                val nombreRutina = rutinaFs?.nombre ?: "Rutina $numero"

                runOnUiThread {
                    button.text = nombreRutina
                    val colorRes = if (tieneEjercicios) R.color.rutina_asignada else R.color.rutina_vacia
                    button.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@RutinasActivity, colorRes)
                    )
                }
            }
        }
    }

    /**
     * Cargar rutinas del cliente desde Firestore (modo trainer).
     * Actualiza el cache en memoria.
     */
    private suspend fun cargarRutinasFirestoreCliente() {
        try {
            val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val app = application as GimnasioproApplication

            Log.d(TAG, "🔄 Cargando rutinas del cliente $clienteId (trainer: $trainerId)...")

            val rutinasResult = app.rutinaRepositoryHibrido.getRutinasDeCliente(clienteId!!, trainerId)

            rutinasResult.fold(
                onSuccess = { rutinas ->
                    rutinasFirestoreCache = rutinas.associateBy { it.numeroRutina }
                    cantidadRutinas = if (rutinas.isEmpty()) 10 else maxOf(10, rutinas.maxOf { it.numeroRutina })
                    Log.d(TAG, "✅ Cargadas ${rutinas.size} rutinas del cliente $clienteId")
                    rutinas.forEach { rutina ->
                        Log.d(TAG, "  - Rutina ${rutina.numeroRutina}: ${rutina.nombre} (${rutina.ejercicioIds.size} ejercicios)")
                    }
                    runOnUiThread { actualizarUIRutinas() }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Error cargando rutinas del cliente: ${error.message}", error)
                    cantidadRutinas = 10
                    runOnUiThread {
                        actualizarUIRutinas()
                        Toast.makeText(this@RutinasActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando rutinas Firestore: ${e.message}", e)
            cantidadRutinas = 10
            runOnUiThread { actualizarUIRutinas() }
        }
    }

    // ==================== NAVEGACIÓN ====================

    private fun onRutinaSelected(numeroRutina: Int) {
        val intent = Intent(this, DetalleRutinaActivity::class.java)
        intent.putExtra(DetalleRutinaActivity.EXTRA_NUMERO_RUTINA, numeroRutina)

        if (modoTrainer && clienteId != null) {
            intent.putExtra(EXTRA_MODO_TRAINER, true)
            intent.putExtra(EXTRA_CLIENTE_ID, clienteId)
            intent.putExtra(EXTRA_CLIENTE_NOMBRE, clienteNombre)
            // Marcar que al volver puede haber cambios en las rutinas
            rutinasCacheModificadas = true
        }

        startActivity(intent)
    }
}