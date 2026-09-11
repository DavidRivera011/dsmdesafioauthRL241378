package com.example.dsmdesafiorl241378.model

data class Pedido(
    val hora_apertura: String = "",
    val articulos: MutableList<Articulo> = mutableListOf(),
    val total_parcial: Double = 0.0
)