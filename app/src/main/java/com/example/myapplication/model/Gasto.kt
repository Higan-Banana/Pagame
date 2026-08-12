package com.example.myapplication.model

data class Gasto(
    val id: Int,
    val nombre: String,
    val pagador: String,
    val monto: Int,
    val hora: String,
    val imageUrl: String
)