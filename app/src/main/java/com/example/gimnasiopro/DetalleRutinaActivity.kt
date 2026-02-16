package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.EjercicioAdapter
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity que muestra el detalle de una rutina con sus ejercicios.
 */
class DetalleRutinaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO_RUTINA = "extra_numero_rutina"
    }

    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido
    private lateinit var rutinaRepository: RutinaRepositoryHibrido
    private lateinit var adapter: EjercicioAdapter

    private lateinit var tvTituloRutina: TextView
    private lateinit var layoutRutinaVacia: LinearLayout
    private lateinit var rvEjerciciosRutina: RecyclerView
    private lateinit var layoutAcciones: LinearLayout
    private lateinit var btnAgregarEjercicios: Button
    private lateinit var btnAnadirEjercicio: Button
    private lateinit var btnBorrarEjercicio: Button
    private lateinit var btnIniciarRutina: Button

    private var numeroRutina: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_rutina)

        // Obtener el número de rutina del intent
        numeroRutina = intent.getIntExtra(EXTRA_NUMERO_RUTINA, 1)

        // Obtener los repositorios desde la Application
        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepositoryHibrido

        setupBackButton()
        setupViews()
        loadRutinaEjercicios()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        tvTituloRutina = findViewById(R.id.tvTituloRutina)
        layoutRutinaVacia = findViewById(R.id.layoutRutinaVacia)
        rvEjerciciosRutina = findViewById(R.id.rvEjerciciosRutina)
        layoutAcciones = findViewById(R.id.layoutAcciones)
        btnAgregarEjercicios = findViewById(R.id.btnAgregarEjercicios)
        btnAnadirEjercicio = findViewById(R.id.btnAnadirEjercicio)
        btnBorrarEjercicio = findViewById(R.id.btnBorrarEjercicio)
        btnIniciarRutina = findViewById(R.id.btnIniciarRutina)

        val btnLimpiarRutina = findViewById<ImageButton>(R.id.btnLimpiarRutina)

        // Configurar título
        tvTituloRutina.text = getString(R.string.rutina_1).replace("1", numeroRutina.toString())

        // Configurar adaptador con selección habilitada
        adapter = EjercicioAdapter(
            onEjercicioClick = { ejercicio ->
                // La selección se maneja en el adapter
            },
            onSelectionChanged = { selectedEjercicios ->
                // Actualizar estado del botón borrar según selección
                actualizarEstadoBotonBorrar(selectedEjercicios.size)
            },
            onEjercicioLongPress = null, // No se usa long press aquí
            maxSeleccion = 10 // Permitir seleccionar hasta 10 ejercicios
        )
        rvEjerciciosRutina.layoutManager = LinearLayoutManager(this)
        rvEjerciciosRutina.adapter = adapter

        // Configurar botones

        btnLimpiarRutina.setOnClickListener {
            confirmarLimpiarRutina()
        }

        btnAgregarEjercicios.setOnClickListener {
            navegarAEjercicios()
        }

        btnAnadirEjercicio.setOnClickListener {
            navegarAEjercicios()
        }

        btnBorrarEjercicio.setOnClickListener {
            borrarEjerciciosSeleccionados()
        }

        btnIniciarRutina.setOnClickListener {
            iniciarEntrenamiento()
        }

        // Inicializar botón borrar como deshabilitado
        actualizarEstadoBotonBorrar(0)
    }

    private fun loadRutinaEjercicios() {
        lifecycleScope.launch {
            // Observar cambios en la rutina
            rutinaRepository.getRutinaByIdFlow(numeroRutina).collectLatest { rutina ->
                val ejercicioIds = rutina?.getEjercicioIdsList() ?: emptyList()

                if (ejercicioIds.isEmpty()) {
                    mostrarRutinaVacia()
                } else {
                    cargarEjercicios(ejercicioIds)
                }
            }
        }
    }

    private fun mostrarRutinaVacia() {
        layoutRutinaVacia.visibility = View.VISIBLE
        rvEjerciciosRutina.visibility = View.GONE
        layoutAcciones.visibility = View.GONE
    }

    private fun mostrarRutinaConEjercicios() {
        layoutRutinaVacia.visibility = View.GONE
        rvEjerciciosRutina.visibility = View.VISIBLE
        layoutAcciones.visibility = View.VISIBLE
    }

    private fun cargarEjercicios(ejercicioIds: List<Long>) {
        lifecycleScope.launch {
            val ejercicios = ejercicioRepository.getEjerciciosByIds(ejercicioIds)

            // Mantener el orden original de los IDs
            val ejerciciosOrdenados = ejercicioIds.mapNotNull { id ->
                ejercicios.find { it.id == id }
            }

            if (ejerciciosOrdenados.isNotEmpty()) {
                adapter.submitList(ejerciciosOrdenados)
                mostrarRutinaConEjercicios()
            } else {
                mostrarRutinaVacia()
            }
        }
    }

    private fun confirmarLimpiarRutina() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_limpiar_rutina))
            .setMessage(getString(R.string.confirmar_limpiar_rutina))
            .setPositiveButton(getString(R.string.btn_limpiar_rutina)) { _, _ ->
                limpiarRutina()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun limpiarRutina() {
        lifecycleScope.launch {
            rutinaRepository.limpiarEjerciciosDeRutina(numeroRutina).fold(
                onSuccess = {
                    Toast.makeText(this@DetalleRutinaActivity, R.string.rutina_limpiada, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(this@DetalleRutinaActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun navegarAEjercicios() {
        val intent = Intent(this, EjerciciosActivity::class.java)
        // Pasar el número de rutina para que al guardar ejercicios se añadan directamente
        intent.putExtra(EjerciciosActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
        startActivity(intent)
    }

    private fun actualizarEstadoBotonBorrar(cantidadSeleccionados: Int) {
        btnBorrarEjercicio.isEnabled = cantidadSeleccionados > 0
        btnBorrarEjercicio.alpha = if (cantidadSeleccionados > 0) 1.0f else 0.5f
    }

    private fun borrarEjerciciosSeleccionados() {
        val ejerciciosSeleccionados = adapter.getSelectedEjercicios()

        if (ejerciciosSeleccionados.isEmpty()) {
            Toast.makeText(this, R.string.selecciona_ejercicios_borrar, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_borrar))
            .setMessage(getString(R.string.confirmar_borrar_ejercicios))
            .setPositiveButton(getString(R.string.btn_borrar)) { _, _ ->
                eliminarEjerciciosSeleccionados(ejerciciosSeleccionados)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun eliminarEjerciciosSeleccionados(ejercicios: Set<Ejercicio>) {
        lifecycleScope.launch {
            val ids = ejercicios.map { it.id }
            val eliminados = rutinaRepository.eliminarEjerciciosDeRutina(numeroRutina, ids)

            adapter.clearSelection()

            Toast.makeText(
                this@DetalleRutinaActivity,
                getString(R.string.ejercicios_eliminados, eliminados),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun iniciarEntrenamiento() {
        val intent = Intent(this, EntrenamientoActivity::class.java)
        intent.putExtra(EntrenamientoActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
        startActivity(intent)
    }
}

