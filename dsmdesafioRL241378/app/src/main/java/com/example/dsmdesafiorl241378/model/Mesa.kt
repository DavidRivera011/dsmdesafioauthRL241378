package com.example.dsmdesafiorl241378.model

data class Mesa(
    val mesa_id: String = "",
    val mesero: String = "",
    val mesero_uid: String = "",   // extra: UID del mesero, útil para consultar sus mesas
    val estado: String = "libre",  // libre | ocupada
    val comensales: Int = 0,
    val pedido: Pedido = Pedido(),
    val cuenta_impresa: Boolean = false
)