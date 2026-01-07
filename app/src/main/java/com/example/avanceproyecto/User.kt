package com.example.avanceproyecto.models

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", "", "", "", "", Timestamp.now())
}

data class UserConnection(
    val connectionId: String = "",
    val userId: String = "",
    val connectedUserId: String = "",
    val connectedUserName: String = "",
    val connectedUserEmail: String = "",
    val type: String = "friend", // "family" o "friend"
    val status: String = "pending", // "pending" o "accepted"
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", "", "friend", "pending", Timestamp.now())
}

data class UserLocation(
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false
) {
    constructor() : this("", "", 0.0, 0.0, System.currentTimeMillis(), false)
}

data class Emergency(
    val emergencyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val audioUrl: String = ""
) {
    constructor() : this("", "", "", 0.0, 0.0, Timestamp.now(), true, "")
}