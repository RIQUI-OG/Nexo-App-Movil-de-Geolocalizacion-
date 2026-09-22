package com.example.avanceproyecto.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.avanceproyecto.HomeActivity
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.ActiveTrip
import com.example.avanceproyecto.models.ChatConversation
import com.example.avanceproyecto.models.ChatMessage
import com.example.avanceproyecto.models.Emergency
import com.example.avanceproyecto.models.RouteAlert
import com.example.avanceproyecto.utils.NotificationHelper
import com.example.avanceproyecto.utils.RouteTrackingHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt

class LocationForegroundService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var userName: String = "Usuario"
    private var currentActiveTrip: ActiveTrip? = null
    private val processedAlerts = mutableSetOf<String>()
    private val processedMessages = mutableSetOf<String>()
    private val chatMessageListeners = mutableMapOf<String, ChildEventListener>()
    private var serviceStartTime = System.currentTimeMillis()

    // Sensor Shake
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private var shakeCount = 0

    // Audio & Alarma Sonora
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    companion object {
        private const val TAG = "LocationService"
        private const val SERVICE_NOTIFICATION_ID = 9901
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_RESET_TIME_MS = 3000

        private var activeInstance: LocationForegroundService? = null

        fun stopAlarm() {
            activeInstance?.stopLoudAlarm()
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        userId = auth.currentUser?.uid ?: ""
        serviceStartTime = System.currentTimeMillis()

        loadUserName()
        initShakeSensor()
        initAudioSystem()

        NotificationHelper.createNotificationChannels(this)
    }

    fun stopLoudAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadUserName() {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: "Usuario"
                }
        }
    }

    private fun initShakeSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun initAudioSystem() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inputUserId = intent?.getStringExtra("USER_ID")
        if (!inputUserId.isNullOrEmpty()) {
            userId = inputUserId
            loadUserName()
        } else if (userId.isEmpty()) {
            userId = auth.currentUser?.uid ?: ""
            loadUserName()
        }

        try {
            startForeground(SERVICE_NOTIFICATION_ID, createPermanentNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando startForeground", e)
        }

        val action = intent?.action
        if (action == "ACTION_SILENT_EMERGENCY") {
            triggerEmergencyAlert(silent = true)
        } else if (action == "ACTION_LOUD_EMERGENCY") {
            triggerEmergencyAlert(silent = false)
        }

        startLocationTracking()
        listenToMyActiveTrip()
        listenToIncomingMessagesAndAlerts()
        listenToIncomingChatMessages()

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()

                if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                if (lastShakeTime + SHAKE_RESET_TIME_MS < now) {
                    shakeCount = 0
                }

                lastShakeTime = now
                shakeCount++

                if (shakeCount >= 3) {
                    shakeCount = 0
                    vibrateFeedback()
                    triggerEmergencyAlert(silent = true)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @SuppressLint("MissingPermission")
    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playLoudAlarm() {
        try {
            audioManager?.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 100,
                0
            )

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo alarma", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerEmergencyAlert(silent: Boolean) {
        if (!silent) {
            playLoudAlarm()
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                saveEmergencyToFirebase(it.latitude, it.longitude)
                sendNotificationToFamily(it.latitude, it.longitude)
                sendEmergencyMessageToAllGroups(it.latitude, it.longitude)
                NotificationHelper.showSecurityAlertNotification(
                    this,
                    if (silent) "🚨 Alerta Silenciosa Enviada" else "🚨 Alerta Sonora Enviada",
                    "Tu ubicación GPS ha sido compartida con tu familia."
                )
            }
        }
    }

    private fun saveEmergencyToFirebase(latitude: Double, longitude: Double) {
        val emergencyId = firestore.collection("emergencies").document().id
        val emergency = Emergency(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            latitude = latitude,
            longitude = longitude,
            timestamp = Timestamp.now(),
            isActive = true,
            audioUrl = ""
        )

        firestore.collection("emergencies").document(emergencyId).set(emergency)

        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis(),
            "isEmergency" to true
        )
        realtimeDatabase.getReference("locations/$userId").setValue(locationData)
    }

    private fun sendNotificationToFamily(latitude: Double, longitude: Double) {
        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "family")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val connectedUserId = document.getString("connectedUserId") ?: continue
                    val notificationData = mapOf(
                        "fromUserId" to userId,
                        "fromUserName" to userName,
                        "toUserId" to connectedUserId,
                        "type" to "emergency",
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "timestamp" to Timestamp.now(),
                        "read" to false
                    )
                    firestore.collection("notifications").add(notificationData)
                }
            }
    }

    private fun sendEmergencyMessageToAllGroups(latitude: Double, longitude: Double) {
        realtimeDatabase.getReference("chat_conversations")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (itemSnapshot in snapshot.children) {
                        val conversation = try {
                            itemSnapshot.getValue(ChatConversation::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        if (conversation != null && conversation.isGroup) {
                            val participants = conversation.participants ?: emptyList()
                            if (participants.contains(userId)) {
                                val groupId = conversation.chatId
                                if (groupId.isNotEmpty() && groupId != "group_family_$userId") {
                                    sendEmergencyMessageToChat(groupId, latitude, longitude)
                                }
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        val familyGroupId = "group_family_$userId"
        sendEmergencyMessageToChat(familyGroupId, latitude, longitude, true)
    }

    private fun sendEmergencyMessageToChat(chatId: String, latitude: Double, longitude: Double, isLegacyFamily: Boolean = false) {
        val msgRef = realtimeDatabase.getReference("chat_messages/$chatId").push()
        val msgId = msgRef.key ?: System.currentTimeMillis().toString()

        val emergencyMessage = ChatMessage(
            messageId = msgId,
            chatId = chatId,
            senderId = "SYSTEM_EMERGENCY",
            senderName = "🚨 ALERTA DE EMERGENCIA",
            text = "🚨 ¡ATENCIÓN! $userName ha activado la alerta de emergencia.\nUbicación GPS: Lat $latitude, Lon $longitude",
            timestamp = System.currentTimeMillis()
        )
        msgRef.setValue(emergencyMessage)

        val conversationUpdate = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "lastMessage" to "🚨 ¡ALERTA DE EMERGENCIA ACTIVADA POR $userName!",
            "lastMessageTime" to System.currentTimeMillis()
        )
        if (isLegacyFamily) {
            conversationUpdate["title"] = "👨‍👩‍👧‍👦 Familia de $userName"
            conversationUpdate["isGroup"] = true
        }
        realtimeDatabase.getReference("chat_conversations/$chatId").updateChildren(conversationUpdate)
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

    private fun listenToIncomingChatMessages() {
        if (userId.isEmpty() || userId.startsWith("guest_")) return

        try {
            realtimeDatabase.getReference("chat_conversations")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (convSnapshot in snapshot.children) {
                            val conversation = try {
                                convSnapshot.getValue(ChatConversation::class.java)
                            } catch (e: Exception) {
                                null
                            } ?: continue

                            val isMyChat = conversation.isGroup || conversation.chatId.contains(userId) || conversation.participants?.contains(userId) == true
                            if (isMyChat) {
                                attachMessageListenerToChat(conversation.chatId, conversation.title)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error escuchando conversaciones de chat", e)
        }
    }

    private fun attachMessageListenerToChat(chatId: String, chatTitle: String) {
        if (chatMessageListeners.containsKey(chatId)) return

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = try {
                    snapshot.getValue(ChatMessage::class.java)
                } catch (e: Exception) {
                    null
                } ?: return

                if (message.senderId != userId && message.timestamp >= serviceStartTime) {
                    if (!processedMessages.contains(message.messageId) && message.messageId.isNotEmpty()) {
                        processedMessages.add(message.messageId)

                        val displayTitle = if (chatTitle.isNotEmpty()) chatTitle else message.senderName
                        val displayText = if (chatTitle.isNotEmpty() && !chatTitle.contains(message.senderName)) {
                            "${message.senderName}: ${message.text}"
                        } else {
                            message.text
                        }

                        NotificationHelper.showChatNotification(
                            context = this@LocationForegroundService,
                            senderName = displayTitle,
                            messageText = displayText,
                            chatId = chatId
                        )
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        chatMessageListeners[chatId] = listener
        realtimeDatabase.getReference("chat_messages/$chatId")
            .orderByChild("timestamp")
            .startAt(serviceStartTime.toDouble())
            .addChildEventListener(listener)
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

                        // Solo notificar si la alerta ocurrió MIENTRAS el servicio está activo (no alertas antiguas almacenadas previamente)
                        if (alert.timestamp >= serviceStartTime && !processedAlerts.contains(alert.alertId) && alert.alertId.isNotEmpty()) {
                            processedAlerts.add(alert.alertId)

                            val title = when (alert.type) {
                                "DEVIATION" -> "🚨 ¡ALERTA DE DESVIACIÓN!"
                                "TRIP_STARTED" -> "🚀 Inicio de Ruta - ${alert.userName}"
                                "TRIP_COMPLETED" -> "✅ Ruta Concluida - ${alert.userName}"
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
        activeInstance = null
        sensorManager.unregisterListener(this)
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "LocationForegroundService detenido")
    }
}
