package com.example.dsmdesafiorl241378

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmdesafiorl241378.model.Articulo

class ArticuloAdapter(
    private val articulos: MutableList<Articulo>,
    private val onCambiarEstado: (Int) -> Unit
) : RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder>() {

    class ArticuloViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.tvArticuloInfo)
        val btnEstado: Button = view.findViewById(R.id.btnCambiarEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticuloViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_articulo, parent, false)
        return ArticuloViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticuloViewHolder, position: Int) {
        val articulo = articulos[position]
        val subtotal = articulo.cantidad * articulo.precio_unitario

        holder.tvInfo.text = "${articulo.platillo} x${articulo.cantidad} — $${"%.2f".format(subtotal)}\n" +
                "Notas: ${articulo.notas.ifBlank { "-" }} · Estado: ${articulo.estado}"

        holder.btnEstado.setOnClickListener {
            onCambiarEstado(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = articulos.size
}