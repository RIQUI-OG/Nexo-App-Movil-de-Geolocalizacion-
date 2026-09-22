package com.example.avanceproyecto.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.sin

object MarkerOffsetHelper {

    private const val OVERLAP_THRESHOLD = 0.00008 // Aproximadamente 8-10 metros
    private const val OFFSET_RADIUS = 0.00014     // Dispersión de ~15 metros

    /**
     * Reorganiza las posiciones de los marcadores para que, si dos o más personas están en las mismas coordenadas exactas,
     * se dispersen circularmente y ambas fotos de perfil sean visibles en el mapa.
     */
    fun applyOffsetToOverlappingLocations(
        locationsMap: Map<String, LatLng>
    ): Map<String, LatLng> {
        if (locationsMap.size <= 1) return locationsMap

        val result = mutableMapOf<String, LatLng>()
        val processedKeys = mutableSetOf<String>()

        val entries = locationsMap.entries.toList()

        for (i in entries.indices) {
            val (keyI, posI) = entries[i]
            if (processedKeys.contains(keyI)) continue

            // Buscar todos los contactos que están encimados cerca de posI
            val overlappingGroup = mutableListOf<Pair<String, LatLng>>()
            overlappingGroup.add(keyI to posI)

            for (j in i + 1 until entries.size) {
                val (keyJ, posJ) = entries[j]
                if (processedKeys.contains(keyJ)) continue

                val latDiff = Math.abs(posI.latitude - posJ.latitude)
                val lngDiff = Math.abs(posI.longitude - posJ.longitude)

                if (latDiff < OVERLAP_THRESHOLD && lngDiff < OVERLAP_THRESHOLD) {
                    overlappingGroup.add(keyJ to posJ)
                }
            }

            // Si hay 2 o más personas en el mismo punto, aplicar dispersión radial
            if (overlappingGroup.size > 1) {
                val centerLat = posI.latitude
                val centerLng = posI.longitude
                val count = overlappingGroup.size

                for (index in 0 until count) {
                    val (key, _) = overlappingGroup[index]
                    val angle = 2 * Math.PI * index / count

                    val newLat = centerLat + OFFSET_RADIUS * sin(angle)
                    val newLng = centerLng + (OFFSET_RADIUS * cos(angle) / cos(Math.toRadians(centerLat)))

                    result[key] = LatLng(newLat, newLng)
                    processedKeys.add(key)
                }
            } else {
                result[keyI] = posI
                processedKeys.add(keyI)
            }
        }

        return result
    }
}
