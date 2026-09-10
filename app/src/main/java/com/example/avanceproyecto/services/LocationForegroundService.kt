package com.example.avanceproyecto.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.avanceproyecto.HomeActivity
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.ActiveTrip
import com.example.avanceproyecto.models.RouteAlert
import com.example.avanceproyecto.utils.NotificationHelper
import com.example.avanceproyecto.utils.RouteTrackingHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var currentActiveTrip: ActiveTrip? = null
    private val processedAlerts = mutableSetOf<String>()

    companion object {
        private const val TAG = "LocationService"
        private const val SERVICE_NOTIFICATION_ID = 9901
    }

    override fun onCreate() {
        super.onCreate()
        auth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        userId = auth.currentUser?.uid ?: ""

        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inputUserId = intent?.getStringExtra("USER_ID")
        if (!inputUserId.isNullOrEmpty()) {
            userId = inputUserId
        } else if (userId.isEmpty()) {
            userId = auth.currentUser?.uid ?: ""
        }

        try {
            startForeground(SERVICE_NOTIFICATION_ID, createPermanentNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando startForeground", e)
        }

        startLocationTracking()
        listenToMyActiveTrip()
        listenToIncomingMessagesAndAlerts()

        return START_STICKY
    }

    private fun createPermanentNotification(): Notification {
        val notificationIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.logo_nexo)
            .setContentTitle("Nexo Seguridad Activa")
            .setContentText("Nexo está funcionando en segundo plano para mantener informados a tus contactos de tu ubicación.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Nexo está funcionando en segundo plano para mantener informados a tus contactos de tu ubicación.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        try {
            val locationRequest = LocationRequest.create().apply {
                interval = 8000
                fastestInterval = 4000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        updateLocationInFirebase(location)
                        checkRouteDeviationInBackground(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando seguimiento GPS en segundo plano", e)
        }
    }

    private fun updateLocationInFirebase(location: Location) {
        if (userId.isEmpty()) return

        try {
            val locationData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis(),
                "isEmergency" to false
            )

            realtimeDatabase.getReference("locations/$userId").setValue(locationData)

            currentActiveTrip?.let { trip ->
                if (trip.status == "IN_PROGRESS" || trip.status == "DEVIATED") {
                    val updates = mapOf(
                        "currentLat" to location.latitude,
                        "currentLng" to location.longitude,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    realtimeDatabase.getReference("active_trips/$userId").updateChildren(updates)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando GPS en segundo plano", e)
        }
    }

    private fun listenToMyActiveTrip() {
        if (userId.isEmpty()) return

        try {
            realtimeDatabase.getReference("active_trips/$userId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val trip = try {
                                snapshot.getValue(ActiveTrip::class.java)
                            } catch (e: Exception) {
                                null
                            }
                            if (trip != null && (trip.status == "IN_PROGRESS" || trip.status == "DEVIATED")) {
                                currentActiveTrip = trip
                            } else {
                                currentActiveTrip = null
                            }
                        } else {
                            currentActiveTrip = null
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkRouteDeviationInBackground(location: Location) {
        val trip = currentActiveTrip ?: return
        if (trip.status != "IN_PROGRESS" || trip.waypoints.isEmpty()) return

        val currentLatLng = LatLng(location.latitude, location.longitude)
        val minDistance = RouteTrackingHelper.minDistanceFromRoute(currentLatLng, trip.waypoints)
        val threshold = RouteTrackingHelper.getThresholdForTransportMode(trip.transportMode)

        if (minDistance > threshold && !trip.deviationAlertSent) {
            val alertMessage = "⚠️ ¡ALERTA DE SEGUNDO PLANO! Te has desviado de la ruta '${trip.routeName}'"

            realtimeDatabase.getReference("active_trips/$userId/status").setValue("DEVIATED")
            realtimeDatabase.getReference("active_trips/$userId/deviationAlertSent").setValue(true)

            NotificationHelper.showSecurityAlertNotification(
                this,
                "🚨 ¡ALERTA DE DESVIACIÓN DE RUTA!",
                alertMessage
            )
        }
    }

    private fun listenToIncomingMessagesAndAlerts() {
        if (userId.isEmpty()) return

        try {
            realtimeDatabase.getReference("route_alerts/$userId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return

                        val alert = try {
                            snapshot.getValue(RouteAlert::class.java)
                        } catch (e: Exception) {
                            null
                        } ?: return

                        if (!processedAlerts.contains(alert.alertId) && alert.alertId.isNotEmpty()) {
                            processedAlerts.add(alert.alertId)

                            val title = when (alert.type) {
                                "DEVIATION" -> "🚨 ¡ALERTA DE DESVIACIÓN!"
                                "TRIP_STARTED" -> "🚀 Inicio de Ruta - ${alert.userName}"
                                else -> "🚨 Alerta de Seguridad"
                            }

                            NotificationHelper.showSecurityAlertNotification(
                                this@LocationForegroundService,
                                title,
                                alert.message
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationForegroundService detenido")
    }
}
