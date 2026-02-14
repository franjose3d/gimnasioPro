package com.example.gimnasiopro

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
import com.example.gimnasiopro.data.EjercicioRepository
import com.example.gimnasiopro.data.RutinaRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity que muestra la lista de ejercicios de un grupo muscular específico.
 * Permite seleccionar hasta 10 ejercicios para guardarlos en una rutina.
 */
class ListaEjerciciosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GRUPO_MUSCULAR = "extra_grupo_muscular"
        private const val MAX_EJERCICIOS = 10
    }

    private lateinit var ejercicioRepository: EjercicioRepository
    private lateinit var rutinaRepository: RutinaRepository
    private lateinit var adapter: EjercicioAdapter
    private lateinit var tvGrupoMuscular: TextView
    private lateinit var rvEjercicios: RecyclerView
    private lateinit var layoutSeleccion: LinearLayout
    private lateinit var tvContadorSeleccion: TextView
    private lateinit var btnCancelarSeleccion: Button
    private lateinit var btnGuardarSeleccion: Button
    private lateinit var tvNoEjercicios: TextView
    private lateinit var btnAgregarEjercicio: Button
    private lateinit var btnEliminarEjercicio: Button
    private lateinit var grupoMuscularActual: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_ejercicios)

        // Obtener los repositorios desde la Application
        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepository

        // Obtener el grupo muscular del intent
        grupoMuscularActual = intent.getStringExtra(EXTRA_GRUPO_MUSCULAR) ?: run {
            finish()
            return
        }

        setupViews(grupoMuscularActual)
        setupBackPressedCallback()
        loadEjercicios(grupoMuscularActual)
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
        btnEliminarEjercicio = findViewById(R.id.btnEliminarEjercicio)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

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

        // NUEVO: Configurar botones + y -
        btnAgregarEjercicio.setOnClickListener {
            mostrarDialogoAgregarEjercicio()
        }

        btnEliminarEjercicio.setOnClickListener {
            mostrarDialogoEliminarEjercicio()
        }
    }

    /**
     * Muestra diálogo para agregar un ejercicio personalizado.
     */
    private fun mostrarDialogoAgregarEjercicio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_ejercicio, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreEjercicio)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcionEjercicio)

        // Limitar descripción a 200 caracteres
        etDescripcion.filters = arrayOf(InputFilter.LengthFilter(200))

        AlertDialog.Builder(this)
            .setTitle("Agregar Ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
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

                // Crear y guardar el ejercicio
                val nuevoEjercicio = Ejercicio(
                    grupoMuscular = grupoMuscularActual,
                    nombre = nombre,
                    descripcion = descripcion,
                    imagenUrl = null
                )

                lifecycleScope.launch {
                    try {
                        ejercicioRepository.insertEjercicio(nuevoEjercicio)
                        Toast.makeText(
                            this@ListaEjerciciosActivity,
                            "Ejercicio agregado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        // La lista se actualizará automáticamente gracias al Flow
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ListaEjerciciosActivity,
                            "Error al guardar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra diálogo para seleccionar y eliminar un ejercicio.
     */
    private fun mostrarDialogoEliminarEjercicio() {
        lifecycleScope.launch {
            try {
                // Obtener todos los ejercicios del grupo muscular actual
                ejercicioRepository.getEjerciciosByGrupoMuscular(grupoMuscularActual)
                    .collectLatest { ejercicios ->
                        if (ejercicios.isEmpty()) {
                            Toast.makeText(
                                this@ListaEjerciciosActivity,
                                "No hay ejercicios para eliminar",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@collectLatest
                        }

                        // Crear array de nombres para el diálogo
                        val nombresEjercicios = ejercicios.map { it.nombre }.toTypedArray()

                        AlertDialog.Builder(this@ListaEjerciciosActivity)
                            .setTitle("¿Qué ejercicio deseas eliminar?")
                            .setItems(nombresEjercicios) { _, which ->
                                val ejercicioSeleccionado = ejercicios[which]
                                confirmarEliminacion(ejercicioSeleccionado)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()

                        // Importante: romper el Flow después de mostrar el diálogo
                        return@collectLatest
                    }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ListaEjerciciosActivity,
                    "Error al cargar ejercicios: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Muestra diálogo de confirmación antes de eliminar un ejercicio.
     */
    private fun confirmarEliminacion(ejercicio: Ejercicio) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar '${ejercicio.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarEjercicio(ejercicio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Elimina un ejercicio de la base de datos.
     */
    private fun eliminarEjercicio(ejercicio: Ejercicio) {
        lifecycleScope.launch {
            try {
                ejercicioRepository.deleteEjercicio(ejercicio)
                Toast.makeText(
                    this@ListaEjerciciosActivity,
                    "Ejercicio eliminado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                // La lista se actualizará automáticamente gracias al Flow
            } catch (e: Exception) {
                Toast.makeText(
                    this@ListaEjerciciosActivity,
                    "Error al eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
                    DatabaseInitializer.initializeIfNeeded(this@ListaEjerciciosActivity, ejercicioRepository, app.rutinaRepository)
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

        // Mostrar diálogo para seleccionar rutina
        mostrarDialogoSeleccionRutina(selectedEjercicios.toList())
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