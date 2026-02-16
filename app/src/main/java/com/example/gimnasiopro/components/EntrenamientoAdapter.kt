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
import java.net.URLEncoder
import java.util.Locale

/**
 * Adapter para mostrar la lista de ejercicios durante el entrenamiento.
 * Permite modificar el peso de cada serie de cada ejercicio.
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntrenamientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio_entrenamiento, parent, false)
        return EntrenamientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntrenamientoViewHolder, position: Int) {
        holder.bind(ejercicios[position])
    }

    override fun getItemCount(): Int = ejercicios.size

    fun getEjercicios(): List<EjercicioEntrenamiento> = ejercicios.toList()

    inner class EntrenamientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreEjercicio: TextView = itemView.findViewById(R.id.tvNombreEjercicio)
        private val checkboxCompletado: CheckBox = itemView.findViewById(R.id.checkboxCompletado)
        private val btnVerVideo: ImageButton = itemView.findViewById(R.id.btnVerVideo)

        // Fila 1: Series 1 y 2 (siempre visibles)
        private val containerSerie1: LinearLayout = itemView.findViewById(R.id.containerSerie1)
        private val containerSerie2: LinearLayout = itemView.findViewById(R.id.containerSerie2)

        // Fila 2: Series 3 y 4 (ocultas inicialmente)
        private val containerSeriesFila2: LinearLayout = itemView.findViewById(R.id.containerSeriesFila2)
        private val containerSerie3: LinearLayout = itemView.findViewById(R.id.containerSerie3)
        private val containerSerie4: LinearLayout = itemView.findViewById(R.id.containerSerie4)

        // Fila 3: Series 5 y 6 (ocultas inicialmente)
        private val containerSeriesFila3: LinearLayout = itemView.findViewById(R.id.containerSeriesFila3)
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

        // Botones incrementar/decrementar Kg
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

        // Botones incrementar/decrementar Rep
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

        // Botones agregar/quitar serie integrados en cada serie
        private val btnAgregarSerie1: ImageButton = itemView.findViewById(R.id.btnAgregarSerie1)
        private val btnQuitarSerie1: ImageButton = itemView.findViewById(R.id.btnQuitarSerie1)
        private val btnAgregarSerie2: ImageButton = itemView.findViewById(R.id.btnAgregarSerie2)
        private val btnQuitarSerie2: ImageButton = itemView.findViewById(R.id.btnQuitarSerie2)
        private val btnAgregarSerie3: ImageButton = itemView.findViewById(R.id.btnAgregarSerie3)
        private val btnQuitarSerie3: ImageButton = itemView.findViewById(R.id.btnQuitarSerie3)
        private val btnAgregarSerie4: ImageButton = itemView.findViewById(R.id.btnAgregarSerie4)
        private val btnQuitarSerie4: ImageButton = itemView.findViewById(R.id.btnQuitarSerie4)
        private val btnAgregarSerie5: ImageButton = itemView.findViewById(R.id.btnAgregarSerie5)
        private val btnQuitarSerie5: ImageButton = itemView.findViewById(R.id.btnQuitarSerie5)
        private val btnAgregarSerie6: ImageButton = itemView.findViewById(R.id.btnAgregarSerie6)
        private val btnQuitarSerie6: ImageButton = itemView.findViewById(R.id.btnQuitarSerie6)

        fun bind(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            tvNombreEjercicio.text = ejercicioEntrenamiento.ejercicio.nombre

            // Configurar estado del checkbox
            checkboxCompletado.isChecked = ejercicioEntrenamiento.completado
            actualizarEstiloCompletado(ejercicioEntrenamiento.completado)

            // Listener del checkbox
            checkboxCompletado.setOnCheckedChangeListener { _, isChecked ->
                ejercicioEntrenamiento.completado = isChecked
                actualizarEstiloCompletado(isChecked)
                onEjercicioCompletado(adapterPosition, isChecked)
            }

            // Botón para ver video en YouTube
            btnVerVideo.setOnClickListener {
                abrirVideoYouTube(ejercicioEntrenamiento.ejercicio.nombre)
            }

            // Actualizar visibilidad de series
            actualizarVisibilidadSeries(ejercicioEntrenamiento)

            // Configurar valores de peso y repeticiones para cada serie
            actualizarPesosSeries(ejercicioEntrenamiento)
            actualizarRepeticionesSeries(ejercicioEntrenamiento)

            // Configurar botones de peso de cada serie
            configurarBotonesSeriePeso(0, ejercicioEntrenamiento, etPeso1, btnIncrementarKg1, btnDecrementarKg1)
            configurarBotonesSeriePeso(1, ejercicioEntrenamiento, etPeso2, btnIncrementarKg2, btnDecrementarKg2)
            configurarBotonesSeriePeso(2, ejercicioEntrenamiento, etPeso3, btnIncrementarKg3, btnDecrementarKg3)
            configurarBotonesSeriePeso(3, ejercicioEntrenamiento, etPeso4, btnIncrementarKg4, btnDecrementarKg4)
            configurarBotonesSeriePeso(4, ejercicioEntrenamiento, etPeso5, btnIncrementarKg5, btnDecrementarKg5)
            configurarBotonesSeriePeso(5, ejercicioEntrenamiento, etPeso6, btnIncrementarKg6, btnDecrementarKg6)

            // Configurar botones de repeticiones de cada serie
            configurarBotonesSerieRep(0, ejercicioEntrenamiento, etRep1, btnIncrementarRep1, btnDecrementarRep1)
            configurarBotonesSerieRep(1, ejercicioEntrenamiento, etRep2, btnIncrementarRep2, btnDecrementarRep2)
            configurarBotonesSerieRep(2, ejercicioEntrenamiento, etRep3, btnIncrementarRep3, btnDecrementarRep3)
            configurarBotonesSerieRep(3, ejercicioEntrenamiento, etRep4, btnIncrementarRep4, btnDecrementarRep4)
            configurarBotonesSerieRep(4, ejercicioEntrenamiento, etRep5, btnIncrementarRep5, btnDecrementarRep5)
            configurarBotonesSerieRep(5, ejercicioEntrenamiento, etRep6, btnIncrementarRep6, btnDecrementarRep6)

            // Configurar botones de agregar/quitar serie integrados
            configurarBotonesAgregarQuitarSerie(ejercicioEntrenamiento)
        }

        private fun configurarBotonesAgregarQuitarSerie(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val agregarSerieListener = View.OnClickListener {
                if (ejercicioEntrenamiento.seriesVisibles < MAX_SERIES) {
                    ejercicioEntrenamiento.seriesVisibles++
                    actualizarVisibilidadSeries(ejercicioEntrenamiento)
                }
            }

            val quitarSerieListener = View.OnClickListener {
                if (ejercicioEntrenamiento.seriesVisibles > 1) {
                    ejercicioEntrenamiento.seriesVisibles--
                    actualizarVisibilidadSeries(ejercicioEntrenamiento)
                }
            }

            // Configurar listeners para todos los botones
            btnAgregarSerie1.setOnClickListener(agregarSerieListener)
            btnAgregarSerie2.setOnClickListener(agregarSerieListener)
            btnAgregarSerie3.setOnClickListener(agregarSerieListener)
            btnAgregarSerie4.setOnClickListener(agregarSerieListener)
            btnAgregarSerie5.setOnClickListener(agregarSerieListener)
            btnAgregarSerie6.setOnClickListener(agregarSerieListener)

            btnQuitarSerie1.setOnClickListener(quitarSerieListener)
            btnQuitarSerie2.setOnClickListener(quitarSerieListener)
            btnQuitarSerie3.setOnClickListener(quitarSerieListener)
            btnQuitarSerie4.setOnClickListener(quitarSerieListener)
            btnQuitarSerie5.setOnClickListener(quitarSerieListener)
            btnQuitarSerie6.setOnClickListener(quitarSerieListener)
        }

        /**
         * Abre YouTube con una búsqueda del ejercicio
         */
        private fun abrirVideoYouTube(nombreEjercicio: String) {
            try {
                val query = URLEncoder.encode("$nombreEjercicio ejercicio gym", "UTF-8")
                val youtubeUrl = "https://www.youtube.com/results?search_query=$query"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                itemView.context.startActivity(intent)
            } catch (e: Exception) {
                // Si falla, intentar abrir solo YouTube
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
                if (indice < ejercicioEntrenamiento.series.size) {
                    ejercicioEntrenamiento.series[indice].pesoKg += KG_INCREMENT
                    actualizarPesoEditText(etPeso, ejercicioEntrenamiento.series[indice].pesoKg)
                }
            }

            btnDecrementar.setOnClickListener {
                if (indice < ejercicioEntrenamiento.series.size) {
                    val serie = ejercicioEntrenamiento.series[indice]
                    if (serie.pesoKg >= KG_INCREMENT) {
                        serie.pesoKg -= KG_INCREMENT
                    } else {
                        serie.pesoKg = MIN_KG
                    }
                    actualizarPesoEditText(etPeso, serie.pesoKg)
                }
            }

            // Click para edición manual
            etPeso.setOnClickListener {
                mostrarDialogoPeso(ejercicioEntrenamiento, indice, etPeso)
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
                if (indice < ejercicioEntrenamiento.series.size) {
                    ejercicioEntrenamiento.series[indice].repeticiones += REP_INCREMENT
                    actualizarRepEditText(etRep, ejercicioEntrenamiento.series[indice].repeticiones)
                }
            }

            btnDecrementar.setOnClickListener {
                if (indice < ejercicioEntrenamiento.series.size) {
                    val serie = ejercicioEntrenamiento.series[indice]
                    if (serie.repeticiones > MIN_REP) {
                        serie.repeticiones -= REP_INCREMENT
                    }
                    actualizarRepEditText(etRep, serie.repeticiones)
                }
            }

            // Click para edición manual de repeticiones
            etRep.setOnClickListener {
                mostrarDialogoRepeticiones(ejercicioEntrenamiento, indice, etRep)
            }
        }

        private fun actualizarPesosSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val series = ejercicioEntrenamiento.series
            if (series.size > 0) actualizarPesoEditText(etPeso1, series[0].pesoKg)
            if (series.size > 1) actualizarPesoEditText(etPeso2, series[1].pesoKg)
            if (series.size > 2) actualizarPesoEditText(etPeso3, series[2].pesoKg)
            if (series.size > 3) actualizarPesoEditText(etPeso4, series[3].pesoKg)
            if (series.size > 4) actualizarPesoEditText(etPeso5, series[4].pesoKg)
            if (series.size > 5) actualizarPesoEditText(etPeso6, series[5].pesoKg)
        }

        private fun actualizarRepeticionesSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val series = ejercicioEntrenamiento.series
            if (series.size > 0) actualizarRepEditText(etRep1, series[0].repeticiones)
            if (series.size > 1) actualizarRepEditText(etRep2, series[1].repeticiones)
            if (series.size > 2) actualizarRepEditText(etRep3, series[2].repeticiones)
            if (series.size > 3) actualizarRepEditText(etRep4, series[3].repeticiones)
            if (series.size > 4) actualizarRepEditText(etRep5, series[4].repeticiones)
            if (series.size > 5) actualizarRepEditText(etRep6, series[5].repeticiones)
        }

        private fun actualizarPesoEditText(editText: EditText, peso: Float) {
            editText.setText(if (peso % 1 == 0f) peso.toInt().toString() else String.format(Locale.getDefault(), "%.1f", peso))
        }

        private fun actualizarRepEditText(editText: EditText, repeticiones: Int) {
            editText.setText(repeticiones.toString())
        }

        private fun actualizarVisibilidadSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val visibles = ejercicioEntrenamiento.seriesVisibles

            // Series 1 y 2 (misma fila)
            containerSerie1.visibility = View.VISIBLE
            containerSerie2.visibility = if (visibles >= 2) View.VISIBLE else View.INVISIBLE

            // Fila 2: Series 3 y 4
            if (visibles >= 3) {
                containerSeriesFila2.visibility = View.VISIBLE
                containerSerie3.visibility = View.VISIBLE
                containerSerie4.visibility = if (visibles >= 4) View.VISIBLE else View.INVISIBLE
            } else {
                containerSeriesFila2.visibility = View.GONE
            }

            // Fila 3: Series 5 y 6
            if (visibles >= 5) {
                containerSeriesFila3.visibility = View.VISIBLE
                containerSerie5.visibility = View.VISIBLE
                containerSerie6.visibility = if (visibles >= 6) View.VISIBLE else View.INVISIBLE
            } else {
                containerSeriesFila3.visibility = View.GONE
            }

            // Ocultar todos los botones de agregar/quitar serie
            btnAgregarSerie1.visibility = View.GONE
            btnQuitarSerie1.visibility = View.GONE
            btnAgregarSerie2.visibility = View.GONE
            btnQuitarSerie2.visibility = View.GONE
            btnAgregarSerie3.visibility = View.GONE
            btnQuitarSerie3.visibility = View.GONE
            btnAgregarSerie4.visibility = View.GONE
            btnQuitarSerie4.visibility = View.GONE
            btnAgregarSerie5.visibility = View.GONE
            btnQuitarSerie5.visibility = View.GONE
            btnAgregarSerie6.visibility = View.GONE
            btnQuitarSerie6.visibility = View.GONE

            // Mostrar solo los botones de la última serie visible
            when (visibles) {
                1 -> {
                    btnAgregarSerie1.visibility = View.VISIBLE
                    // No mostrar quitar porque solo hay 1 serie
                }
                2 -> {
                    btnAgregarSerie2.visibility = View.VISIBLE
                    btnQuitarSerie2.visibility = View.VISIBLE
                }
                3 -> {
                    btnAgregarSerie3.visibility = View.VISIBLE
                    btnQuitarSerie3.visibility = View.VISIBLE
                }
                4 -> {
                    btnAgregarSerie4.visibility = View.VISIBLE
                    btnQuitarSerie4.visibility = View.VISIBLE
                }
                5 -> {
                    btnAgregarSerie5.visibility = View.VISIBLE
                    btnQuitarSerie5.visibility = View.VISIBLE
                }
                6 -> {
                    // No mostrar agregar porque es el máximo
                    btnQuitarSerie6.visibility = View.VISIBLE
                }
            }
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
    }
}
