package com.example.avanceproyecto.models

data class Contact(
    val connectionId: String = "",
    val userId: String = "",
    val connectedUserId: String = "",
    val connectedUserName: String = "",
    val connectedUserEmail: String = "",
    val type: String = "friend",
    val status: String = "pending"
)
