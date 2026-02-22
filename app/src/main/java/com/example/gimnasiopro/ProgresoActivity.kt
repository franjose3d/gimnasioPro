package com.example.gimnasiopro

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.data.EstadisticaRepository
import com.example.gimnasiopro.data.RegistroEntrenamientoRepository
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.EstadisticaFirestore
import com.example.gimnasiopro.data.firestore.EstadisticaFirestoreRepository
import com.example.gimnasiopro.data.firestore.EstadisticaRepositoryHibrido
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Activity que muestra el progreso del usuario en sus entrenamientos.
 * Muestra estadísticas de tiempo de hoy, mes, racha actual y equilibrio muscular.
 *
 * Soporta MODO TRAINER: el trainer ve las estadísticas del cliente
 * directamente desde Firestore (acceso directo, sin duplicar datos).
 */
class ProgresoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ProgresoActivity"
    }

    private lateinit var estadisticaRepository: EstadisticaRepositoryHibrido
    private lateinit var registroRepository: RegistroEntrenamientoRepository
    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido

    private lateinit var tvTiempoHoy: TextView
    private lateinit var tvTiempoMes: TextView
    private lateinit var tvEntrenamientosMes: TextView
    private lateinit var tvRachaActual: TextView
    private lateinit var tvPesoMovidoHoy: TextView
    private lateinit var tvRecordPesoMovido: TextView

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

    // Modo trainer: acceder a estadísticas del cliente desde Firestore
    private var modoTrainer = false
    private var clienteId: String? = null
    private var clienteNombre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progreso)

        // Detectar modo trainer
        modoTrainer = intent.getBooleanExtra("MODO_TRAINER", false)
        clienteId = intent.getStringExtra("CLIENTE_ID")
        clienteNombre = intent.getStringExtra("CLIENTE_NOMBRE")

        val app = application as GimnasioproApplication
        estadisticaRepository = app.estadisticaRepositoryHibrido
        registroRepository = app.registroEntrenamientoRepository
        ejercicioRepository = app.ejercicioRepository

        setupBackButton()
        setupViews()

        if (modoTrainer && clienteId != null) {
            Log.d(TAG, "📊 Modo TRAINER: viendo progreso de $clienteNombre ($clienteId)")
            loadStatsFromFirestore()
        } else {
            loadStats()
        }
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Si estamos en modo trainer, mostrar nombre del cliente en el título
        if (modoTrainer && clienteNombre != null) {
            try {
                val tvTitulo = findViewById<TextView>(R.id.tvTituloProgreso)
                tvTitulo?.text = "Progreso de $clienteNombre"
            } catch (_: Exception) {
                // Si no existe el TextView del título, no hacer nada
            }
        }
    }

    private fun setupViews() {
        tvTiempoHoy = findViewById(R.id.tvTiempoHoy)
        tvTiempoMes = findViewById(R.id.tvTiempoMes)
        tvEntrenamientosMes = findViewById(R.id.tvEntrenamientosMes)
        tvRachaActual = findViewById(R.id.tvRachaActual)
        tvPesoMovidoHoy = findViewById(R.id.tvPesoMovidoHoy)
        tvRecordPesoMovido = findViewById(R.id.tvRecordPesoMovido)

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

            // Cargar peso movido hoy (volumen = kg × repeticiones)
            val pesoHoy = estadisticaRepository.getVolumenHoy()
            tvPesoMovidoHoy.text = formatearPeso(pesoHoy)

            // Cargar récord de peso movido
            val recordPeso = estadisticaRepository.getRecordVolumen()
            tvRecordPesoMovido.text = formatearPeso(recordPeso)

            // Cargar equilibrio muscular
            loadEquilibrioMuscular()
        }
    }

    /**
     * Cargar estadísticas del cliente directamente desde Firestore (modo trainer).
     * El trainer accede al espacio Firestore del cliente sin duplicar datos.
     */
    private fun loadStatsFromFirestore() {
        lifecycleScope.launch {
            try {
                val firestoreRepo = EstadisticaFirestoreRepository(clienteId!!, "cliente")
                val calendar = Calendar.getInstance()
                val anio = calendar.get(Calendar.YEAR)
                val mes = calendar.get(Calendar.MONTH) + 1

                // Obtener estadísticas del mes actual del cliente
                val estadisticasMes = firestoreRepo.getEstadisticasPorMes(anio, mes).first()

                // Estadística de hoy
                val hoy = EstadisticaFirestore.formatFecha(Date())
                val estadisticaHoy = estadisticasMes.find { it.fecha == hoy }

                // Tiempo de hoy
                val tiempoHoyMs = estadisticaHoy?.tiempoEntrenamientoMs ?: 0L
                tvTiempoHoy.text = EstadisticaRepository.formatearTiempo(tiempoHoyMs)

                // Tiempo total del mes
                val tiempoMesMs = estadisticasMes.sumOf { it.tiempoEntrenamientoMs }
                tvTiempoMes.text = EstadisticaRepository.formatearTiempo(tiempoMesMs)

                // Entrenamientos del mes
                val entrenamientosMes = estadisticasMes.sumOf { it.numeroEntrenamientos }
                tvEntrenamientosMes.text = entrenamientosMes.toString()

                // Racha actual (contar días consecutivos hacia atrás)
                val racha = calcularRachaDesdeFirestore(estadisticasMes)
                tvRachaActual.text = racha.toString()

                // Peso movido hoy
                val pesoHoy = estadisticaHoy?.volumenTotal ?: 0f
                tvPesoMovidoHoy.text = formatearPeso(pesoHoy)

                // Récord de peso movido
                val recordPeso = estadisticasMes.maxOfOrNull { it.volumenTotal } ?: 0f
                tvRecordPesoMovido.text = formatearPeso(recordPeso)

                // Equilibrio muscular (simplificado para modo trainer)
                resetEquilibrioMuscular()

                Log.d(TAG, "✅ Estadísticas del cliente cargadas: ${estadisticasMes.size} días con datos")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando estadísticas del cliente: ${e.message}", e)
                Toast.makeText(this@ProgresoActivity, "Error al cargar estadísticas: ${e.message}", Toast.LENGTH_SHORT).show()
                // Poner valores por defecto
                tvTiempoHoy.text = EstadisticaRepository.formatearTiempo(0)
                tvTiempoMes.text = EstadisticaRepository.formatearTiempo(0)
                tvEntrenamientosMes.text = "0"
                tvRachaActual.text = "0"
                tvPesoMovidoHoy.text = formatearPeso(0f)
                tvRecordPesoMovido.text = formatearPeso(0f)
                resetEquilibrioMuscular()
            }
        }
    }

    /**
     * Calcula la racha de días consecutivos entrenando desde las estadísticas de Firestore.
     */
    private fun calcularRachaDesdeFirestore(estadisticas: List<EstadisticaFirestore>): Int {
        if (estadisticas.isEmpty()) return 0

        // Ordenar por fecha descendente
        val diasConEntrenamiento = estadisticas
            .filter { it.numeroEntrenamientos > 0 }
            .sortedByDescending { it.fechaTimestamp }

        if (diasConEntrenamiento.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        var racha = 0
        var diaEsperado = Calendar.getInstance()

        for (estadistica in diasConEntrenamiento) {
            calendar.time = estadistica.fechaTimestamp
            val mismoDia = calendar.get(Calendar.YEAR) == diaEsperado.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == diaEsperado.get(Calendar.DAY_OF_YEAR)

            if (mismoDia || racha == 0) {
                racha++
                diaEsperado.time = estadistica.fechaTimestamp
                diaEsperado.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return racha
    }

    /**
     * Resetea todas las barras de equilibrio muscular a 0.
     */
    private fun resetEquilibrioMuscular() {
        actualizarBarraProgreso(progressPectorales, tvPorcentajePectorales, 0)
        actualizarBarraProgreso(progressEspalda, tvPorcentajeEspalda, 0)
        actualizarBarraProgreso(progressHombros, tvPorcentajeHombros, 0)
        actualizarBarraProgreso(progressBiceps, tvPorcentajeBiceps, 0)
        actualizarBarraProgreso(progressTriceps, tvPorcentajeTriceps, 0)
        actualizarBarraProgreso(progressAbdominales, tvPorcentajeAbdominales, 0)
        actualizarBarraProgreso(progressPiernas, tvPorcentajePiernas, 0)
        actualizarBarraProgreso(progressGluteos, tvPorcentajeGluteos, 0)
        actualizarBarraProgreso(progressGemelos, tvPorcentajeGemelos, 0)
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

    private fun formatearPeso(pesoKg: Float): String {
        return when {
            pesoKg >= 1000 -> String.format(java.util.Locale.getDefault(), "%.1f t", pesoKg / 1000)
            pesoKg > 0 -> String.format(java.util.Locale.getDefault(), "%.0f kg", pesoKg)
            else -> "0 kg"
        }
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
        if (modoTrainer && clienteId != null) {
            loadStatsFromFirestore()
        } else {
            loadStats()
        }
    }
}

