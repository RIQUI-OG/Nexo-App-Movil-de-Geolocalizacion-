package com.example.avanceproyecto.utils

import android.location.Location
import com.example.avanceproyecto.models.RoutePoint
import com.google.android.gms.maps.model.LatLng

object RouteTrackingHelper {

    /**
     * Retorna el umbral de distancia máxima tolerada (en metros) antes de considerar
     * que el usuario se ha salido de la ruta, dependiendo del modo de transporte.
     */
    fun getThresholdForTransportMode(transportMode: String): Double {
        return when (transportMode.lowercase()) {
            "caminando", "walking" -> 100.0 // 100 metros
            "bicicleta", "bicycling" -> 150.0 // 150 metros
            "auto", "driving" -> 250.0 // 250 metros
            "transporte público", "transporte", "transit" -> 300.0 // 300 metros
            else -> 150.0
        }
    }

    /**
     * Calcula la distancia mínima en metros desde la ubicación actual a la polilínea formada por los waypoints.
     */
    fun minDistanceFromRoute(currentLocation: LatLng, routePoints: List<RoutePoint>): Double {
        if (routePoints.isEmpty()) return 0.0
        if (routePoints.size == 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                routePoints[0].latitude, routePoints[0].longitude,
                results
            )
            return results[0].toDouble()
        }

        var minDistance = Double.MAX_VALUE

        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]

            val distToSegment = distanceToSegment(
                currentLocation,
                LatLng(p1.latitude, p1.longitude),
                LatLng(p2.latitude, p2.longitude)
            )

            if (distToSegment < minDistance) {
                minDistance = distToSegment
            }
        }

        return minDistance
    }

    /**
     * Distancia en metros desde un punto P al segmento de recta formado por A y B.
     */
    private fun distanceToSegment(p: LatLng, a: LatLng, b: LatLng): Double {
        val resultsA = FloatArray(1)
        Location.distanceBetween(p.latitude, p.longitude, a.latitude, a.longitude, resultsA)
        val distA = resultsA[0].toDouble()

        val resultsB = FloatArray(1)
        Location.distanceBetween(p.latitude, p.longitude, b.latitude, b.longitude, resultsB)
        val distB = resultsB[0].toDouble()

        val resultsAB = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, resultsAB)
        val lengthAB = resultsAB[0].toDouble()

        if (lengthAB == 0.0) return distA

        val deltaX = b.longitude - a.longitude
        val deltaY = b.latitude - a.latitude

        val t = ((p.longitude - a.longitude) * deltaX + (p.latitude - a.latitude) * deltaY) / (deltaX * deltaX + deltaY * deltaY)

        return if (t < 0.0) {
            distA
        } else if (t > 1.0) {
            distB
        } else {
            val projLat = a.latitude + t * deltaY
            val projLng = a.longitude + t * deltaX
            val resultsProj = FloatArray(1)
            Location.distanceBetween(p.latitude, p.longitude, projLat, projLng, resultsProj)
            resultsProj[0].toDouble()
        }
    }
}
