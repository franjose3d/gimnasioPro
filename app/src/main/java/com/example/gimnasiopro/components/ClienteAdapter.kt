package com.example.gimnasiopro.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.ListaClientesActivity
import com.example.gimnasiopro.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para mostrar lista de clientes conectados a un trainer.
 */
class ClienteAdapter(
    private val clientes: List<ListaClientesActivity.ClienteConConexion>,
    private val onClienteClick: (ListaClientesActivity.ClienteConConexion) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class ClienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.containerCliente)
        val ivPhoto: ImageView = view.findViewById(R.id.ivClientePhoto)
        val tvNombre: TextView = view.findViewById(R.id.tvClienteNombre)
        val tvEmail: TextView = view.findViewById(R.id.tvClienteEmail)
        val tvFechaConexion: TextView = view.findViewById(R.id.tvFechaConexion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val clienteConConexion = clientes[position]
        val cliente = clienteConConexion.cliente
        val conexion = clienteConConexion.conexion

        holder.tvNombre.text = cliente.nombre ?: "Cliente"
        holder.tvEmail.text = cliente.email ?: ""

        // Fecha de conexión
        val fechaConexion = conexion.fechaRespuesta ?: conexion.fechaSolicitud
        if (fechaConexion != null) {
            holder.tvFechaConexion.text = "Conectado desde: ${dateFormat.format(fechaConexion)}"
            holder.tvFechaConexion.visibility = View.VISIBLE
        } else {
            holder.tvFechaConexion.visibility = View.GONE
        }

        holder.container.setOnClickListener {
            onClienteClick(clienteConConexion)
        }
    }

    override fun getItemCount(): Int = clientes.size
}

