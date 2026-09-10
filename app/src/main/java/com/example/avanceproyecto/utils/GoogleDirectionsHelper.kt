package com.example.avanceproyecto.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import com.example.avanceproyecto.models.RoutePoint
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

data class RouteStepDetail(
    val travelMode: String = "WALKING", // "WALKING", "TRANSIT", "DRIVING", "BICYCLING"
    val icon: String = "🚶",
    val lineName: String = "",
    val lineColorHex: String = "#156082",
    val instructions: String = "",
    val durationText: String = ""
)

data class RouteOption(
    val id: Int = 0,
    val summary: String = "Ruta Principal",
    val distanceText: String = "",
    val durationText: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val fareText: String = "",
    val stepDetails: List<RouteStepDetail> = emptyList(),
    val waypoints: List<RoutePoint> = emptyList(),
    val startAddress: String = "",
    val endAddress: String = ""
)

data class ModeDurations(
    val drivingDuration: String = "",
    val transitDuration: String = "",
    val walkingDuration: String = "",
    val bicyclingDuration: String = ""
)

object GoogleDirectionsHelper {

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    interface DirectionsCallback {
        fun onSuccess(routeOptions: List<RouteOption>)
        fun onError(errorMessage: String)
    }

    interface DurationsCallback {
        fun onDurationsReady(durations: ModeDurations)
    }

