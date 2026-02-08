package com.example.gimnasiopro

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RutinasActivity : AppCompatActivity() {

    private lateinit var rutinaRepository: RutinaRepository
    private lateinit var allRutinaButtons: List<Triple<Button, Int, LinearLayout?>>
    private lateinit var tvContadorRutinas: TextView

    private var cantidadRutinas = 10
    private val minRutinas = 1
    private val maxRutinas = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutinas)

        // Inicializar repositorio
        val database = GymDatabase.getDatabase(this)
        rutinaRepository = RutinaRepository(database.rutinaDao())

        tvContadorRutinas = findViewById(R.id.tvContadorRutinas)

        setupBackButton()
        setupRutinaButtons()
        setupIncrementDecrementButtons()

        // Cargar cantidad de rutinas actual
        cargarCantidadRutinas()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar colores y nombres cada vez que volvemos a esta pantalla
        actualizarRutinas()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupRutinaButtons() {
        // Lista de todos los botones con su número y fila contenedora (para los adicionales)
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
            // Click normal - abrir detalle
            button.setOnClickListener {
                onRutinaSelected(numero)
            }

            // Long press - cambiar nombre
            button.setOnLongClickListener {
                mostrarDialogoCambiarNombre(button, numero)
                true
            }
        }

        // Cargar datos iniciales
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
            cantidadRutinas = rutinaRepository.getCountRutinas()
            if (cantidadRutinas < minRutinas) {
                cantidadRutinas = 10
                rutinaRepository.initializeRutinasIfNeeded()
            }
            runOnUiThread {
                actualizarUIRutinas()
            }
        }
    }

    private fun incrementarRutinas() {
        if (cantidadRutinas >= maxRutinas) {
            Toast.makeText(this, "Máximo $maxRutinas rutinas", Toast.LENGTH_SHORT).show()
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

        // Confirmar antes de eliminar
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

    private fun actualizarUIRutinas() {
        tvContadorRutinas.text = cantidadRutinas.toString()

        // Mostrar/ocultar filas de rutinas adicionales
        allRutinaButtons.forEach { (button, numero, row) ->
            if (numero <= 10) {
                // Los primeros 10 siempre visibles pero solo activos si existen
                button.visibility = if (numero <= cantidadRutinas) View.VISIBLE else View.INVISIBLE
                button.isEnabled = numero <= cantidadRutinas
            } else {
                // Los adicionales (11-20) controlan la visibilidad de la fila
                row?.visibility = when {
                    numero % 2 == 1 -> { // Primer botón de la fila (impar)
                        if (numero <= cantidadRutinas || (numero + 1) <= cantidadRutinas) View.VISIBLE else View.GONE
                    }
                    else -> row.visibility // Mantener visibilidad actual para el segundo botón
                }
                button.visibility = if (numero <= cantidadRutinas) View.VISIBLE else View.INVISIBLE
                button.isEnabled = numero <= cantidadRutinas
            }
        }

        actualizarRutinas()
    }

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

        // Mostrar teclado automáticamente
        editText.requestFocus()
    }

    private fun guardarNombreRutina(button: Button, numeroRutina: Int, nuevoNombre: String) {
        lifecycleScope.launch {
            try {
                rutinaRepository.actualizarNombreRutina(numeroRutina, nuevoNombre)
                runOnUiThread {
                    button.text = nuevoNombre
                    Toast.makeText(
                        this@RutinasActivity,
                        R.string.nombre_rutina_actualizado,
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun actualizarRutinas() {
        lifecycleScope.launch {
            allRutinaButtons.forEach { (button, numero, _) ->
                if (numero <= cantidadRutinas) {
                    try {
                        val rutina = rutinaRepository.getRutinaByNumero(numero).first()
                        val tieneEjercicios = rutina?.ejercicioIds?.isNotEmpty() == true
                        val nombreRutina = rutina?.nombre ?: "Rutina $numero"

                        runOnUiThread {
                            // Actualizar nombre del botón
                            button.text = nombreRutina

                            // Actualizar color según si tiene ejercicios
                            val colorRes = if (tieneEjercicios) {
                                R.color.rutina_asignada
                            } else {
                                R.color.rutina_vacia
                            }
                            button.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(this@RutinasActivity, colorRes)
                            )
                        }
                    } catch (_: Exception) {
                        runOnUiThread {
                            button.text = "Rutina $numero"
                            button.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(this@RutinasActivity, R.color.rutina_vacia)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onRutinaSelected(numeroRutina: Int) {
        val intent = Intent(this, DetalleRutinaActivity::class.java)
        intent.putExtra(DetalleRutinaActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
        startActivity(intent)
    }
}

