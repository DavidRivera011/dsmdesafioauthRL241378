package com.example.dsmdesafiorl241378

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmdesafiorl241378.model.Mesa

class MesaResumenAdapter(
    private val mesas: MutableList<Mesa>,
    private val onClick: (Mesa) -> Unit
) : RecyclerView.Adapter<MesaResumenAdapter.MesaViewHolder>() {

    class MesaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvResumen: TextView = view.findViewById(R.id.tvResumenMesa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mesa_resumen, parent, false)
        return MesaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        val mesa = mesas[position]
        holder.tvResumen.text = "Mesa: ${mesa.mesa_id}\n" +
                "Mesero: ${mesa.mesero} · Comensales: ${mesa.comensales}\n" +
                "Estado: ${mesa.estado} · Total: $${"%.2f".format(mesa.pedido.total_parcial)}\n" +
                "Cuenta impresa: ${if (mesa.cuenta_impresa) "Sí" else "No"}"

        holder.itemView.setOnClickListener { onClick(mesa) }
    }

    override fun getItemCount(): Int = mesas.size

    fun actualizar(nuevasMesas: List<Mesa>) {
        mesas.clear()
        mesas.addAll(nuevasMesas)
        notifyDataSetChanged()
    }
}