    /**
     * Convierte una dirección de texto a LatLng
     */
    fun geocodeAddress(context: Context, addressStr: String): LatLng? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocationName(addressStr, 1)
            if (!addresses.isNullOrEmpty()) {
                LatLng(addresses[0].latitude, addresses[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene sugerencias de direcciones para autocompletado
     */
    fun getAddressSuggestions(context: Context, query: String): List<String> {
        if (query.length < 3) return emptyList()
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocationName(query, 5)
            addresses?.mapNotNull { address ->
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    if (i > 0) sb.append(", ")
                    sb.append(address.getAddressLine(i))
                }
                if (sb.isNotEmpty()) sb.toString() else address.featureName
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Consulta las duraciones para todos los medios de transporte simultáneamente
     */
    fun fetchAllModeDurations(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        callback: DurationsCallback
    ) {
        executor.execute {
            val drivingDur = fetchSingleModeDuration(origin, destination, "driving", apiKey)
            val transitDur = fetchSingleModeDuration(origin, destination, "transit", apiKey)
            val walkingDur = fetchSingleModeDuration(origin, destination, "walking", apiKey)
            val bicyclingDur = fetchSingleModeDuration(origin, destination, "bicycling", apiKey)

            val durations = ModeDurations(
                drivingDuration = drivingDur,
                transitDuration = transitDur,
                walkingDuration = walkingDur,
                bicyclingDuration = bicyclingDur
            )

            mainHandler.post {
                callback.onDurationsReady(durations)
            }
        }
    }

    private fun fetchSingleModeDuration(origin: LatLng, destination: LatLng, mode: String, apiKey: String): String {
        return try {
            val urlString = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=$mode" +
                    "&key=$apiKey"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                if (json.optString("status") == "OK") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val legs = routes.getJSONObject(0).getJSONArray("legs")
                        if (legs.length() > 0) {
                            return legs.getJSONObject(0).getJSONObject("duration").getString("text")
                        }
                    }
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Consulta las opciones de ruta detalladas de la Google Maps Directions API
     */
    fun fetchRoute(
        origin: LatLng,
        destination: LatLng,
        transportMode: String,
        apiKey: String,
        callback: DirectionsCallback
    ) {
        executor.execute {
            try {
                val modeParam = when (transportMode.lowercase()) {
                    "auto", "driving" -> "driving"
                    "caminando", "walking" -> "walking"
                    "bicicleta", "bicycling" -> "bicycling"
                    "transporte público", "transporte", "transit" -> "transit"
                    else -> "transit"
                }

                val urlString = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=${origin.latitude},${origin.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&mode=$modeParam" +
                        "&alternatives=true" +
                        "&key=$apiKey"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val json = JSONObject(response.toString())
                    val status = json.optString("status", "UNKNOWN")

                    if (status == "OK") {
                        val routesArray = json.getJSONArray("routes")
                        val routeOptions = mutableListOf<RouteOption>()

                        for (i in 0 until routesArray.length()) {
                            val routeObj = routesArray.getJSONObject(i)
                            var summaryStr = routeObj.optString("summary", "")
                            if (summaryStr.isEmpty()) {
                                summaryStr = if (i == 0) "Ruta Principal" else "Ruta Alternativa $i"
                            } else {
                                summaryStr = "vía $summaryStr"
                            }

                            val overviewPolyline = routeObj.getJSONObject("overview_polyline").getString("points")
                            val legs = routeObj.getJSONArray("legs")

                            var distanceText = ""
                            var durationText = ""
                            var startAddr = ""
                            var endAddr = ""
                            var depTime = ""
                            var arrTime = ""
                            var fareText = ""
                            val stepDetails = mutableListOf<RouteStepDetail>()

                            if (legs.length() > 0) {
                                val leg0 = legs.getJSONObject(0)
                                distanceText = leg0.optJSONObject("distance")?.optString("text", "") ?: ""
                                durationText = leg0.optJSONObject("duration")?.optString("text", "") ?: ""
                                startAddr = leg0.optString("start_address", "")
                                endAddr = leg0.optString("end_address", "")

                                depTime = leg0.optJSONObject("departure_time")?.optString("text", "") ?: ""
                                arrTime = leg0.optJSONObject("arrival_time")?.optString("text", "") ?: ""

                                val fareObj = routeObj.optJSONObject("fare")
                                if (fareObj != null) {
                                    fareText = fareObj.optString("text", "")
                                }

                                // Parsear pasos
                                val steps = leg0.optJSONArray("steps")
                                if (steps != null) {
                                    for (j in 0 until steps.length()) {
                                        val stepObj = steps.getJSONObject(j)
                                        val travelMode = stepObj.optString("travel_mode", "WALKING")
                                        val stepDuration = stepObj.optJSONObject("duration")?.optString("text", "") ?: ""
                                        val instructions = stepObj.optString("html_instructions", "")
                                            .replace(Regex("<[^>]*>"), " ")
                                            .trim()

                                        if (travelMode == "TRANSIT") {
                                            val transitDetails = stepObj.optJSONObject("transit_details")
                                            val lineObj = transitDetails?.optJSONObject("line")
                                            val lineShortName = lineObj?.optString("short_name", "") ?: ""
                                            val lineName = lineObj?.optString("name", "") ?: lineShortName
                                            val colorHex = lineObj?.optString("color", "#156082") ?: "#156082"

                                            val vehicleObj = lineObj?.optJSONObject("vehicle")
                                            val vehicleType = vehicleObj?.optString("type", "BUS") ?: "BUS"

                                            val icon = when (vehicleType.uppercase()) {
                                                "SUBWAY", "METRO", "HEAVY_RAIL" -> "🚇"
                                                "BUS", "TROLLEYBUS", "SHARE_TAXI" -> "🚌"
                                                "TRAM", "LIGHT_RAIL" -> "🚊"
                                                else -> "🚌"
                                            }

                                            stepDetails.add(
                                                RouteStepDetail(
                                                    travelMode = "TRANSIT",
                                                    icon = icon,
                                                    lineName = if (lineShortName.isNotEmpty()) lineShortName else lineName,
                                                    lineColorHex = if (colorHex.startsWith("#")) colorHex else "#$colorHex",
                                                    instructions = instructions,
                                                    durationText = stepDuration
                                                )
                                            )
                                        } else if (travelMode == "WALKING") {
                                            stepDetails.add(
                                                RouteStepDetail(
                                                    travelMode = "WALKING",
                                                    icon = "🚶",
                                                    lineName = "",
                                                    durationText = stepDuration
                                                )
                                            )
                                        } else if (travelMode == "DRIVING") {
                                            stepDetails.add(
                                                RouteStepDetail(
                                                    travelMode = "DRIVING",
                                                    icon = "🚗",
                                                    durationText = stepDuration
                                                )
                                            )
                                        } else if (travelMode == "BICYCLING") {
                                            stepDetails.add(
                                                RouteStepDetail(
                                                    travelMode = "BICYCLING",
                                                    icon = "🚴",
                                                    durationText = stepDuration
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            val decodedLatLngs = PolyUtil.decode(overviewPolyline)
                            val waypoints = decodedLatLngs.map { RoutePoint(it.latitude, it.longitude) }

                            routeOptions.add(
                                RouteOption(
                                    id = i,
                                    summary = summaryStr,
                                    distanceText = distanceText,
                                    durationText = durationText,
                                    departureTime = depTime,
                                    arrivalTime = arrTime,
                                    fareText = fareText,
                                    stepDetails = stepDetails,
                                    waypoints = waypoints,
                                    startAddress = startAddr,
                                    endAddress = endAddr
                                )
                            )
                        }

                        if (routeOptions.isNotEmpty()) {
                            mainHandler.post {
                                callback.onSuccess(routeOptions)
                            }
                            return@execute
                        }
                    }
                }

                fallbackDirectRoute(origin, destination, callback)

            } catch (e: Exception) {
                e.printStackTrace()
                fallbackDirectRoute(origin, destination, callback)
            }
        }
    }

    private fun fallbackDirectRoute(origin: LatLng, destination: LatLng, callback: DirectionsCallback) {
        val fallbackPoints = listOf(
            RoutePoint(origin.latitude, origin.longitude),
            RoutePoint(destination.latitude, destination.longitude)
        )
        val defaultOption = RouteOption(
            id = 0,
            summary = "Ruta Directa",
            distanceText = "Distancia aprox.",
            durationText = "Tiempo aprox.",
            departureTime = "",
            arrivalTime = "",
            fareText = "",
            stepDetails = listOf(
                RouteStepDetail("DRIVING", "🚗", "", "#156082", "Conducir directo", "")
            ),
            waypoints = fallbackPoints,
            startAddress = "Origen",
            endAddress = "Destino"
        )

        mainHandler.post {
            callback.onSuccess(listOf(defaultOption))
        }
    }
}
