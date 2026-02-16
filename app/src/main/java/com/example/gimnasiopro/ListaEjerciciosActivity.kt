package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.EjercicioAdapter
import com.example.gimnasiopro.data.DatabaseInitializer
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.toRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity que muestra la lista de ejercicios de un grupo muscular específico.
 * Permite seleccionar hasta 10 ejercicios para guardarlos en una rutina.
 */
class ListaEjerciciosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GRUPO_MUSCULAR = "extra_grupo_muscular"
        const val EXTRA_NUMERO_RUTINA = "extra_numero_rutina"
        private const val MAX_EJERCICIOS = 10
    }

    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido
    private lateinit var rutinaRepository: com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido
    private lateinit var adapter: EjercicioAdapter
    private lateinit var tvGrupoMuscular: TextView
    private lateinit var rvEjercicios: RecyclerView
    private lateinit var layoutSeleccion: LinearLayout
    private lateinit var tvContadorSeleccion: TextView
    private lateinit var btnCancelarSeleccion: Button
    private lateinit var btnGuardarSeleccion: Button
    private lateinit var tvNoEjercicios: TextView
    private lateinit var btnAgregarEjercicio: Button
    private lateinit var layoutTrainerControls: LinearLayout
    private lateinit var btnCargarEjercicios: Button
    private lateinit var grupoMuscularActual: String

    // Número de rutina si viene de DetalleRutinaActivity (-1 si no viene de una rutina específica)
    private var numeroRutinaDestino: Int = -1

    // Panel colapsable
    private lateinit var btnExpandirOpciones: LinearLayout
    private lateinit var layoutContenidoColapsable: LinearLayout
    private lateinit var ivExpandirFlecha: android.widget.ImageView
    private var panelExpandido = false

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var tipoUsuario: String = "" // "trainer" o "cliente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_ejercicios)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Obtener los repositorios desde la Application
        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepositoryHibrido

        // Obtener el grupo muscular del intent
        grupoMuscularActual = intent.getStringExtra(EXTRA_GRUPO_MUSCULAR) ?: run {
            finish()
            return
        }

        // Obtener el número de rutina si viene de DetalleRutinaActivity
        numeroRutinaDestino = intent.getIntExtra(EXTRA_NUMERO_RUTINA, -1)

        setupViews(grupoMuscularActual)
        setupBackPressedCallback()
        loadEjercicios(grupoMuscularActual)

        // Detectar tipo de usuario y mostrar/ocultar controles de trainer
        detectarTipoUsuario()
    }

    /**
     * Detecta si el usuario actual es trainer o cliente
     * y muestra/oculta los controles correspondientes:
     * - Trainer: ve botón "Cargar ejercicios" + botón "+ Nuevo ejercicio"
     * - Cliente: ve solo botón "Cargar ejercicios"
     * - No logueado: ve botón "Cargar ejercicios" (al pulsarlo le pide registrarse)
     */
    private fun detectarTipoUsuario() {
        val userId = auth.currentUser?.uid

        // SIEMPRE mostrar el botón de cargar ejercicios (incentiva el registro)
        btnCargarEjercicios.visibility = View.VISIBLE

        if (userId == null) {
            // Usuario no logueado - ocultar controles de trainer
            layoutTrainerControls.visibility = View.GONE
            return
        }

        // Buscar primero en trainers
        firestore.collection("trainers").document(userId).get()
            .addOnSuccessListener { trainerDoc ->
                if (trainerDoc.exists()) {
                    tipoUsuario = "trainer"
                    layoutTrainerControls.visibility = View.VISIBLE
                } else {
                    // Si no es trainer, buscar en clientes
                    firestore.collection("clientes").document(userId).get()
                        .addOnSuccessListener { clienteDoc ->
                            if (clienteDoc.exists()) {
                                tipoUsuario = "cliente"
                            }
                            layoutTrainerControls.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            layoutTrainerControls.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener {
                // Error al consultar - ocultar controles de trainer por seguridad
                layoutTrainerControls.visibility = View.GONE
            }
    }

    /**
     * Expande o contrae el panel de opciones (cargar ejercicios, nuevo ejercicio)
     */
    private fun togglePanelOpciones() {
        panelExpandido = !panelExpandido

        if (panelExpandido) {
            // Expandir
            layoutContenidoColapsable.visibility = View.VISIBLE
            ivExpandirFlecha.animate().rotation(180f).setDuration(200).start()
        } else {
            // Contraer
            layoutContenidoColapsable.visibility = View.GONE
            ivExpandirFlecha.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun setupViews(grupoMuscular: String) {
        tvGrupoMuscular = findViewById(R.id.tvGrupoMuscular)
        rvEjercicios = findViewById(R.id.rvEjercicios)
        layoutSeleccion = findViewById(R.id.layoutSeleccion)
        tvContadorSeleccion = findViewById(R.id.tvContadorSeleccion)
        btnCancelarSeleccion = findViewById(R.id.btnCancelarSeleccion)
        btnGuardarSeleccion = findViewById(R.id.btnGuardarSeleccion)
        tvNoEjercicios = findViewById(R.id.tvNoEjercicios)
        btnAgregarEjercicio = findViewById(R.id.btnAgregarEjercicio)
        layoutTrainerControls = findViewById(R.id.layoutTrainerControls)
        btnCargarEjercicios = findViewById(R.id.btnCargarEjercicios)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Panel colapsable
        btnExpandirOpciones = findViewById(R.id.btnExpandirOpciones)
        layoutContenidoColapsable = findViewById(R.id.layoutContenidoColapsable)
        ivExpandirFlecha = findViewById(R.id.ivExpandirFlecha)

        // Ocultar controles de trainer por defecto (se mostrarán si el usuario es trainer)
        layoutTrainerControls.visibility = View.GONE
        // Ocultar botón cargar ejercicios por defecto (se mostrará según estado de login)
        btnCargarEjercicios.visibility = View.GONE

        // Configurar botón expandir/contraer panel
        btnExpandirOpciones.setOnClickListener {
            togglePanelOpciones()
        }

        // Configurar título
        tvGrupoMuscular.text = grupoMuscular

        // Configurar botón de retroceso
        btnBack.setOnClickListener {
            if (adapter.getSelectedCount() > 0) {
                // Si hay selecciones, preguntar si quiere cancelar
                AlertDialog.Builder(this)
                    .setTitle("Cancelar selección")
                    .setMessage("¿Deseas cancelar la selección actual?")
                    .setPositiveButton("Sí") { _, _ ->
                        cancelarSeleccion()
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                finish()
            }
        }

        // Configurar adaptador con callbacks de selección
        adapter = EjercicioAdapter(
            onEjercicioClick = { ejercicio ->
                // TODO: Navegar al detalle del ejercicio si se desea
            },
            onSelectionChanged = { selectedEjercicios ->
                onSelectionChanged(selectedEjercicios)
            },
            onEjercicioLongPress = { ejercicio ->
                // Mostrar diálogo para eliminar ejercicio del dispositivo local
                mostrarDialogoEliminarEjercicioLocal(ejercicio)
            },
            maxSeleccion = MAX_EJERCICIOS
        )
        rvEjercicios.layoutManager = LinearLayoutManager(this)
        rvEjercicios.adapter = adapter

        // Configurar botones de selección
        btnCancelarSeleccion.setOnClickListener {
            cancelarSeleccion()
        }

        btnGuardarSeleccion.setOnClickListener {
            guardarSeleccion()
        }

        // Configurar botón para agregar ejercicio (solo trainers)
        btnAgregarEjercicio.setOnClickListener {
            mostrarDialogoAgregarEjercicio()
        }

        // Configurar botón para cargar ejercicios desde Firebase
        btnCargarEjercicios.setOnClickListener {
            cargarEjerciciosDesdeFirebase()
        }
    }

    /**
     * Muestra diálogo para agregar un ejercicio personalizado.
     */
    private fun mostrarDialogoAgregarEjercicio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_ejercicio, null)
        val spinnerGrupo = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerGrupoMuscular)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreEjercicio)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcionEjercicio)

        // Lista de grupos musculares
        val gruposMusculares = listOf(
            "Pectorales",
            "Espalda",
            "Hombros",
            "Bíceps y Antebrazo",
            "Tríceps",
            "Abdominales",
            "Piernas",
            "Glúteos",
            "Gemelos"
        )

        // Configurar Spinner
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            gruposMusculares
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGrupo.adapter = adapter

        // Seleccionar el grupo muscular actual por defecto
        val indexActual = gruposMusculares.indexOf(grupoMuscularActual)
        if (indexActual >= 0) {
            spinnerGrupo.setSelection(indexActual)
        }

        // Limitar descripción a 200 caracteres
        etDescripcion.filters = arrayOf(InputFilter.LengthFilter(200))

        AlertDialog.Builder(this)
            .setTitle("Agregar Ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val grupoSeleccionado = spinnerGrupo.selectedItem.toString()
                val nombre = etNombre.text.toString().trim()
                val descripcion = etDescripcion.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (descripcion.isEmpty()) {
                    Toast.makeText(this, "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Verificar si el ejercicio ya existe y guardarlo
                guardarEjercicioSiNoExiste(grupoSeleccionado, nombre, descripcion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Verifica si un ejercicio ya existe en Firebase y lo guarda si no existe.
     * Muestra mensaje de error si ya existe.
     */
    private fun guardarEjercicioSiNoExiste(grupoMuscular: String, nombre: String, descripcion: String) {
        lifecycleScope.launch {
            try {
                // Obtener la app para acceder al repositorio remoto
                val app = application as GimnasioproApplication
                val remoteRepository = app.ejercicioFirestoreRepository

                // Verificar si ya existe un ejercicio con ese nombre en el grupo
                val ejerciciosExistentes = remoteRepository.getEjerciciosByGrupoMuscular(grupoMuscular).first()
                val yaExiste = ejerciciosExistentes.any {
                    it.nombre.equals(nombre, ignoreCase = true)
                }

                if (yaExiste) {
                    // El ejercicio ya existe - mostrar aviso
                    runOnUiThread {
                        AlertDialog.Builder(this@ListaEjerciciosActivity)
                            .setTitle("⚠️ Ejercicio duplicado")
                            .setMessage("Ya existe un ejercicio llamado \"$nombre\" en el grupo $grupoMuscular.\n\nPor favor, elige otro nombre o verifica si el ejercicio ya está disponible.")
                            .setPositiveButton("Entendido", null)
                            .show()
                    }
                    return@launch
                }

                // El ejercicio no existe - guardarlo
                val nuevoEjercicio = Ejercicio(
                    grupoMuscular = grupoMuscular,
                    nombre = nombre,
                    descripcion = descripcion,
                    imagenUrl = null
                )

                val trainerId = auth.currentUser?.uid
                ejercicioRepository.insertEjercicio(nuevoEjercicio, trainerId)

                runOnUiThread {
                    Toast.makeText(
                        this@ListaEjerciciosActivity,
                        "✅ Ejercicio agregado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Recargar lista si el ejercicio es del grupo actual
                    if (grupoMuscular == grupoMuscularActual) {
                        loadEjercicios(grupoMuscularActual)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ListaEjerciciosActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Muestra diálogo para eliminar un ejercicio SOLO del dispositivo local.
     * El ejercicio seguirá existiendo en Firebase y se puede volver a cargar.
     */
    private fun mostrarDialogoEliminarEjercicioLocal(ejercicio: Ejercicio) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar ejercicio")
            .setMessage("¿Quieres eliminar \"${ejercicio.nombre}\" de tu dispositivo?\n\nEste ejercicio solo se eliminará de tu teléfono. Puedes volver a cargarlo desde la nube en cualquier momento.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarEjercicioLocal(ejercicio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Elimina un ejercicio de la base de datos local (Room).
     * NO lo elimina de Firebase.
     */
    private fun eliminarEjercicioLocal(ejercicio: Ejercicio) {
        lifecycleScope.launch {
            try {
                // Obtener repositorio local
                val app = application as GimnasioproApplication
                app.localEjercicioRepository.deleteEjercicio(ejercicio)

                runOnUiThread {
                    Toast.makeText(
                        this@ListaEjerciciosActivity,
                        "✅ Ejercicio eliminado de tu dispositivo",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Recargar la lista
                    loadEjercicios(grupoMuscularActual)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ListaEjerciciosActivity,
                        "Error al eliminar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Muestra diálogo invitando al usuario a registrarse para acceder a más ejercicios.
     */
    private fun mostrarDialogoRegistro() {
        AlertDialog.Builder(this)
            .setTitle("🔐 Registro requerido")
            .setMessage("Para acceder a todos los ejercicios disponibles, necesitas crear una cuenta.\n\n¿Quieres registrarte ahora?")
            .setPositiveButton("Registrarme") { _, _ ->
                // Ir a PersonalTrainerActivity (tiene opciones de registro)
                val intent = Intent(this, PersonalTrainerActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    /**
     * Carga ejercicios del grupo muscular actual desde Firebase.
     * Si no está logueado, muestra diálogo para registrarse.
     */
    private fun cargarEjerciciosDesdeFirebase() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Usuario no logueado - mostrar diálogo para registrarse
            mostrarDialogoRegistro()
            return
        }

        // Mostrar indicador de carga
        btnCargarEjercicios.isEnabled = false
        btnCargarEjercicios.text = getString(R.string.cargando_ejercicios_firebase)

        lifecycleScope.launch {
            try {
                // Obtener la app para acceder al repositorio remoto
                val app = application as GimnasioproApplication
                val remoteRepository = app.ejercicioFirestoreRepository

                // Obtener ejercicios de Firebase para este grupo muscular
                remoteRepository.getEjerciciosByGrupoMuscular(grupoMuscularActual)
                    .collect { ejerciciosFirestore ->
                        var ejerciciosCargados = 0

                        for (ejercicioFirestore in ejerciciosFirestore) {
                            // Convertir a modelo Room
                            val ejercicioRoom = ejercicioFirestore.toRoom()

                            // Verificar si ya existe en local (por nombre y grupo)
                            val existente = ejercicioRepository.getEjerciciosByGrupoMuscular(grupoMuscularActual)
                                .first()
                                .find { it.nombre.equals(ejercicioRoom.nombre, ignoreCase = true) }

                            if (existente == null) {
                                // No existe, insertarlo
                                ejercicioRepository.insertEjercicio(ejercicioRoom)
                                ejerciciosCargados++
                            }
                        }

                        // Mostrar resultado
                        runOnUiThread {
                            btnCargarEjercicios.isEnabled = true
                            btnCargarEjercicios.text = getString(R.string.btn_cargar_ejercicios)

                            if (ejerciciosCargados > 0) {
                                Toast.makeText(
                                    this@ListaEjerciciosActivity,
                                    getString(R.string.ejercicios_cargados, ejerciciosCargados),
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Recargar la lista
                                loadEjercicios(grupoMuscularActual)
                            } else {
                                Toast.makeText(
                                    this@ListaEjerciciosActivity,
                                    getString(R.string.no_hay_mas_ejercicios),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        // Solo procesar la primera emisión
                        return@collect
                    }
            } catch (e: Exception) {
                runOnUiThread {
                    btnCargarEjercicios.isEnabled = true
                    btnCargarEjercicios.text = getString(R.string.btn_cargar_ejercicios)
                    Toast.makeText(
                        this@ListaEjerciciosActivity,
                        "${getString(R.string.error_cargar_ejercicios_firebase)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun loadEjercicios(grupoMuscular: String) {
        android.util.Log.d("ListaEjercicios", "=== Iniciando carga de ejercicios ===")
        android.util.Log.d("ListaEjercicios", "Grupo muscular solicitado: '$grupoMuscular'")

        // Mostrar indicador de carga
        tvNoEjercicios.text = getString(R.string.cargando_ejercicios)
        tvNoEjercicios.visibility = View.VISIBLE
        rvEjercicios.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Primero verificar si hay ejercicios en la BD
                val totalCount = ejercicioRepository.getEjerciciosCount()
                android.util.Log.d("ListaEjercicios", "Total ejercicios en BD: $totalCount")

                if (totalCount == 0) {
                    android.util.Log.w("ListaEjercicios", "¡BD vacía! Forzando reinicialización...")
                    // Intentar reinicializar
                    val app = application as GimnasioproApplication
                    DatabaseInitializer.initializeIfNeeded(this@ListaEjerciciosActivity, app.localEjercicioRepository, app.rutinaRepository)
                }

                ejercicioRepository.getEjerciciosByGrupoMuscular(grupoMuscular).collectLatest { ejercicios ->
                    android.util.Log.d("ListaEjercicios", "Ejercicios recibidos del repositorio: ${ejercicios.size}")

                    if (ejercicios.isEmpty()) {
                        android.util.Log.w("ListaEjercicios", "¡ADVERTENCIA! Lista vacía recibida para '$grupoMuscular'")
                        // Verificar el conteo total de ejercicios
                        val newCount = ejercicioRepository.getEjerciciosCount()
                        android.util.Log.d("ListaEjercicios", "Conteo actual de ejercicios en BD: $newCount")

                        // Mostrar mensaje de no hay ejercicios
                        tvNoEjercicios.text = getString(R.string.no_ejercicios)
                        tvNoEjercicios.visibility = View.VISIBLE
                        rvEjercicios.visibility = View.GONE
                    } else {
                        ejercicios.take(3).forEachIndexed { index, ej ->
                            android.util.Log.d("ListaEjercicios", "  [$index] ${ej.nombre} - Grupo: ${ej.grupoMuscular}")
                        }
                        if (ejercicios.size > 3) {
                            android.util.Log.d("ListaEjercicios", "  ... y ${ejercicios.size - 3} más")
                        }

                        // Ocultar mensaje y mostrar lista
                        tvNoEjercicios.visibility = View.GONE
                        rvEjercicios.visibility = View.VISIBLE
                    }

                    adapter.submitList(ejercicios)
                    android.util.Log.d("ListaEjercicios", "Lista enviada al adapter")
                }
            } catch (e: Exception) {
                android.util.Log.e("ListaEjercicios", "Error cargando ejercicios", e)
                tvNoEjercicios.text = getString(R.string.error_cargar_ejercicios)
                tvNoEjercicios.visibility = View.VISIBLE
                rvEjercicios.visibility = View.GONE
            }
        }
    }

    private fun onSelectionChanged(selectedEjercicios: Set<Ejercicio>) {
        val count = selectedEjercicios.size

        // Mostrar/ocultar barra de selección basándose en si hay ejercicios seleccionados
        layoutSeleccion.visibility = if (count > 0) View.VISIBLE else View.GONE

        // Actualizar contador
        tvContadorSeleccion.text = getString(R.string.seleccion_contador, count)

        // Mostrar mensaje si se alcanza el máximo
        if (count >= MAX_EJERCICIOS && adapter.isMaxReached()) {
            Toast.makeText(this, R.string.max_ejercicios_alcanzado, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelarSeleccion() {
        adapter.clearSelection()
        layoutSeleccion.visibility = View.GONE
    }

    private fun guardarSeleccion() {
        val selectedEjercicios = adapter.getSelectedEjercicios()

        if (selectedEjercicios.isEmpty()) {
            Toast.makeText(this, R.string.selecciona_ejercicios, Toast.LENGTH_SHORT).show()
            return
        }

        // Si viene de DetalleRutinaActivity, guardar directamente en esa rutina
        if (numeroRutinaDestino > 0) {
            guardarEjerciciosEnRutina(numeroRutinaDestino, selectedEjercicios.toList())
        } else {
            // Si no viene de una rutina específica, mostrar diálogo para seleccionar
            mostrarDialogoSeleccionRutina(selectedEjercicios.toList())
        }
    }

    private fun mostrarDialogoSeleccionRutina(ejercicios: List<Ejercicio>) {
        lifecycleScope.launch {
            // Construir la lista de rutinas con su conteo actual de ejercicios
            val rutinasConConteo = mutableListOf<String>()
            for (i in 1..10) {
                val numEjercicios = rutinaRepository.getNumeroEjerciciosEnRutina(i)
                val espaciosDisponibles = RutinaRepository.MAX_EJERCICIOS_POR_RUTINA - numEjercicios
                val estado = when {
                    numEjercicios >= RutinaRepository.MAX_EJERCICIOS_POR_RUTINA -> " (LLENA)"
                    numEjercicios > 0 -> " ($numEjercicios/10 - $espaciosDisponibles disponibles)"
                    else -> " (vacía)"
                }
                rutinasConConteo.add(getString(R.string.rutina_1).replace("1", i.toString()) + estado)
            }

            AlertDialog.Builder(this@ListaEjerciciosActivity)
                .setTitle(R.string.selecciona_rutina)
                .setItems(rutinasConConteo.toTypedArray()) { _, which ->
                    val numeroRutina = which + 1
                    guardarEjerciciosEnRutina(numeroRutina, ejercicios)
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }
    }

    private fun guardarEjerciciosEnRutina(numeroRutina: Int, ejercicios: List<Ejercicio>) {
        lifecycleScope.launch {
            // Obtener los IDs de los ejercicios (convertir Long a Int)
            val ejercicioIds = ejercicios.map { it.id.toInt() }

            // Añadir ejercicios a la rutina (sin borrar los existentes)
            val resultado = rutinaRepository.agregarEjerciciosARutina(numeroRutina, ejercicioIds)

            // Mostrar mensaje según el resultado
            val toastDuration = if (resultado.ejerciciosNoAgregados > 0) {
                Toast.LENGTH_LONG
            } else {
                Toast.LENGTH_SHORT
            }

            Toast.makeText(
                this@ListaEjerciciosActivity,
                resultado.mensaje,
                toastDuration
            ).show()

            // Limpiar selección
            cancelarSeleccion()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this) {
            if (adapter.getSelectedCount() > 0) {
                // Si hay selecciones, preguntar si quiere cancelar
                AlertDialog.Builder(this@ListaEjerciciosActivity)
                    .setTitle("Cancelar selección")
                    .setMessage("¿Deseas cancelar la selección actual?")
                    .setPositiveButton("Sí") { _, _ ->
                        cancelarSeleccion()
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                finish()
            }
        }
    }
}