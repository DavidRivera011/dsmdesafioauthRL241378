package com.example.dsmdesafiorl241378.model

data class Articulo(
    val platillo: String = "",
    val cantidad: Int = 0,
    val notas: String = "",
    val precio_unitario: Double = 0.0,
    val estado: String = "en_progreso" // en_progreso | servido
)