package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gimnasiopro.components.CalendarView
import com.example.gimnasiopro.data.GymDatabase
import com.example.gimnasiopro.data.RutinaDiaSemana
import com.example.gimnasiopro.data.RutinaDiaSemanaRepository
import com.example.gimnasiopro.data.RutinaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Activity para la pantalla GIM.
 * Muestra un calendario y opciones para rutinas.
 * Permite asignar rutinas a días de la semana.
 */
class GimActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var rutinaDiaSemanaRepository: RutinaDiaSemanaRepository
    private lateinit var rutinaRepository: RutinaRepository
    private var diaSeleccionado: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gim)
        // TEST FIREBASE - TEMPORAL
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        android.util.Log.d("Firebase", "✅ Firebase conectado: ${db != null}")

        // Inicializar repositorios
        val database = GymDatabase.getDatabase(this)
        rutinaDiaSemanaRepository = RutinaDiaSemanaRepository(database.rutinaDiaSemanaDao())
        rutinaRepository = RutinaRepository(database.rutinaDao())

        // Inicializar día seleccionado con el día actual
        diaSeleccionado = getDiaSemanaActual()

        setupBackButton()
        setupCalendar()
        setupButtons()

        // ✅ NUEVO: Cargar días con rutina
        cargarDiasConRutina()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupCalendar() {
        val calendarContainer = findViewById<FrameLayout>(R.id.calendarContainer)
        calendarView = CalendarView(this)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        calendarView.layoutParams = params
        calendarContainer.addView(calendarView)

        calendarView.setOnDateSelectedListener(object : CalendarView.OnDateSelectedListener {
            override fun onDateSelected(year: Int, month: Int, day: Int) {
                val monthName = getMonthName(month)
                showToast("Fecha seleccionada: $day de $monthName de $year")

                // Calcular día de la semana del día seleccionado
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                diaSeleccionado = when (dayOfWeek) {
                    Calendar.MONDAY -> RutinaDiaSemana.LUNES
                    Calendar.TUESDAY -> RutinaDiaSemana.MARTES
                    Calendar.WEDNESDAY -> RutinaDiaSemana.MIERCOLES
                    Calendar.THURSDAY -> RutinaDiaSemana.JUEVES
                    Calendar.FRIDAY -> RutinaDiaSemana.VIERNES
                    Calendar.SATURDAY -> RutinaDiaSemana.SABADO
                    Calendar.SUNDAY -> RutinaDiaSemana.DOMINGO
                    else -> RutinaDiaSemana.LUNES
                }
            }
        })

        // ✅ NUEVO: Long press listener
        calendarView.setOnDateLongPressListener(object : CalendarView.OnDateLongPressListener {
            override fun onDateLongPress(year: Int, month: Int, day: Int, diaSemana: Int) {
                mostrarDialogoOpcionesRutina(diaSemana)
            }
        })
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnMiRutina).setOnClickListener {
            verificarRutinaDelDia()
        }

        findViewById<Button>(R.id.btnNuevaRutina).setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Obtiene el día de la semana actual (1=Lunes, 7=Domingo)
     */
    private fun getDiaSemanaActual(): Int {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.MONDAY -> RutinaDiaSemana.LUNES
            Calendar.TUESDAY -> RutinaDiaSemana.MARTES
            Calendar.WEDNESDAY -> RutinaDiaSemana.MIERCOLES
            Calendar.THURSDAY -> RutinaDiaSemana.JUEVES
            Calendar.FRIDAY -> RutinaDiaSemana.VIERNES
            Calendar.SATURDAY -> RutinaDiaSemana.SABADO
            Calendar.SUNDAY -> RutinaDiaSemana.DOMINGO
            else -> RutinaDiaSemana.LUNES
        }
    }

    /**
     * Carga qué días de la semana tienen rutina asignada y actualiza el calendario
     */
    private fun cargarDiasConRutina() {
        lifecycleScope.launch {
            val diasConRutina = mutableSetOf<Int>()

            // Verificar cada día de la semana
            for (dia in 1..7) {
                val numeroRutina = rutinaDiaSemanaRepository.getNumeroRutinaPorDia(dia)
                if (numeroRutina != null) {
                    diasConRutina.add(dia)
                }
            }

            runOnUiThread {
                calendarView.setDiasConRutina(diasConRutina)
            }
        }
    }

    /**
     * Verifica si hay una rutina asignada para el día seleccionado.
     * Si no hay, muestra diálogo para elegir o crear una.
     */
    private fun verificarRutinaDelDia() {
        val diaSemana = diaSeleccionado
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)

        lifecycleScope.launch {
            android.util.Log.d("GimActivity", "🟢 Verificando rutina del día: $nombreDia")
            val numeroRutina = rutinaDiaSemanaRepository.getNumeroRutinaPorDia(diaSemana)

            if (numeroRutina != null) {
                android.util.Log.d("GimActivity", "🟢 Ya tiene rutina asignada: $numeroRutina")
                // Hay rutina asignada, ir directamente al detalle
                val intent = Intent(this@GimActivity, DetalleRutinaActivity::class.java)
                intent.putExtra(DetalleRutinaActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
                startActivity(intent)
            } else {
                android.util.Log.d("GimActivity", "🟢 No tiene rutina, inicializando...")
                // Asegurar que las rutinas existen
                rutinaRepository.initializeRutinasIfNeeded()

                val rutinas = rutinaRepository.getAllRutinas().first()

                android.util.Log.d("GimActivity", "🔴 Rutinas obtenidas: ${rutinas.size}")
                android.util.Log.d("GimActivity", "🔴 isEmpty: ${rutinas.isEmpty()}")

                runOnUiThread {
                    if (rutinas.isEmpty()) {
                        android.util.Log.d("GimActivity", "❌ Mostrando diálogo SIN rutinas")
                        AlertDialog.Builder(this@GimActivity)
                            .setTitle("Sin rutinas")
                            .setMessage("No tienes ninguna rutina creada. ¿Quieres crear una nueva?")
                            .setPositiveButton("Crear rutina") { _, _ ->
                                val intent = Intent(this@GimActivity, RutinasActivity::class.java)
                                startActivity(intent)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        android.util.Log.d("GimActivity", "✅ Mostrando diálogo CON ${rutinas.size} rutinas")

                        val opciones = rutinas.map { rutina ->
                            val tieneEjercicios = rutina.ejercicioIds.isNotEmpty()
                            val estado = if (tieneEjercicios) " ✓" else " (vacía)"
                            "${rutina.nombre}$estado"
                        }.toMutableList()

                        opciones.add("➕ Crear nueva rutina")

                        AlertDialog.Builder(this@GimActivity)
                            .setTitle("Elige una rutina para los $nombreDia")
                            .setAdapter(ArrayAdapter(this@GimActivity, android.R.layout.simple_list_item_1, opciones)) { _, which ->
                                if (which < rutinas.size) {
                                    val rutinaSeleccionada = rutinas[which]
                                    asignarRutinaADia(diaSemana, rutinaSeleccionada.numeroRutina, nombreDia)
                                } else {
                                    val intent = Intent(this@GimActivity, RutinasActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            }
        }
    }

    /**
     * Muestra diálogo con opciones para cambiar o eliminar rutina (long press)
     */
    private fun mostrarDialogoOpcionesRutina(diaSemana: Int) {
        val nombreDia = RutinaDiaSemana.getNombreDia(diaSemana)

        AlertDialog.Builder(this)
            .setTitle("Rutina de los $nombreDia")
            .setItems(arrayOf("✏️ Cambiar rutina", "🗑️ Eliminar rutina")) { _, which ->
                when (which) {
                    0 -> cambiarRutinaDia(diaSemana, nombreDia)
                    1 -> eliminarRutinaDia(diaSemana, nombreDia)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cambia la rutina asignada a un día de la semana
     */
    private fun cambiarRutinaDia(diaSemana: Int, nombreDia: String) {
        lifecycleScope.launch {
            rutinaRepository.initializeRutinasIfNeeded()
            val rutinas = rutinaRepository.getAllRutinas().first()

            runOnUiThread {
                val opciones = rutinas.map { rutina ->
                    val tieneEjercicios = rutina.ejercicioIds.isNotEmpty()
                    val estado = if (tieneEjercicios) " ✓" else " (vacía)"
                    "${rutina.nombre}$estado"
                }.toMutableList()

                opciones.add("➕ Crear nueva rutina")

                AlertDialog.Builder(this@GimActivity)
                    .setTitle("Cambiar rutina de los $nombreDia")
                    .setAdapter(ArrayAdapter(this@GimActivity, android.R.layout.simple_list_item_1, opciones)) { _, which ->
                        if (which < rutinas.size) {
                            val rutinaSeleccionada = rutinas[which]
                            asignarRutinaADia(diaSemana, rutinaSeleccionada.numeroRutina, nombreDia)
                        } else {
                            val intent = Intent(this@GimActivity, RutinasActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    /**
     * Elimina la rutina asignada a un día de la semana
     */
    private fun eliminarRutinaDia(diaSemana: Int, nombreDia: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar rutina?")
            .setMessage("Se eliminará la rutina asignada a los $nombreDia")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    rutinaDiaSemanaRepository.eliminarRutinaDia(diaSemana)
                    runOnUiThread {
                        showToast("Rutina eliminada de los $nombreDia")
                        cargarDiasConRutina() // Refrescar calendario
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Asigna una rutina a un día de la semana y abre el detalle.
     */
    private fun asignarRutinaADia(diaSemana: Int, numeroRutina: Int, nombreDia: String) {
        lifecycleScope.launch {
            rutinaRepository.initializeRutinasIfNeeded()
            rutinaDiaSemanaRepository.asignarRutinaADia(diaSemana, numeroRutina)

            runOnUiThread {
                showToast("Rutina asignada a los $nombreDia")
                cargarDiasConRutina() // ✅ Refrescar calendario

                val intent = Intent(this@GimActivity, DetalleRutinaActivity::class.java)
                intent.putExtra(DetalleRutinaActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
                startActivity(intent)
            }
        }
    }

    private fun getMonthName(month: Int): String {
        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return months[month]
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}