package com.example.gimnasiopro.components

import android.app.AlertDialog
import android.graphics.Paint
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
        private const val MIN_SERIES = 3
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

        // Series 1-3 (siempre visibles)
        private val containerSerie1: LinearLayout = itemView.findViewById(R.id.containerSerie1)
        private val containerSerie2: LinearLayout = itemView.findViewById(R.id.containerSerie2)
        private val containerSerie3: LinearLayout = itemView.findViewById(R.id.containerSerie3)

        // Series 4-6 (opcionales)
        private val containerSeriesExtra: LinearLayout = itemView.findViewById(R.id.containerSeriesExtra)
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

        // Botones incrementar/decrementar
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

        // Botones agregar/quitar serie
        private val btnAgregarSerie: ImageButton = itemView.findViewById(R.id.btnAgregarSerie)
        private val btnQuitarSerie: ImageButton = itemView.findViewById(R.id.btnQuitarSerie)

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

            // Actualizar visibilidad de series
            actualizarVisibilidadSeries(ejercicioEntrenamiento)

            // Configurar valores de peso para cada serie
            actualizarPesosSeries(ejercicioEntrenamiento)

            // Configurar botones de cada serie
            configurarBotonesSerie(0, ejercicioEntrenamiento, etPeso1, btnIncrementarKg1, btnDecrementarKg1)
            configurarBotonesSerie(1, ejercicioEntrenamiento, etPeso2, btnIncrementarKg2, btnDecrementarKg2)
            configurarBotonesSerie(2, ejercicioEntrenamiento, etPeso3, btnIncrementarKg3, btnDecrementarKg3)
            configurarBotonesSerie(3, ejercicioEntrenamiento, etPeso4, btnIncrementarKg4, btnDecrementarKg4)
            configurarBotonesSerie(4, ejercicioEntrenamiento, etPeso5, btnIncrementarKg5, btnDecrementarKg5)
            configurarBotonesSerie(5, ejercicioEntrenamiento, etPeso6, btnIncrementarKg6, btnDecrementarKg6)

            // Botón agregar serie
            btnAgregarSerie.setOnClickListener {
                if (ejercicioEntrenamiento.seriesVisibles < MAX_SERIES) {
                    ejercicioEntrenamiento.seriesVisibles++
                    actualizarVisibilidadSeries(ejercicioEntrenamiento)
                }
            }

            // Botón quitar serie
            btnQuitarSerie.setOnClickListener {
                if (ejercicioEntrenamiento.seriesVisibles > MIN_SERIES) {
                    ejercicioEntrenamiento.seriesVisibles--
                    actualizarVisibilidadSeries(ejercicioEntrenamiento)
                }
            }
        }

        private fun configurarBotonesSerie(
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

        private fun actualizarPesosSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val series = ejercicioEntrenamiento.series
            if (series.size > 0) actualizarPesoEditText(etPeso1, series[0].pesoKg)
            if (series.size > 1) actualizarPesoEditText(etPeso2, series[1].pesoKg)
            if (series.size > 2) actualizarPesoEditText(etPeso3, series[2].pesoKg)
            if (series.size > 3) actualizarPesoEditText(etPeso4, series[3].pesoKg)
            if (series.size > 4) actualizarPesoEditText(etPeso5, series[4].pesoKg)
            if (series.size > 5) actualizarPesoEditText(etPeso6, series[5].pesoKg)
        }

        private fun actualizarPesoEditText(editText: EditText, peso: Float) {
            editText.setText(if (peso % 1 == 0f) peso.toInt().toString() else String.format(Locale.getDefault(), "%.1f", peso))
        }

        private fun actualizarVisibilidadSeries(ejercicioEntrenamiento: EjercicioEntrenamiento) {
            val visibles = ejercicioEntrenamiento.seriesVisibles

            // Series 4, 5, 6 están en containerSeriesExtra
            if (visibles > 3) {
                containerSeriesExtra.visibility = View.VISIBLE
                containerSerie4.visibility = if (visibles >= 4) View.VISIBLE else View.GONE
                containerSerie5.visibility = if (visibles >= 5) View.VISIBLE else View.GONE
                containerSerie6.visibility = if (visibles >= 6) View.VISIBLE else View.GONE
            } else {
                containerSeriesExtra.visibility = View.GONE
            }

            // Actualizar estado de botones
            btnAgregarSerie.isEnabled = visibles < MAX_SERIES
            btnAgregarSerie.alpha = if (visibles < MAX_SERIES) 1f else 0.5f
            btnQuitarSerie.isEnabled = visibles > MIN_SERIES
            btnQuitarSerie.alpha = if (visibles > MIN_SERIES) 1f else 0.5f
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
