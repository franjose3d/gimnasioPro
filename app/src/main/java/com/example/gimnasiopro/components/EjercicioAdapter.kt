package com.example.gimnasiopro.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.Ejercicio

/**
 * Adaptador para mostrar la lista de ejercicios con soporte para selección múltiple.
 */
class EjercicioAdapter(
    private val onEjercicioClick: (Ejercicio) -> Unit,
    private val onSelectionChanged: (Set<Ejercicio>) -> Unit,
    private val maxSeleccion: Int = 10
) : ListAdapter<Ejercicio, EjercicioAdapter.EjercicioViewHolder>(EjercicioDiffCallback()) {

    private val selectedEjercicios = mutableSetOf<Ejercicio>()

    // La selección siempre está activa
    var selectionMode = true
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio, parent, false)
        return EjercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: EjercicioViewHolder, position: Int) {
        val ejercicio = getItem(position)
        holder.bind(ejercicio)
    }

    fun getSelectedEjercicios(): Set<Ejercicio> = selectedEjercicios.toSet()

    fun getSelectedCount(): Int = selectedEjercicios.size

    fun clearSelection() {
        selectedEjercicios.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedEjercicios)
    }

    private fun toggleSelection(ejercicio: Ejercicio): Boolean {
        if (selectedEjercicios.contains(ejercicio)) {
            selectedEjercicios.remove(ejercicio)
            onSelectionChanged(selectedEjercicios)
            return true
        } else {
            if (selectedEjercicios.size < maxSeleccion) {
                selectedEjercicios.add(ejercicio)
                onSelectionChanged(selectedEjercicios)
                return true
            }
            return false // No se pudo agregar porque se alcanzó el máximo
        }
    }

    fun isMaxReached(): Boolean = selectedEjercicios.size >= maxSeleccion

    inner class EjercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreEjercicio)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionEjercicio)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkboxEjercicio)

        fun bind(ejercicio: Ejercicio) {
            tvNombre.text = ejercicio.nombre
            tvDescripcion.text = ejercicio.descripcion

            // Checkbox siempre visible
            checkBox.visibility = View.VISIBLE
            checkBox.isChecked = selectedEjercicios.contains(ejercicio)

            // Configurar click listener en toda la tarjeta
            itemView.setOnClickListener {
                val wasToggled = toggleSelection(ejercicio)
                if (wasToggled) {
                    checkBox.isChecked = selectedEjercicios.contains(ejercicio)
                }
            }

            // Click en el checkbox
            checkBox.setOnClickListener {
                val wasToggled = toggleSelection(ejercicio)
                // Si no se pudo agregar (máximo alcanzado), revertir el estado del checkbox
                if (!wasToggled) {
                    checkBox.isChecked = selectedEjercicios.contains(ejercicio)
                }
            }
        }
    }

    private class EjercicioDiffCallback : DiffUtil.ItemCallback<Ejercicio>() {
        override fun areItemsTheSame(oldItem: Ejercicio, newItem: Ejercicio): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ejercicio, newItem: Ejercicio): Boolean {
            return oldItem == newItem
        }
    }
}

