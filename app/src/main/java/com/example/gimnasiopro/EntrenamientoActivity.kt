package com.example.gimnasiopro

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.EntrenamientoAdapter
import com.example.gimnasiopro.data.EjercicioEntrenamiento
import com.example.gimnasiopro.data.RegistroEntrenamiento
import com.example.gimnasiopro.data.RegistroEntrenamientoRepository
import com.example.gimnasiopro.data.RutinaRepository
import com.example.gimnasiopro.data.SerieEntrenamiento
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.EstadisticaRepositoryHibrido
import com.example.gimnasiopro.data.sync.SyncManager
import kotlinx.coroutines.launch

/**
 * Activity para realizar un entrenamiento con los ejercicios de una rutina.
 * Permite registrar repeticiones y peso para cada ejercicio.
 */
class EntrenamientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO_RUTINA = "extra_numero_rutina"
    }

    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido
    private lateinit var rutinaRepository: RutinaRepository
    private lateinit var registroRepository: RegistroEntrenamientoRepository
    private lateinit var estadisticaRepository: EstadisticaRepositoryHibrido
    private lateinit var adapter: EntrenamientoAdapter

    private lateinit var tvTituloEntrenamiento: TextView
    private lateinit var rvEjerciciosEntrenamiento: RecyclerView
    private lateinit var btnFinalizarEntrenamiento: Button

    private var numeroRutina: Int = 1
    private val ejerciciosEntrenamiento = mutableListOf<EjercicioEntrenamiento>()

    // Tiempo de inicio del entrenamiento
    private var tiempoInicio: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrenamiento)

        numeroRutina = intent.getIntExtra(EXTRA_NUMERO_RUTINA, 1)

        // Registrar tiempo de inicio
        tiempoInicio = System.currentTimeMillis()

        // IMPORTANTE: Activar modo entrenamiento para trabajar solo con Room
        // Durante el entrenamiento NO se sincroniza con Firebase
        // Solo se sincronizará al pulsar "Finalizar entrenamiento"
        SyncManager.startTrainingMode()

        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepository
        registroRepository = app.registroEntrenamientoRepository
        estadisticaRepository = app.estadisticaRepositoryHibrido

        setupViews()
        setupBackPressedCallback()
        cargarEjerciciosDeRutina()
    }

    private fun setupViews() {
        tvTituloEntrenamiento = findViewById(R.id.tvTituloEntrenamiento)
        rvEjerciciosEntrenamiento = findViewById(R.id.rvEjerciciosEntrenamiento)
        btnFinalizarEntrenamiento = findViewById(R.id.btnFinalizarEntrenamiento)

        // Configurar título
        tvTituloEntrenamiento.text = getString(R.string.titulo_entrenamiento_con_numero, numeroRutina)

        // Configurar botón volver
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            confirmarSalir()
        }

        // Configurar RecyclerView
        adapter = EntrenamientoAdapter(ejerciciosEntrenamiento)
        rvEjerciciosEntrenamiento.layoutManager = LinearLayoutManager(this)
        rvEjerciciosEntrenamiento.adapter = adapter

        // IMPORTANTE: Desactivar reciclaje para evitar pérdida de datos durante scroll
        // Durante un entrenamiento es crítico mantener todos los datos en memoria
        rvEjerciciosEntrenamiento.setItemViewCacheSize(100) // Cache muy grande
        rvEjerciciosEntrenamiento.isNestedScrollingEnabled = false

        // Desactivar completamente el reciclaje
        rvEjerciciosEntrenamiento.setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
            setMaxRecycledViews(0, 0) // No reciclar nada
        })

        (rvEjerciciosEntrenamiento.layoutManager as LinearLayoutManager).apply {
            // Mantener todas las vistas en memoria
            initialPrefetchItemCount = 50
        }

        // Desactivar animaciones para evitar problemas de estado
        rvEjerciciosEntrenamiento.itemAnimator = null

        // IMPORTANTE: Guardar estado automáticamente al hacer scroll
        // Esto asegura que no se pierdan datos cuando las vistas se reciclan
        rvEjerciciosEntrenamiento.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Guardar estado cuando el scroll comienza o termina
                if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                    newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    forceAdapterStateSave()
                }
            }
        })

        // Configurar botón finalizar
        btnFinalizarEntrenamiento.setOnClickListener {
            finalizarEntrenamiento()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmarSalir()
            }
        })
    }

    private fun cargarEjerciciosDeRutina() {
        lifecycleScope.launch {
            val rutina = rutinaRepository.getRutinaByNumeroSync(numeroRutina)
            val ejercicioIds = rutina?.getEjercicioIdsList() ?: emptyList()

            if (ejercicioIds.isNotEmpty()) {
                val ejercicios = ejercicioRepository.getEjerciciosByIds(ejercicioIds)

                // Obtener últimos registros para precargar valores
                val ultimosRegistros = registroRepository.getUltimosRegistrosDeRutina(numeroRutina)

                // Mantener el orden original y crear EjercicioEntrenamiento con valores anteriores
                val ejerciciosOrdenados = ejercicioIds.mapNotNull { id ->
                    ejercicios.find { it.id == id }
                }.map { ejercicio ->
                    val ultimoRegistro = ultimosRegistros[ejercicio.id]
                    val pesoAnterior = ultimoRegistro?.pesoKg ?: 0f
                    // Calcular repeticiones promedio por serie (asumiendo 3 series por defecto)
                    val repsTotalesAnteriores = ultimoRegistro?.repeticiones ?: 30
                    val repsPorSerie = (repsTotalesAnteriores / 3).coerceAtLeast(1)

                    // Crear series con el peso y repeticiones anteriores
                    val seriesIniciales = mutableListOf(
                        SerieEntrenamiento(repeticiones = repsPorSerie, pesoKg = pesoAnterior),
                        SerieEntrenamiento(repeticiones = repsPorSerie, pesoKg = pesoAnterior),
                        SerieEntrenamiento(repeticiones = repsPorSerie, pesoKg = pesoAnterior)
                    )

                    EjercicioEntrenamiento(
                        ejercicio = ejercicio,
                        series = seriesIniciales,
                        completado = false // Siempre inicia sin completar
                    )
                }

                ejerciciosEntrenamiento.clear()
                ejerciciosEntrenamiento.addAll(ejerciciosOrdenados)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun confirmarSalir() {
        // Guardar estado antes de mostrar el diálogo
        forceAdapterStateSave()

        AlertDialog.Builder(this)
            .setTitle(R.string.btn_volver)
            .setMessage(R.string.confirmar_salir_entrenamiento)
            .setPositiveButton(R.string.btn_volver) { _, _ ->
                // Resetear SyncManager al salir sin guardar
                SyncManager.reset()
                finish()
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    /**
     * Fuerza a todas las vistas visibles del RecyclerView a guardar su estado actual.
     * Esto es crítico para no perder datos antes de finalizar o salir.
     */
    private fun forceAdapterStateSave() {
        val layoutManager = rvEjerciciosEntrenamiento.layoutManager as? LinearLayoutManager ?: return

        // Iterar por todas las vistas visibles y forzar guardado
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val holder = rvEjerciciosEntrenamiento.getChildViewHolder(child)
            if (holder is EntrenamientoAdapter.EntrenamientoViewHolder) {
                holder.saveCurrentState()
            }
        }
    }

    private fun finalizarEntrenamiento() {
        lifecycleScope.launch {
            // IMPORTANTE: Forzar guardado del estado actual del adapter
            // Esto captura cualquier valor que esté en los EditText pero no se haya guardado
            forceAdapterStateSave()

            // Calcular tiempo de entrenamiento
            val tiempoFin = System.currentTimeMillis()
            val tiempoEntrenamientoMs = tiempoFin - tiempoInicio

            // Crear registros para cada ejercicio del entrenamiento
            val fechaEntrenamiento = tiempoFin
            val registros = ejerciciosEntrenamiento.map { ejercicioEntrenamiento ->
                // Calcular peso promedio de las series visibles para estadísticas
                val seriesVisibles = ejercicioEntrenamiento.series.take(ejercicioEntrenamiento.seriesVisibles)
                val pesoPromedio = if (seriesVisibles.isNotEmpty()) {
                    seriesVisibles.map { it.pesoKg }.average().toFloat()
                } else {
                    0f
                }

                // Calcular repeticiones totales de todas las series visibles
                val repeticionesTotales = ejercicioEntrenamiento.calcularRepeticionesTotales()

                RegistroEntrenamiento(
                    rutinaId = numeroRutina,
                    ejercicioId = ejercicioEntrenamiento.ejercicio.id,
                    repeticiones = repeticionesTotales, // Total de repeticiones de todas las series
                    pesoKg = pesoPromedio, // Peso promedio para estadísticas de progreso
                    completado = ejercicioEntrenamiento.completado,
                    fechaEntrenamiento = fechaEntrenamiento
                )
            }

            // 1. Guardar todos los registros en Room (local)
            registroRepository.guardarRegistros(registros)

            // 2. Calcular estadísticas del entrenamiento
            val ejerciciosCompletados = ejerciciosEntrenamiento.count { it.completado }
            // Usar calcularVolumenTotal() que suma correctamente todas las series visibles
            val volumenTotal = ejerciciosEntrenamiento
                .filter { it.completado }
                .sumOf { it.calcularVolumenTotal().toDouble() }
                .toFloat()

            // 3. IMPORTANTE: Desactivar modo entrenamiento ANTES de registrar estadísticas
            // Esto permite que se sincronice con Firebase
            SyncManager.endTrainingMode()

            // 4. Registrar estadísticas del día (ahora SÍ sincronizará con Firebase)
            estadisticaRepository.registrarEntrenamiento(
                tiempoMs = tiempoEntrenamientoMs,
                ejerciciosCompletados = ejerciciosCompletados,
                volumenTotal = volumenTotal,
                rutinaId = numeroRutina
            )

            // 5. Marcar sincronización como completada
            SyncManager.syncCompleted()

            Toast.makeText(this@EntrenamientoActivity, R.string.entrenamiento_guardado, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

