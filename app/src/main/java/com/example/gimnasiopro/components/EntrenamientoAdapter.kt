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
        // Evita que el RecyclerView recicle y pierda el estado
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Usar el ID único del ejercicio para identificar cada item
        return ejercicios[position].ejercicio.id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntrenamientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio_entrenamiento, parent, false)
        return EntrenamientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntrenamientoViewHolder, position: Int) {
        // IMPORTANTE: Antes de bind, guardar el estado actual si existe
        holder.saveCurrentState()
        holder.bind(ejercicios[position], position)
    }

    // IMPORTANTE: Guardar el estado antes de reciclar
    override fun onViewRecycled(holder: EntrenamientoViewHolder) {
        // Guardar el estado actual ANTES de reciclar
        holder.saveCurrentState()
        holder.cleanup()
        super.onViewRecycled(holder)
    }

    // Indicar que no queremos reciclar items - cada posición tiene viewType único
    override fun getItemViewType(position: Int): Int {
        // Usar el ID del ejercicio para evitar reciclaje entre diferentes ejercicios
        return ejercicios.getOrNull(position)?.ejercicio?.id?.toInt() ?: position
    }

    override fun getItemCount(): Int = ejercicios.size

    fun getEjercicios(): List<EjercicioEntrenamiento> = ejercicios.toList()

    /**
     * Obtiene el ejercicio actual de la posición - usado internamente para
     * asegurar que siempre se modifica el ejercicio correcto
     */
    private fun getEjercicioAt(position: Int): EjercicioEntrenamiento? {
        return if (position in 0 until ejercicios.size) ejercicios[position] else null
    }

    inner class EntrenamientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreEjercicio: TextView = itemView.findViewById(R.id.tvNombreEjercicio)

        // Referencia al ejercicio actual y posición - se actualiza en cada bind()
        private var currentEjercicio: EjercicioEntrenamiento? = null
        private var currentPosition: Int = -1
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

        fun bind(ejercicioEntrenamiento: EjercicioEntrenamiento, position: Int) {
            // Guardar referencia al ejercicio actual y posición
            currentEjercicio = ejercicioEntrenamiento
            currentPosition = position

            tvNombreEjercicio.text = ejercicioEntrenamiento.ejercicio.nombre

            // IMPORTANTE: Limpiar listener antes de cambiar el estado para evitar
            // que se dispare accidentalmente durante el reciclaje/rebind
            checkboxCompletado.setOnCheckedChangeListener(null)

            // Configurar estado del checkbox
            checkboxCompletado.isChecked = ejercicioEntrenamiento.completado
            actualizarEstiloCompletado(ejercicioEntrenamiento.completado)

            // Listener del checkbox - se configura DESPUÉS de setear el valor
            // Usa currentEjercicio para asegurar que modifica el ejercicio correcto
            checkboxCompletado.setOnCheckedChangeListener { _, isChecked ->
                currentEjercicio?.let { ejercicio ->
                    ejercicio.completado = isChecked
                    actualizarEstiloCompletado(isChecked)
                    onEjercicioCompletado(bindingAdapterPosition, isChecked)
                }
            }

            // Botón para ver video en YouTube
            btnVerVideo.setOnClickListener {
                currentEjercicio?.let { abrirVideoYouTube(it.ejercicio.nombre) }
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
                // Usar currentEjercicio para asegurar que modificamos el ejercicio correcto
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                if (indice < ejercicio.series.size) {
                    ejercicio.series[indice].pesoKg += KG_INCREMENT
                    actualizarPesoEditText(etPeso, ejercicio.series[indice].pesoKg)
                }
            }

            btnDecrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                if (indice < ejercicio.series.size) {
                    val serie = ejercicio.series[indice]
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
                currentEjercicio?.let { ejercicio ->
                    mostrarDialogoPeso(ejercicio, indice, etPeso)
                }
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
                if (indice < ejercicio.series.size) {
                    ejercicio.series[indice].repeticiones += REP_INCREMENT
                    actualizarRepEditText(etRep, ejercicio.series[indice].repeticiones)
                }
            }

            btnDecrementar.setOnClickListener {
                val ejercicio = currentEjercicio ?: return@setOnClickListener
                if (indice < ejercicio.series.size) {
                    val serie = ejercicio.series[indice]
                    if (serie.repeticiones > MIN_REP) {
                        serie.repeticiones -= REP_INCREMENT
                    }
                    actualizarRepEditText(etRep, serie.repeticiones)
                }
            }

            // Click para edición manual de repeticiones
            etRep.setOnClickListener {
                currentEjercicio?.let { ejercicio ->
                    mostrarDialogoRepeticiones(ejercicio, indice, etRep)
                }
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

        /**
         * Limpia todos los listeners para evitar memory leaks al reciclar la vista
         */
        fun cleanup() {
            checkboxCompletado.setOnCheckedChangeListener(null)
            btnVerVideo.setOnClickListener(null)
            // No es necesario limpiar los botones de kg/rep porque se reconfiguran en bind()
        }

        /**
         * IMPORTANTE: Guarda el estado actual de los EditTexts al modelo de datos.
         * Esto se llama antes de reciclar o rebind para asegurar que no se pierdan datos.
         */
        fun saveCurrentState() {
            val ejercicio = currentEjercicio ?: return
            if (currentPosition < 0) return

            try {
                // Guardar pesos de cada serie desde los EditTexts
                ejercicio.series.getOrNull(0)?.pesoKg = etPeso1.text.toString().toFloatOrNull() ?: ejercicio.series[0].pesoKg
                ejercicio.series.getOrNull(1)?.pesoKg = etPeso2.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(1) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(2)?.pesoKg = etPeso3.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(2) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(3)?.pesoKg = etPeso4.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(3) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(4)?.pesoKg = etPeso5.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(4) { SerieEntrenamiento() }.pesoKg
                ejercicio.series.getOrNull(5)?.pesoKg = etPeso6.text.toString().toFloatOrNull() ?: ejercicio.series.getOrElse(5) { SerieEntrenamiento() }.pesoKg

                // Guardar repeticiones de cada serie
                ejercicio.series.getOrNull(0)?.repeticiones = etRep1.text.toString().toIntOrNull() ?: ejercicio.series[0].repeticiones
                ejercicio.series.getOrNull(1)?.repeticiones = etRep2.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(1) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(2)?.repeticiones = etRep3.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(2) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(3)?.repeticiones = etRep4.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(3) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(4)?.repeticiones = etRep5.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(4) { SerieEntrenamiento() }.repeticiones
                ejercicio.series.getOrNull(5)?.repeticiones = etRep6.text.toString().toIntOrNull() ?: ejercicio.series.getOrElse(5) { SerieEntrenamiento() }.repeticiones

                // Guardar estado del checkbox
                ejercicio.completado = checkboxCompletado.isChecked
            } catch (e: Exception) {
                // En caso de error, no hacer nada para no corromper los datos
                android.util.Log.e("EntrenamientoAdapter", "Error guardando estado: ${e.message}")
            }
        }
    }
}
