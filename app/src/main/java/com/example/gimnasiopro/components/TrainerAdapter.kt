package com.example.gimnasiopro.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.firestore.UserHelper

/**
 * Adapter para mostrar lista de trainers disponibles.
 */
class TrainerAdapter(
    private val trainers: List<UserHelper.UserInfo>,
    private val onTrainerClick: (UserHelper.UserInfo) -> Unit
) : RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder>() {

    class TrainerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.containerTrainer)
        val ivPhoto: ImageView = view.findViewById(R.id.ivTrainerPhoto)
        val tvNombre: TextView = view.findViewById(R.id.tvTrainerNombre)
        val tvEmail: TextView = view.findViewById(R.id.tvTrainerEmail)
        val tvCertificado: TextView = view.findViewById(R.id.tvTrainerCertificado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trainer, parent, false)
        return TrainerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        val trainer = trainers[position]

        holder.tvNombre.text = trainer.nombre ?: "Trainer"
        holder.tvEmail.text = trainer.email ?: ""

        // Obtener certificado del documento
        val certificado = trainer.documento?.getString("certificado")
        if (!certificado.isNullOrBlank()) {
            holder.tvCertificado.text = "🏅 $certificado"
            holder.tvCertificado.visibility = View.VISIBLE
        } else {
            holder.tvCertificado.visibility = View.GONE
        }

        holder.container.setOnClickListener {
            onTrainerClick(trainer)
        }
    }

    override fun getItemCount(): Int = trainers.size
}

