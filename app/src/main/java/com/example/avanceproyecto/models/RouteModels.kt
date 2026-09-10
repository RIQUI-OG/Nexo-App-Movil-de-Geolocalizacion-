package com.example.avanceproyecto.models

data class RoutePoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    constructor() : this(0.0, 0.0)
}

data class Route(
    val routeId: String = "",
    val userId: String = "",
    val routeName: String = "",
    val transportMode: String = "Caminando", // "Caminando", "Auto", "Bicicleta", "Transporte Público"
    val startName: String = "",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endName: String = "",
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val waypoints: List<RoutePoint> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "Caminando", "", 0.0, 0.0, "", 0.0, 0.0, emptyList(), System.currentTimeMillis())
}

data class ActiveTrip(
    val tripId: String = "",
    val userId: String = "",
    val userName: String = "",
    val routeId: String = "",
    val routeName: String = "",
    val transportMode: String = "Caminando",
    val status: String = "IN_PROGRESS", // "IN_PROGRESS", "DEVIATED", "COMPLETED"
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val waypoints: List<RoutePoint> = emptyList(),
    val deviationAlertSent: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "Caminando", "IN_PROGRESS", 0.0, 0.0, emptyList(), false, System.currentTimeMillis(), System.currentTimeMillis())
}

data class RouteAlert(
    val alertId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val userName: String = "",
    val routeName: String = "",
    val type: String = "TRIP_STARTED", // "TRIP_STARTED", "DEVIATION", "TRIP_COMPLETED"
    val message: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "TRIP_STARTED", "", 0.0, 0.0, System.currentTimeMillis())
}
