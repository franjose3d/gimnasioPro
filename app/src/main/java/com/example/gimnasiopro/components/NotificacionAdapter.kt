package com.example.gimnasiopro.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.firestore.Notificacion
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Adapter para mostrar notificaciones en RecyclerView.
 */
class NotificacionAdapter(
    private val onNotificacionClick: (Notificacion) -> Unit,
    private val onAceptarClick: (Notificacion) -> Unit,
    private val onRechazarClick: (Notificacion) -> Unit,
    private val onEliminarClick: ((Notificacion) -> Unit)? = null
) : ListAdapter<Notificacion, NotificacionAdapter.NotificacionViewHolder>(NotificacionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivTipoNotificacion: ImageView = itemView.findViewById(R.id.ivTipoNotificacion)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvRemitente: TextView = itemView.findViewById(R.id.tvRemitente)
        private val tvMensaje: TextView = itemView.findViewById(R.id.tvMensaje)
        private val tvTiempo: TextView = itemView.findViewById(R.id.tvTiempo)
        private val viewNoLeida: View = itemView.findViewById(R.id.viewNoLeida)
        private val layoutAcciones: LinearLayout = itemView.findViewById(R.id.layoutAcciones)
        private val btnAceptar: Button = itemView.findViewById(R.id.btnAceptar)
        private val btnRechazar: Button = itemView.findViewById(R.id.btnRechazar)
        private val tvExpiracion: TextView = itemView.findViewById(R.id.tvExpiracion)
        private val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)

        fun bind(notificacion: Notificacion) {
            tvTitulo.text = notificacion.titulo
            tvRemitente.text = "De: ${notificacion.remitenteNombre}"
            tvMensaje.text = notificacion.mensaje
            tvTiempo.text = formatTiempoRelativo(notificacion.fechaCreacion)

            // Indicador de no leída
            viewNoLeida.visibility = if (notificacion.leida) View.GONE else View.VISIBLE

            // Icono según tipo
            val iconRes = when (notificacion.tipo) {
                Notificacion.TIPO_SOLICITUD_CONEXION -> R.drawable.ic_user_placeholder
                Notificacion.TIPO_INVITACION_TRAINER -> R.drawable.ic_rutinas
                Notificacion.TIPO_MENSAJE -> R.drawable.ic_notification
                Notificacion.TIPO_RUTINA_ACTUALIZADA -> R.drawable.ic_ejercicios
                Notificacion.TIPO_CONEXION_ACEPTADA -> R.drawable.ic_check
                Notificacion.TIPO_CONEXION_RECHAZADA -> R.drawable.ic_close
                else -> R.drawable.ic_notification
            }
            ivTipoNotificacion.setImageResource(iconRes)

            // Mostrar botones de acción para solicitudes/invitaciones NO procesadas
            val esSolicitudOInvitacion = (
                notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER
            )
            val mostrarAcciones = esSolicitudOInvitacion && !notificacion.procesada
            layoutAcciones.visibility = if (mostrarAcciones) View.VISIBLE else View.GONE

            // Mostrar expiración para mensajes
            if (notificacion.tipo == Notificacion.TIPO_MENSAJE && notificacion.fechaExpiracion != null) {
                val diasRestantes = calcularDiasRestantes(notificacion.fechaExpiracion)
                tvExpiracion.text = "Expira en $diasRestantes día${if (diasRestantes != 1L) "s" else ""}"
                tvExpiracion.visibility = View.VISIBLE
            } else {
                tvExpiracion.visibility = View.GONE
            }

            // Click listeners
            itemView.setOnClickListener { onNotificacionClick(notificacion) }
            btnAceptar.setOnClickListener { onAceptarClick(notificacion) }
            btnRechazar.setOnClickListener { onRechazarClick(notificacion) }

            // Botón eliminar (siempre visible en el layout)
            btnEliminar.setOnClickListener { onEliminarClick?.invoke(notificacion) }

            // Long press para eliminar cualquier notificación
            itemView.setOnLongClickListener {
                onEliminarClick?.invoke(notificacion)
                true
            }
        }

        private fun formatTiempoRelativo(fecha: Date): String {
            val ahora = Date()
            val diffMs = ahora.time - fecha.time

            val minutos = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val horas = TimeUnit.MILLISECONDS.toHours(diffMs)
            val dias = TimeUnit.MILLISECONDS.toDays(diffMs)

            return when {
                minutos < 1 -> "Ahora"
                minutos < 60 -> "Hace ${minutos}m"
                horas < 24 -> "Hace ${horas}h"
                dias < 7 -> "Hace ${dias}d"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                    sdf.format(fecha)
                }
            }
        }

        private fun calcularDiasRestantes(fechaExpiracion: Date): Long {
            val diffMs = fechaExpiracion.time - Date().time
            return TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(0)
        }
    }

    class NotificacionDiffCallback : DiffUtil.ItemCallback<Notificacion>() {
        override fun areItemsTheSame(oldItem: Notificacion, newItem: Notificacion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notificacion, newItem: Notificacion): Boolean {
            return oldItem == newItem
        }
    }
}

