package com.example.dsmdesafiorl241378

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dsmdesafiorl241378.databinding.ActivityMesaBinding
import com.example.dsmdesafiorl241378.model.Articulo
import com.example.dsmdesafiorl241378.model.Mesa
import com.example.dsmdesafiorl241378.model.Pedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MesaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMesaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: com.google.firebase.database.DatabaseReference

    private val articulos = mutableListOf<Articulo>()
    private lateinit var adapter: ArticuloAdapter

    private var currentMesaId: String = ""
    private var currentComensales: Int = 0
    private var currentEstadoMesa: String = "libre"
    private var currentHoraApertura: String = ""
    private var currentCuentaImpresa: Boolean = false
    private var editando: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.tvMesero.text = "Mesero: ${obtenerNombreMesero()}"

        val opcionesEstado = arrayOf("en_progreso", "servido")
        binding.spEstado.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, opcionesEstado
        )

        adapter = ArticuloAdapter(articulos) { position -> cambiarEstadoArticulo(position) }
        binding.rvArticulos.layoutManager = LinearLayoutManager(this)
        binding.rvArticulos.adapter = adapter
        binding.rvArticulos.isNestedScrollingEnabled = false

        binding.btnAbrirMesa.setOnClickListener { abrirMesa() }
        binding.btnAgregarArticulo.setOnClickListener { agregarArticulo() }
        binding.btnGuardarMesa.setOnClickListener { guardarMesa() }

        binding.swCuenta.setOnCheckedChangeListener { _, isChecked ->
            currentCuentaImpresa = isChecked
        }

        val mesaIdExtra = intent.getStringExtra("mesa_id")
        if (!mesaIdExtra.isNullOrBlank()) {
            editando = true
            cargarMesaExistente(mesaIdExtra)
        }
    }
    private fun obtenerNombreMesero(): String {
        val user = auth.currentUser
        return user?.displayName
            ?: user?.providerData?.firstOrNull {
                it.providerId != "firebase" && !it.displayName.isNullOrBlank()
            }?.displayName
            ?: user?.email
            ?: "Mesero"
    }

    private fun obtenerFechaActualIso(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        return formato.format(Date())
    }

    private fun abrirMesa() {
        val mesaId = binding.etMesaId.text.toString().trim()
        val comensalesTexto = binding.etComensales.text.toString().trim()

        if (mesaId.isEmpty() || comensalesTexto.isEmpty()) {
            Toast.makeText(this, "Completa el ID de la mesa y los comensales", Toast.LENGTH_SHORT).show()
            return
        }

        val comensales = comensalesTexto.toIntOrNull()
        if (comensales == null || comensales <= 0) {
            Toast.makeText(this, "Comensales debe ser un número mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }
        currentMesaId = mesaId
        currentComensales = comensales
        currentEstadoMesa = "ocupada"
        currentCuentaImpresa = false
        currentHoraApertura = obtenerFechaActualIso()
        articulos.clear()
        adapter.notifyDataSetChanged()
        actualizarTotal()

        binding.etMesaId.isEnabled = false
        binding.etComensales.isEnabled = false
        binding.btnAbrirMesa.isEnabled = false

        Toast.makeText(this, "Mesa abierta. Ya puedes agregar artículos.", Toast.LENGTH_SHORT).show()
    }

    private fun agregarArticulo() {
        if (currentMesaId.isBlank()) {
            Toast.makeText(this, "Primero abre una mesa", Toast.LENGTH_SHORT).show()
            return
        }

        val platillo = binding.etPlatillo.text.toString().trim()
        val cantidadTexto = binding.etCantidad.text.toString().trim()
        val precioTexto = binding.etPrecio.text.toString().trim()
        val notas = binding.etNotas.text.toString().trim()
        val estado = binding.spEstado.selectedItem.toString()

        if (platillo.isEmpty() || cantidadTexto.isEmpty() || precioTexto.isEmpty()) {
            Toast.makeText(this, "Completa platillo, cantidad y precio", Toast.LENGTH_SHORT).show()
            return
        }

        val cantidad = cantidadTexto.toIntOrNull()
        val precio = precioTexto.toDoubleOrNull()

        if (cantidad == null || cantidad <= 0) {
            Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show()
            return
        }
        if (precio == null || precio < 0) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show()
            return
        }

        articulos.add(Articulo(platillo, cantidad, notas, precio, estado))
        adapter.notifyItemInserted(articulos.size - 1)
        actualizarTotal()

        binding.etPlatillo.text.clear()
        binding.etCantidad.text.clear()
        binding.etPrecio.text.clear()
        binding.etNotas.text.clear()
    }

    private fun cambiarEstadoArticulo(position: Int) {
        val articulo = articulos[position]
        val nuevoEstado = if (articulo.estado == "en_progreso") "servido" else "en_progreso"
        articulos[position] = articulo.copy(estado = nuevoEstado)
        adapter.notifyItemChanged(position)
    }

    private fun actualizarTotal() {
        val total = articulos.sumOf { it.cantidad * it.precio_unitario }
        binding.tvTotal.text = "Total parcial: $${"%.2f".format(total)}"
    }

    private fun guardarMesa() {
        if (currentMesaId.isBlank()) {
            Toast.makeText(this, "Primero abre una mesa", Toast.LENGTH_SHORT).show()
            return
        }

        val total = articulos.sumOf { it.cantidad * it.precio_unitario }
        val pedido = Pedido(
            hora_apertura = currentHoraApertura,
            articulos = articulos.toMutableList(),
            total_parcial = total
        )
        val mesa = Mesa(
            mesa_id = currentMesaId,
            mesero = obtenerNombreMesero(),
            mesero_uid = auth.currentUser?.uid ?: "",
            estado = currentEstadoMesa,
            comensales = currentComensales,
            pedido = pedido,
            cuenta_impresa = currentCuentaImpresa
        )
        database.child("mesas").child(currentMesaId).setValue(mesa)
            .addOnSuccessListener {
                Toast.makeText(this, "Mesa guardada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarMesaExistente(mesaId: String) {
        database.child("mesas").child(mesaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mesa = snapshot.getValue(Mesa::class.java)
                    if (mesa == null) {
                        Toast.makeText(this@MesaActivity, "No se encontró la mesa", Toast.LENGTH_SHORT).show()
                        return
                    }

                    currentMesaId = mesa.mesa_id
                    currentComensales = mesa.comensales
                    currentEstadoMesa = mesa.estado
                    currentCuentaImpresa = mesa.cuenta_impresa
                    currentHoraApertura = mesa.pedido.hora_apertura

                    articulos.clear()
                    articulos.addAll(mesa.pedido.articulos)
                    adapter.notifyDataSetChanged()
                    actualizarTotal()

                    binding.etMesaId.setText(mesa.mesa_id)
                    binding.etMesaId.isEnabled = false
                    binding.etComensales.setText(mesa.comensales.toString())
                    binding.etComensales.isEnabled = false
                    binding.btnAbrirMesa.isEnabled = false
                    binding.swCuenta.isChecked = mesa.cuenta_impresa
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MesaActivity, "Error al cargar: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}