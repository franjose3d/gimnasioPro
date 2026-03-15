package com.example.gimnasiopro.components

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.EjercicioEntrenamiento
import com.example.gimnasiopro.data.SerieEntrenamiento
import java.net.URLEncoder
import java.util.Locale

/**
 * Adapter para mostrar la lista de ejercicios durante el entrenamiento.
 * Permite modificar el peso de cada serie de cada ejercicio.
 *
 * IMPORTANTE: Usa setHasStableIds(true) para evitar que el RecyclerView
 * pierda el estado de las vistas durante el scroll/reciclaje.
 */
class EntrenamientoAdapter(
    private val ejercicios: MutableList<EjercicioEntrenamiento>,
    private val onEjercicioCompletado: (Int, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<EntrenamientoAdapter.EntrenamientoViewHolder>() {

    companion object {
        private const val KG_INCREMENT = 5f
        private const val MIN_KG = 0f
        private const val REP_INCREMENT = 1
        private const val MIN_REP = 1
        private const val DEFAULT_REP = 10
        private const val MIN_SERIES = 1
        private const val MAX_SERIES = 6
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return ejercicios[position].ejercicio.id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntrenamientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio_entrenamiento, parent, false)
        return EntrenamientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntrenamientoViewHolder, position: Int) {
        holder.saveCurrentState()
        holder.bind(ejercicios[position], position)
    }

    override fun onViewRecycled(holder: EntrenamientoViewHolder) {
        holder.saveCurrentState()
        holder.cleanup()
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int {
        return ejercicios.getOrNull(position)?.ejercicio?.id?.toInt() ?: position
    }

    override fun getItemCount(): Int = ejercicios.size

    fun getEjercicios(): List<EjercicioEntrenamiento> = ejercicios.toList()

    private fun getEjercicioAt(position: Int): EjercicioEntrenamiento? {
        return if (position in 0 until ejercicios.size) ejercicios[position] else null
    }

    inner class EntrenamientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvNombreEjercicio: TextView = itemView.findViewById(R.id.tvNombreEjercicio)
        private val checkboxCompletado: CheckBox = itemView.findViewById(R.id.checkboxCompletado)
        private val btnVerVideo: ImageButton = itemView.findViewById(R.id.btnVerVideo)

        private var currentEjercicio: EjercicioEntrenamiento? = null
        private var currentPosition: Int = -1

        // Contenedores de cada serie
        private val containerSerie1: LinearLayout = itemView.findViewById(R.id.containerSerie1)
        private val containerSerie2: LinearLayout = itemView.findViewById(R.id.containerSerie2)
        private val containerSerie3: LinearLayout = itemView.findViewById(R.id.containerSerie3)
        private val containerSerie4: LinearLayout = itemView.findViewById(R.id.containerSerie4)
        private val containerSerie5: LinearLayout = itemView.findViewById(R.id.containerSerie5)
        private val containerSerie6: LinearLayout = itemView.findViewById(R.id.containerSerie6)

        // EditTexts de peso
        private val etPeso1: EditText = itemView.findViewById(R.id.etPeso1)
        private val etPeso2: EditText = itemView.findViewById(R.id.etPeso2)
        private val etPeso3: EditText = itemView.findViewById(R.id.etPeso3)
        private val etPeso4: EditText = itemView.findViewById(R.id.etPeso4)
        private val etPeso5: EditText = itemView.findViewById(R.id.etPeso5)
        private val etPeso6: EditText = itemView.findViewById(R.id.etPeso6)

        // EditTexts de repeticiones
        private val etRep1: EditText = itemView.findViewById(R.id.etRep1)
        private val etRep2: EditText = itemView.findViewById(R.id.etRep2)
        private val etRep3: EditText = itemView.findViewById(R.id.etRep3)
        private val etRep4: EditText = itemView.findViewById(R.id.etRep4)
        private val etRep5: EditText = itemView.findViewById(R.id.etRep5)
        private val etRep6: EditText = itemView.findViewById(R.id.etRep6)

        // Botones kg
        private val btnIncrementarKg1: ImageButton = itemView.findViewById(R.id.btnIncrementarKg1)
        private val btnDecrementarKg1: ImageButton = itemView.findViewById(R.id.btnDecrementarKg1)
        private val btnIncrementarKg2: ImageButton = itemView.findViewById(R.id.btnIncrementarKg2)
        private val btnDecrementarKg2: ImageButton = itemView.findViewById(R.id.btnDecrementarKg2)
        private val btnIncrementarKg3: ImageButton = itemView.findViewById(R.id.btnIncrementarKg3)
        private val btnDecrementarKg3: ImageButton = itemView.findViewById(R.id.btnDecrementarKg3)
        private val btnIncrementarKg4: ImageButton = itemView.findViewById(R.id.btnIncrementarKg4)
        private val btnDecrementarKg4: ImageButton = itemView.findViewById(R.id.btnDecrementarKg4)
        private val btnIncrementarKg5: ImageButton = itemView.findViewById(R.id.btnIncrementarKg5)
        private val btnDecrementarKg5: ImageButton = itemView.findViewById(R.id.btnDecrementarKg5)
        private val btnIncrementarKg6: ImageButton = itemView.findViewById(R.id.btnIncrementarKg6)
        private val btnDecrementarKg6: ImageButton = itemView.findViewById(R.id.btnDecrementarKg6)

        // Botones rep
        private val btnIncrementarRep1: ImageButton = itemView.findViewById(R.id.btnIncrementarRep1)
        private val btnDecrementarRep1: ImageButton = itemView.findViewById(R.id.btnDecrementarRep1)
        private val btnIncrementarRep2: ImageButton = itemView.findViewById(R.id.btnIncrementarRep2)
        private val btnDecrementarRep2: ImageButton = itemView.findViewById(R.id.btnDecrementarRep2)
        private val btnIncrementarRep3: ImageButton = itemView.findViewById(R.id.btnIncrementarRep3)
        private val btnDecrementarRep3: ImageButton = itemView.findViewById(R.id.btnDecrementarRep3)
        private val btnIncrementarRep4: ImageButton = itemView.findViewById(R.id.btnIncrementarRep4)
        private val btnDecrementarRep4: ImageButton = itemView.findViewById(R.id.btnDecrementarRep4)
        private val btnIncrementarRep5: ImageButton = itemView.findViewById(R.id.btnIncrementarRep5)
        private val btnDecrementarRep5: ImageButton = itemView.findViewById(R.id.btnDecrementarRep5)
        private val btnIncrementarRep6: ImageButton = itemView.findViewById(R.id.btnIncrementarRep6)
        private val btnDecrementarRep6: ImageButton = itemView.findViewById(R.id.btnDecrementarRep6)

        // Botones quitar serie (uno por fila, aparece en la última fila visible)
        private val btnQuitarSerie1: ImageButton = itemView.findViewById(R.id.btnQuitarSerie1)
        private val btnQuitarSerie2: ImageButton = itemView.findViewById(R.id.btnQuitarSerie2)
        private val btnQuitarSerie3: ImageButton = itemView.findViewById(R.id.btnQuitarSerie3)
        private val btnQuitarSerie4: ImageButton = itemView.findViewById(R.id.btnQuitarSerie4)
        private val btnQuitarSerie5: ImageButton = itemView.findViewById(R.id.btnQuitarSerie5)
        private val btnQuitarSerie6: ImageButton = itemView.findViewById(R.id.btnQuitarSerie6)

        // Botón único para añadir serie (debajo de la última fila visible)
        private val btnAgregarSerie: ImageButton = itemView.findViewById(R.id.btnAgregarSerie)

        fun bind(ejercicioEntrenamiento: EjercicioEntrenamiento, position: Int) {
            currentEjercicio = ejercicioEntrenamiento
            currentPosition = position

            tvNombreEjercicio.text = ejercicioEntrenamiento.ejercicio.nombre

            checkboxCompletado.setOnCheckedChangeListener(null)
            checkboxCompletado.isChecked = ejercicioEntrenamiento.completado
            actualizarEstiloCompletado(ejercicioEntrenamiento.completado)
            checkboxCompletado.setOnCheckedChangeListener { _, isChecked ->
                currentEjercicio?.let { ejercicio ->
                    ejercicio.completado = isChecked
                    actualizarEstiloCompletado(isChecked)
                    onEjercicioCompletado(bindingAdapterPosition, isChecked)
                }
            }

            btnVerVideo.setOnClickListener {
                currentEjercicio?.let { abrirVideoYouTube(it.ejercicio.nombre) }
            }

            actualizarVisibilidadSeries(ejercicioEntrenamiento)
            actualizarPesosSeries(ejercicioEntrenamiento)
            actualizarRepeticionesSeries(ejercicioEntrenamiento)

            configurarBotonesSeriePeso(0, ejercicioEntrenamiento, etPeso1, btnIncrementarKg1, btnDecrementarKg1)
            configurarBotonesSeriePeso(1, ejercicioEntrenamiento, etPeso2, btnIncrementarKg2, btnDecrementarKg2)
            configurarBotonesSeriePeso(2, ejercicioEntrenamiento, etPeso3, btnIncrementarKg3, btnDecrementarKg3)
            configurarBotonesSeriePeso(3, ejercicioEntrenamiento, etPeso4, btnIncrementarKg4, btnDecrementarKg4)
            configurarBotonesSeriePeso(4, ejercicioEntrenamiento, etPeso5, btnIncrementarKg5, btnDecrementarKg5)
            configurarBotonesSeriePeso(5, ejercicioEntrenamiento, etPeso6, btnIncrementarKg6, btnDecrementarKg6)

            configurarBotonesSerieRep(0, ejercicioEntrenamiento, etRep1, btnIncrementarRep1, btnDecrementarRep1)
            configurarBotonesSerieRep(1, ejercicioEntrenamiento, etRep2, btnIncrementarRep2, btnDecrementarRep2)
            configurarBotonesSerieRep(2, ejercicioEntrenamiento, etRep3, btnIncrementarRep3, btnDecrementarRep3)
            configurarBotonesSerieRep(3, ejercicioEntrenamiento, etRep4, btnIncrementarRep4, btnDecrementarRep4)
            configurarBotonesSerieRep(4, ejercicioEntrenamiento, etRep5, btnIncrementarRep5, btnDecrementarRep5)
            configurarBotonesSerieRep(5, ejercicioEntrenamiento, etRep6, btnIncrementarRep6, btnDecrementarRep6)

            configurarBotonesAgregarQuitarSerie(ejercicioEntrenamiento)
        }

        private fun configurarBotonesAgregarQuitarSerie(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            // Botón único "+" debajo de la última fila
            btnAgregarSerie.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                if (ejercicio.seriesVisibles < MAX_SERIES) {
                    ejercicio.seriesVisibles++
                    actualizarVisibilidadSeries(ejercicio)
                }
            }

            // Botón "-" en cada fila — quita la última serie visible
            val quitarListener = View.OnClickListener {
                val ejercicio = currentEjercicio ?: return@OnClickListener
                if (ejercicio.seriesVisibles > MIN_SERIES) {
                    ejercicio.seriesVisibles--
                    actualizarVisibilidadSeries(ejercicio)
                }
            }

            btnQuitarSerie1.setOnClickListener(quitarListener)
            btnQuitarSerie2.setOnClickListener(quitarListener)
            btnQuitarSerie3.setOnClickListener(quitarListener)
            btnQuitarSerie4.setOnClickListener(quitarListener)
            btnQuitarSerie5.setOnClickListener(quitarListener)
            btnQuitarSerie6.setOnClickListener(quitarListener)
        }

        private fun actualizarVisibilidadSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val visibles = ejercicioEntrenamiento.seriesVisibles

            // Mostrar/ocultar filas
            containerSerie1.visibility = View.VISIBLE
            containerSerie2.visibility = if (visibles >= 2) View.VISIBLE else View.GONE
            containerSerie3.visibility = if (visibles >= 3) View.VISIBLE else View.GONE
            containerSerie4.visibility = if (visibles >= 4) View.VISIBLE else View.GONE
            containerSerie5.visibility = if (visibles >= 5) View.VISIBLE else View.GONE
            containerSerie6.visibility = if (visibles >= 6) View.VISIBLE else View.GONE

            // Ocultar todos los botones quitar
            btnQuitarSerie1.visibility = View.GONE
            btnQuitarSerie2.visibility = View.GONE
            btnQuitarSerie3.visibility = View.GONE
            btnQuitarSerie4.visibility = View.GONE
            btnQuitarSerie5.visibility = View.GONE
            btnQuitarSerie6.visibility = View.GONE

            // Mostrar "-" solo en la última fila visible, y solo si hay más de 1 serie
            if (visibles > MIN_SERIES) {
                when (visibles) {
                    2 -> btnQuitarSerie2.visibility = View.VISIBLE
                    3 -> btnQuitarSerie3.visibility = View.VISIBLE
                    4 -> btnQuitarSerie4.visibility = View.VISIBLE
                    5 -> btnQuitarSerie5.visibility = View.VISIBLE
                    6 -> btnQuitarSerie6.visibility = View.VISIBLE
                }
            }

            // Botón "+" único: visible solo si no se ha llegado al máximo
            btnAgregarSerie.visibility = if (visibles < MAX_SERIES) View.VISIBLE else View.GONE
        }

        private fun abrirVideoYouTube(nombreEjercicio: String) {
            try {
                val query = URLEncoder.encode("$nombreEjercicio ejercicio gym", "UTF-8")
                val youtubeUrl = "https://www.youtube.com/results?search_query=$query"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                itemView.context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                itemView.context.startActivity(intent)
            }
        }

        private fun configurarBotonesSeriePeso(
            indice: Int,
            ejercicioEntrenamiento: EjercicioEntrenamiento,
            etPeso: EditText,
            btnIncrementar: ImageButton,
            btnDecrementar: ImageButton
        ) {
            btnIncrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                val serie = ejercicio.series.getOrNull(indice) ?: return@setOnClickListener
                serie.pesoKg += KG_INCREMENT
                actualizarPesoEditText(etPeso, serie.pesoKg)
            }
            btnDecrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                val serie = ejercicio.series.getOrNull(indice) ?: return@setOnClickListener
                serie.pesoKg = if (serie.pesoKg >= KG_INCREMENT) serie.pesoKg - KG_INCREMENT else MIN_KG
                actualizarPesoEditText(etPeso, serie.pesoKg)
            }
            etPeso.setOnClickListener {
                currentEjercicio?.let { ejercicio -> mostrarDialogoPeso(ejercicio, indice, etPeso) }
            }
        }

        private fun configurarBotonesSerieRep(
            indice: Int,
            ejercicioEntrenamiento: EjercicioEntrenamiento,
            etRep: EditText,
            btnIncrementar: ImageButton,
            btnDecrementar: ImageButton
        ) {
            btnIncrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                val serie = ejercicio.series.getOrNull(indice) ?: return@setOnClickListener
                serie.repeticiones += REP_INCREMENT
                actualizarRepEditText(etRep, serie.repeticiones)
            }
            btnDecrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                val serie = ejercicio.series.getOrNull(indice) ?: return@setOnClickListener
                if (serie.repeticiones > MIN_REP) serie.repeticiones -= REP_INCREMENT
                actualizarRepEditText(etRep, serie.repeticiones)
            }
            etRep.setOnClickListener {
                currentEjercicio?.let { ejercicio -> mostrarDialogoRepeticiones(ejercicio, indice, etRep) }
            }
        }

        private fun actualizarPesosSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val s = ejercicioEntrenamiento.series
            if (s.size > 0) actualizarPesoEditText(etPeso1, s[0].pesoKg)
            if (s.size > 1) actualizarPesoEditText(etPeso2, s[1].pesoKg)
            if (s.size > 2) actualizarPesoEditText(etPeso3, s[2].pesoKg)
            if (s.size > 3) actualizarPesoEditText(etPeso4, s[3].pesoKg)
            if (s.size > 4) actualizarPesoEditText(etPeso5, s[4].pesoKg)
            if (s.size > 5) actualizarPesoEditText(etPeso6, s[5].pesoKg)
        }

        private fun actualizarRepeticionesSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val s = ejercicioEntrenamiento.series
            if (s.size > 0) actualizarRepEditText(etRep1, s[0].repeticiones)
            if (s.size > 1) actualizarRepEditText(etRep2, s[1].repeticiones)
            if (s.size > 2) actualizarRepEditText(etRep3, s[2].repeticiones)
            if (s.size > 3) actualizarRepEditText(etRep4, s[3].repeticiones)
            if (s.size > 4) actualizarRepEditText(etRep5, s[4].repeticiones)
            if (s.size > 5) actualizarRepEditText(etRep6, s[5].repeticiones)
        }

        private fun actualizarPesoEditText(editText: EditText, peso: Float) {
            editText.setText(if (peso % 1 == 0f) peso.toInt().toString() else String.format(Locale.getDefault(), "%.1f", peso))
        }

        private fun actualizarRepEditText(editText: EditText, repeticiones: Int) {
            editText.setText(repeticiones.toString())
        }

        private fun mostrarDialogoPeso(ejercicioEntrenamiento: EjercicioEntrenamiento, indice: Int, etPeso: EditText) {
            if (indice >= ejercicioEntrenamiento.series.size) return
            val context = itemView.context
            val editText = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(ejercicioEntrenamiento.series[indice].pesoKg.toString())
                hint = context.getString(R.string.hint_peso)
                setPadding(50, 30, 50, 30)
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.titulo_ingresar_peso)
                .setView(editText)
                .setPositiveButton(R.string.btn_guardar) { _, _ ->
                    val valor = editText.text.toString().toFloatOrNull() ?: 0f
                    ejercicioEntrenamiento.series[indice].pesoKg = maxOf(valor, MIN_KG)
                    actualizarPesoEditText(etPeso, ejercicioEntrenamiento.series[indice].pesoKg)
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }

        private fun mostrarDialogoRepeticiones(ejercicioEntrenamiento: EjercicioEntrenamiento, indice: Int, etRep: EditText) {
            if (indice >= ejercicioEntrenamiento.series.size) return
            val context = itemView.context
            val editText = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(ejercicioEntrenamiento.series[indice].repeticiones.toString())
                hint = context.getString(R.string.label_rep)
                setPadding(50, 30, 50, 30)
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.label_rep)
                .setView(editText)
                .setPositiveButton(R.string.btn_guardar) { _, _ ->
                    val valor = editText.text.toString().toIntOrNull() ?: DEFAULT_REP
                    ejercicioEntrenamiento.series[indice].repeticiones = maxOf(valor, MIN_REP)
                    actualizarRepEditText(etRep, ejercicioEntrenamiento.series[indice].repeticiones)
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }

        private fun actualizarEstiloCompletado(completado: Boolean) {
            if (completado) {
                tvNombreEjercicio.paintFlags = tvNombreEjercicio.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemView.alpha = 0.6f
            } else {
                tvNombreEjercicio.paintFlags = tvNombreEjercicio.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemView.alpha = 1.0f
            }
        }

        fun cleanup() {
            checkboxCompletado.setOnCheckedChangeListener(null)
            btnVerVideo.setOnClickListener(null)
        }

        fun saveCurrentState() {
            val ejercicio = currentEjercicio ?: return
            if (currentPosition < 0) return
            try {
                ejercicio.series.getOrNull(0)?.pesoKg = etPeso1.text.toString().toFloatOrNull() ?: ejercicio.series[0].pesoKg
                ejercicio.series.getOrNull(1)?.pesoKg = etPeso2.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(1) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(2)?.pesoKg = etPeso3.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(2) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(3)?.pesoKg = etPeso4.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(3) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(4)?.pesoKg = etPeso5.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(4) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(5)?.pesoKg = etPeso6.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(5) { SerieEntrenamiento() }.pesoKg

                ejercicio.series.getOrNull(0)?.repeticiones = etRep1.text.toString().toIntOrNull() ?: ejercicio.series[0].repeticiones
                ejercicio.series.getOrNull(1)?.repeticiones = etRep2.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(1) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(2)?.repeticiones = etRep3.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(2) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(3)?.repeticiones = etRep4.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(3) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(4)?.repeticiones = etRep5.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(4) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(5)?.repeticiones = etRep6.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(5) { SerieEntrenamiento() }.repeticiones

                ejercicio.completado = checkboxCompletado.isChecked
            } catch (e: Exception) {
                android.util.Log.e("EntrenamientoAdapter", "Error guardando estado: ${e.message}")
            }
        }
    }
}