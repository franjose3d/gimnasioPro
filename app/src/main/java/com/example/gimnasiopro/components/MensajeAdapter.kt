package com.example.gimnasiopro.components

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.firestore.Mensaje
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para mostrar mensajes en una conversación.
 */
class MensajeAdapter(
    private val currentUserId: String
) : ListAdapter<Mensaje, MensajeAdapter.MensajeViewHolder>(MensajeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mensaje, parent, false)
        return MensajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId)
    }

    class MensajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerMensaje: LinearLayout = itemView.findViewById(R.id.containerMensaje)
        private val tvTexto: TextView = itemView.findViewById(R.id.tvTexto)
        private val tvHora: TextView = itemView.findViewById(R.id.tvHora)

        private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(mensaje: Mensaje, currentUserId: String) {
            val esMio = mensaje.remitenteId == currentUserId

            tvTexto.text = mensaje.texto
            tvHora.text = dateFormat.format(mensaje.fechaEnvio)

            // Ajustar alineación y estilo según si es mensaje propio o recibido
            val params = containerMensaje.layoutParams as LinearLayout.LayoutParams

            if (esMio) {
                params.gravity = Gravity.END
                containerMensaje.setBackgroundResource(R.drawable.bg_mensaje_enviado)
            } else {
                params.gravity = Gravity.START
                containerMensaje.setBackgroundResource(R.drawable.bg_mensaje_recibido)
            }

            containerMensaje.layoutParams = params
        }
    }

    class MensajeDiffCallback : DiffUtil.ItemCallback<Mensaje>() {
        override fun areItemsTheSame(oldItem: Mensaje, newItem: Mensaje): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Mensaje, newItem: Mensaje): Boolean {
            return oldItem == newItem
        }
    }
}

