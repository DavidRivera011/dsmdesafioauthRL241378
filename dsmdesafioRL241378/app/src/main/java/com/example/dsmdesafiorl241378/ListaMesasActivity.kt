package com.example.dsmdesafiorl241378

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dsmdesafiorl241378.databinding.ActivityListaMesasBinding
import com.example.dsmdesafiorl241378.model.Mesa
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaMesasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaMesasBinding
    private lateinit var adapter: MesaResumenAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaMesasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MesaResumenAdapter(mutableListOf()) { mesa ->
            val intent = Intent(this, MesaActivity::class.java)
            intent.putExtra("mesa_id", mesa.mesa_id)
            startActivity(intent)
        }

        binding.rvMesas.layoutManager = LinearLayoutManager(this)
        binding.rvMesas.adapter = adapter

        cargarMesas()
    }

    override fun onResume() {
        super.onResume()
        cargarMesas()
    }

    private fun cargarMesas() {
        FirebaseDatabase.getInstance().reference.child("mesas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listaMesas = mutableListOf<Mesa>()
                    for (mesaSnapshot in snapshot.children) {
                        mesaSnapshot.getValue(Mesa::class.java)?.let { listaMesas.add(it) }
                    }
                    adapter.actualizar(listaMesas)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ListaMesasActivity, "Error al cargar mesas: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}