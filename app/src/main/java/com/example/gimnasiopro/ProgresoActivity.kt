package com.example.gimnasiopro

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.RegistroEntrenamientoRepository
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import kotlinx.coroutines.launch

/**
 * Activity que muestra el progreso del usuario en sus entrenamientos.
 * Muestra estadísticas de tiempo de hoy, mes, racha actual y equilibrio muscular.
 */
class ProgresoActivity : AppCompatActivity() {

    private lateinit var estadisticaRepository: EstadisticaRepository
    private lateinit var registroRepository: RegistroEntrenamientoRepository
    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido

    private lateinit var tvTiempoHoy: TextView
    private lateinit var tvTiempoMes: TextView
    private lateinit var tvEntrenamientosMes: TextView
    private lateinit var tvRachaActual: TextView

    // Progress bars y textos de porcentaje para grupos musculares
    private lateinit var progressPectorales: ProgressBar
    private lateinit var progressEspalda: ProgressBar
    private lateinit var progressHombros: ProgressBar
    private lateinit var progressBiceps: ProgressBar
    private lateinit var progressTriceps: ProgressBar
    private lateinit var progressAbdominales: ProgressBar
    private lateinit var progressPiernas: ProgressBar
    private lateinit var progressGluteos: ProgressBar
    private lateinit var progressGemelos: ProgressBar

