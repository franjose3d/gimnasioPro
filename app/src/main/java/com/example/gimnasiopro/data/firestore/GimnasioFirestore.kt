package com.example.gimnasiopro.data.firestore

data class GimnasioFirestore(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val mapsQuery: String = "",
    val logoUrl: String = "",
    val webUrl: String = "",
    val activo: Boolean = true
)
