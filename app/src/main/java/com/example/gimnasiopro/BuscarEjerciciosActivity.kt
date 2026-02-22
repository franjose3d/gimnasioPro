package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.EjercicioAdapter
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Activity para buscar ejercicios en toda la librería.
 * Implementa búsqueda por similitud (aproximada) que encuentra ejercicios
 * cuyos nombres contengan la palabra buscada.
 */
class BuscarEjerciciosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO_RUTINA = "extra_numero_rutina"
        private const val MAX_EJERCICIOS = 10
        private const val SEARCH_DELAY_MS = 300L // Delay para evitar búsquedas excesivas
    }

    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido
    private lateinit var rutinaRepository: com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido
    private lateinit var adapter: EjercicioAdapter

    private lateinit var etBuscar: EditText
    private lateinit var rvResultados: RecyclerView
    private lateinit var tvNoResultados: TextView
    private lateinit var tvEscribeParaBuscar: TextView
    private lateinit var layoutSeleccion: LinearLayout
    private lateinit var tvContadorSeleccion: TextView
    private lateinit var btnCancelarSeleccion: Button
    private lateinit var btnGuardarSeleccion: Button

    // Lista completa de ejercicios para búsqueda local
    private var todosLosEjercicios: List<Ejercicio> = emptyList()

    // Job para debounce de búsqueda
    private var searchJob: Job? = null

    // Número de rutina si viene de DetalleRutinaActivity (-1 si no)
    private var numeroRutinaDestino: Int = -1

    // Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_ejercicios)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Obtener los repositorios desde la Application
        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepositoryHibrido

        // Obtener el número de rutina si viene de una rutina específica
        numeroRutinaDestino = intent.getIntExtra(EXTRA_NUMERO_RUTINA, -1)

        setupViews()
        setupSearchListener()
        cargarTodosLosEjercicios()
    }

    private fun setupViews() {
        etBuscar = findViewById(R.id.etBuscar)
        rvResultados = findViewById(R.id.rvResultados)
        tvNoResultados = findViewById(R.id.tvNoResultados)
        tvEscribeParaBuscar = findViewById(R.id.tvEscribeParaBuscar)
        layoutSeleccion = findViewById(R.id.layoutSeleccion)
        tvContadorSeleccion = findViewById(R.id.tvContadorSeleccion)
        btnCancelarSeleccion = findViewById(R.id.btnCancelarSeleccion)
        btnGuardarSeleccion = findViewById(R.id.btnGuardarSeleccion)

        // Botón volver
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Configurar RecyclerView
        rvResultados.layoutManager = LinearLayoutManager(this)

        // Modo de selección si viene de una rutina
        val modoSeleccion = numeroRutinaDestino > 0

        adapter = EjercicioAdapter(
            onEjercicioClick = { ejercicio ->
                if (modoSeleccion) {
                    // Seleccionar/deseleccionar - se maneja internamente en el adapter
                    actualizarUISeleccion()
                } else {
                    // TODO: Mostrar detalle del ejercicio
                }
            },
            onSelectionChanged = { selectedEjercicios ->
                actualizarUISeleccion()
            },
            onEjercicioLongPress = null,
            maxSeleccion = MAX_EJERCICIOS
        )
        rvResultados.adapter = adapter

        // Mostrar panel de selección si viene de rutina
        if (modoSeleccion) {
            layoutSeleccion.visibility = View.VISIBLE
            actualizarUISeleccion()

            btnCancelarSeleccion.setOnClickListener {
                adapter.clearSelections()
                actualizarUISeleccion()
            }

            btnGuardarSeleccion.setOnClickListener {
                guardarEjerciciosSeleccionados()
            }
        }

        // Focus automático en el campo de búsqueda
        etBuscar.requestFocus()
    }

    private fun setupSearchListener() {
        // Búsqueda mientras escribe (con debounce)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                // Cancelar búsqueda anterior
                searchJob?.cancel()

                if (query.isEmpty()) {
                    mostrarMensajeInicial()
                    return
                }

                // Debounce: esperar antes de buscar
                searchJob = lifecycleScope.launch {
                    delay(SEARCH_DELAY_MS)
                    buscarEjercicios(query)
                }
            }
        })

        // Búsqueda al pulsar Enter/buscar en teclado
        etBuscar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etBuscar.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchJob?.cancel()
                    buscarEjercicios(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun cargarTodosLosEjercicios() {
        lifecycleScope.launch {
            try {
                todosLosEjercicios = ejercicioRepository.allEjercicios.first()
            } catch (e: Exception) {
                Toast.makeText(
                    this@BuscarEjerciciosActivity,
                    "Error al cargar ejercicios",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Busca ejercicios por similitud (aproximada).
     * Un ejercicio coincide si su nombre contiene la palabra buscada,
     * ignorando mayúsculas/minúsculas y acentos.
     */
    private fun buscarEjercicios(query: String) {
        if (todosLosEjercicios.isEmpty()) {
            mostrarNoResultados()
            return
        }

        val queryNormalizado = normalizarTexto(query)

        // Búsqueda por similitud: contiene la palabra buscada
        val resultados = todosLosEjercicios.filter { ejercicio ->
            val nombreNormalizado = normalizarTexto(ejercicio.nombre)
            val descripcionNormalizada = normalizarTexto(ejercicio.descripcion)
            val grupoNormalizado = normalizarTexto(ejercicio.grupoMuscular)

            // Buscar en nombre, descripción o grupo muscular
            nombreNormalizado.contains(queryNormalizado) ||
                descripcionNormalizada.contains(queryNormalizado) ||
                grupoNormalizado.contains(queryNormalizado)
        }.sortedWith(compareBy(
            // Priorizar coincidencias en nombre
            { !normalizarTexto(it.nombre).contains(queryNormalizado) },
            // Luego alfabéticamente
            { it.nombre }
        ))

        if (resultados.isEmpty()) {
            mostrarNoResultados()
        } else {
            mostrarResultados(resultados)
        }
    }

    /**
     * Normaliza texto para comparación:
     * - Convierte a minúsculas
     * - Elimina acentos y caracteres especiales
     */
    private fun normalizarTexto(texto: String): String {
        return texto
            .lowercase(Locale.getDefault())
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ñ", "n")
    }

    private fun mostrarMensajeInicial() {
        tvEscribeParaBuscar.visibility = View.VISIBLE
        tvNoResultados.visibility = View.GONE
        rvResultados.visibility = View.GONE
    }

    private fun mostrarNoResultados() {
        tvEscribeParaBuscar.visibility = View.GONE
        tvNoResultados.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE
    }

    private fun mostrarResultados(resultados: List<Ejercicio>) {
        tvEscribeParaBuscar.visibility = View.GONE
        tvNoResultados.visibility = View.GONE
        rvResultados.visibility = View.VISIBLE
        adapter.submitList(resultados)
    }

    private fun actualizarUISeleccion() {
        val count = adapter.getSelectedCount()
        tvContadorSeleccion.text = "$count / $MAX_EJERCICIOS seleccionados"
        btnGuardarSeleccion.isEnabled = count > 0
    }

    private fun guardarEjerciciosSeleccionados() {
        val seleccionados = adapter.getSelectedEjercicios().map { it.id.toInt() }
        if (seleccionados.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un ejercicio", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Debes iniciar sesión para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Usar agregarEjerciciosARutina del repositorio
                val resultado = rutinaRepository.agregarEjerciciosARutina(
                    numeroRutinaDestino,
                    seleccionados
                )

                if (resultado.exito) {
                    Toast.makeText(
                        this@BuscarEjerciciosActivity,
                        getString(R.string.ejercicios_guardados),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Volver a DetalleRutinaActivity
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@BuscarEjerciciosActivity,
                        resultado.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BuscarEjerciciosActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