    private lateinit var tvPorcentajePectorales: TextView
    private lateinit var tvPorcentajeEspalda: TextView
    private lateinit var tvPorcentajeHombros: TextView
    private lateinit var tvPorcentajeBiceps: TextView
    private lateinit var tvPorcentajeTriceps: TextView
    private lateinit var tvPorcentajeAbdominales: TextView
    private lateinit var tvPorcentajePiernas: TextView
    private lateinit var tvPorcentajeGluteos: TextView
    private lateinit var tvPorcentajeGemelos: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progreso)

        val app = application as GimnasioproApplication
        estadisticaRepository = app.estadisticaRepository
        registroRepository = app.registroEntrenamientoRepository
        ejercicioRepository = app.ejercicioRepository

        setupBackButton()
        setupViews()
        loadStats()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        tvTiempoHoy = findViewById(R.id.tvTiempoHoy)
        tvTiempoMes = findViewById(R.id.tvTiempoMes)
        tvEntrenamientosMes = findViewById(R.id.tvEntrenamientosMes)
        tvRachaActual = findViewById(R.id.tvRachaActual)

        // Progress bars
        progressPectorales = findViewById(R.id.progressPectorales)
        progressEspalda = findViewById(R.id.progressEspalda)
        progressHombros = findViewById(R.id.progressHombros)
        progressBiceps = findViewById(R.id.progressBiceps)
        progressTriceps = findViewById(R.id.progressTriceps)
        progressAbdominales = findViewById(R.id.progressAbdominales)
        progressPiernas = findViewById(R.id.progressPiernas)
        progressGluteos = findViewById(R.id.progressGluteos)
        progressGemelos = findViewById(R.id.progressGemelos)

        // Textos de porcentaje
        tvPorcentajePectorales = findViewById(R.id.tvPorcentajePectorales)
        tvPorcentajeEspalda = findViewById(R.id.tvPorcentajeEspalda)
        tvPorcentajeHombros = findViewById(R.id.tvPorcentajeHombros)
        tvPorcentajeBiceps = findViewById(R.id.tvPorcentajeBiceps)
        tvPorcentajeTriceps = findViewById(R.id.tvPorcentajeTriceps)
        tvPorcentajeAbdominales = findViewById(R.id.tvPorcentajeAbdominales)
        tvPorcentajePiernas = findViewById(R.id.tvPorcentajePiernas)
        tvPorcentajeGluteos = findViewById(R.id.tvPorcentajeGluteos)
        tvPorcentajeGemelos = findViewById(R.id.tvPorcentajeGemelos)
    }

    private fun loadStats() {
        lifecycleScope.launch {
            // Cargar tiempo de entrenamiento de hoy
            val tiempoHoyMs = estadisticaRepository.getTiempoHoy()
            tvTiempoHoy.text = EstadisticaRepository.formatearTiempo(tiempoHoyMs)

            // Cargar tiempo total del mes
            val tiempoMesMs = estadisticaRepository.getTiempoMesActual()
            tvTiempoMes.text = EstadisticaRepository.formatearTiempo(tiempoMesMs)

            // Cargar número de entrenamientos del mes
            val entrenamientosMes = estadisticaRepository.getEntrenamientosMesActual()
            tvEntrenamientosMes.text = entrenamientosMes.toString()

            // Cargar racha actual
            val racha = estadisticaRepository.getRachaActual()
            tvRachaActual.text = racha.toString()

            // Cargar equilibrio muscular
            loadEquilibrioMuscular()
        }
    }

    private suspend fun loadEquilibrioMuscular() {
        // Obtener fechas del mes actual
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis

        calendar.add(java.util.Calendar.MONTH, 1)
        val finMes = calendar.timeInMillis

        // Obtener ejercicios completados del mes
        val ejerciciosCompletadosIds = registroRepository.getEjerciciosCompletadosEntreFechas(inicioMes, finMes)

        // Obtener conteo por grupo muscular
        val conteosPorGrupo = ejercicioRepository.getConteoPorGrupoMuscular(ejerciciosCompletadosIds)

        // Calcular el total de ejercicios completados
        val totalEjercicios = conteosPorGrupo.values.sum()

        if (totalEjercicios == 0) {
            // Si no hay ejercicios, todas las barras a 0
            actualizarBarraProgreso(progressPectorales, tvPorcentajePectorales, 0)
            actualizarBarraProgreso(progressEspalda, tvPorcentajeEspalda, 0)
            actualizarBarraProgreso(progressHombros, tvPorcentajeHombros, 0)
            actualizarBarraProgreso(progressBiceps, tvPorcentajeBiceps, 0)
            actualizarBarraProgreso(progressTriceps, tvPorcentajeTriceps, 0)
            actualizarBarraProgreso(progressAbdominales, tvPorcentajeAbdominales, 0)
            actualizarBarraProgreso(progressPiernas, tvPorcentajePiernas, 0)
            actualizarBarraProgreso(progressGluteos, tvPorcentajeGluteos, 0)
            actualizarBarraProgreso(progressGemelos, tvPorcentajeGemelos, 0)
            return
        }

        // Calcular porcentaje de cada grupo (el ideal sería ~11% cada uno para 9 grupos)
        val porcentajePectorales = calcularPorcentaje(conteosPorGrupo["Pectorales"] ?: 0, totalEjercicios)
        val porcentajeEspalda = calcularPorcentaje(conteosPorGrupo["Espalda"] ?: 0, totalEjercicios)
        val porcentajeHombros = calcularPorcentaje(conteosPorGrupo["Hombros"] ?: 0, totalEjercicios)
        val porcentajeBiceps = calcularPorcentaje(conteosPorGrupo["Bíceps y Antebrazo"] ?: 0, totalEjercicios)
        val porcentajeTriceps = calcularPorcentaje(conteosPorGrupo["Tríceps"] ?: 0, totalEjercicios)
        val porcentajeAbdominales = calcularPorcentaje(conteosPorGrupo["Abdominales"] ?: 0, totalEjercicios)
        val porcentajePiernas = calcularPorcentaje(conteosPorGrupo["Piernas"] ?: 0, totalEjercicios)
        val porcentajeGluteos = calcularPorcentaje(conteosPorGrupo["Glúteos"] ?: 0, totalEjercicios)
        val porcentajeGemelos = calcularPorcentaje(conteosPorGrupo["Gemelos"] ?: 0, totalEjercicios)

        // Actualizar UI
        actualizarBarraProgreso(progressPectorales, tvPorcentajePectorales, porcentajePectorales)
        actualizarBarraProgreso(progressEspalda, tvPorcentajeEspalda, porcentajeEspalda)
        actualizarBarraProgreso(progressHombros, tvPorcentajeHombros, porcentajeHombros)
        actualizarBarraProgreso(progressBiceps, tvPorcentajeBiceps, porcentajeBiceps)
        actualizarBarraProgreso(progressTriceps, tvPorcentajeTriceps, porcentajeTriceps)
        actualizarBarraProgreso(progressAbdominales, tvPorcentajeAbdominales, porcentajeAbdominales)
        actualizarBarraProgreso(progressPiernas, tvPorcentajePiernas, porcentajePiernas)
        actualizarBarraProgreso(progressGluteos, tvPorcentajeGluteos, porcentajeGluteos)
        actualizarBarraProgreso(progressGemelos, tvPorcentajeGemelos, porcentajeGemelos)
    }

    private fun calcularPorcentaje(cantidad: Int, total: Int): Int {
        if (total == 0) return 0
        return ((cantidad.toFloat() / total) * 100).toInt()
    }

    private fun actualizarBarraProgreso(progressBar: ProgressBar, textView: TextView, porcentaje: Int) {
        progressBar.progress = porcentaje
        textView.text = "$porcentaje%"
    }

    override fun onResume() {
        super.onResume()
        // Recargar estadísticas al volver a la pantalla
        loadStats()
    }
}

