package com.example.avanceproyecto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
import android.content.DialogInterface
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import android.Manifest
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.avanceproyecto.models.Emergency
import com.google.firebase.Timestamp

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mMap: GoogleMap? = null

    private var userId: String = ""
    private var userName: String = ""

    private val CHANNEL_ID = "nexo_alerts_channel"
    private val NOTIFICATION_ID = 1
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener datos del usuario
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: "Usuario"

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupMap()
        createNotificationChannel()
        requestLocationPermission()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.homeMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Nexo Seguridad"
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navView.getHeaderView(0)
        val imgProfileHeader = headerView.findViewById<android.widget.ImageView>(R.id.img_profile_header)
        imgProfileHeader?.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupUI() {
        val tvWelcomeTitle = findViewById<TextView>(R.id.tvWelcomeTitle)
        tvWelcomeTitle.text = "¡BIENVENIDO(A), $userName!"

        // Botón de Emergencia - Click en toda la tarjeta
        val cardEmergency = findViewById<CardView>(R.id.cardEmergency)
        cardEmergency.setOnClickListener {
            showEmergencyConfirmation()
        }

        // Mapa desplegado en Home (clic abre vista de mapa completa)
        val cardMapContainer = findViewById<CardView>(R.id.cardMapContainer)
        cardMapContainer?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // Botón de Rutas
        val cardRoutes = findViewById<CardView>(R.id.cardRoutes)
        cardRoutes?.setOnClickListener {
            val intent = Intent(this, RoutesActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // Botón de Contactos
        val cardContacts = findViewById<CardView>(R.id.cardContacts)
        cardContacts.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // Botón de Chats
        val cardChats = findViewById<CardView>(R.id.cardChats)
        cardChats?.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun showEmergencyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Alerta de Emergencia")
            .setMessage("¿Estás seguro de enviar una alerta de emergencia a tu familia?\n\nSe compartirá tu ubicación actual.")
            .setPositiveButton("SÍ, ENVIAR") { _, _ ->
                sendEmergencyAlert()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendEmergencyAlert() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                saveEmergencyToFirebase(it.latitude, it.longitude)
                sendNotificationToFamily(it.latitude, it.longitude)
                showLocalNotification(it.latitude, it.longitude)
            } ?: run {
                Toast.makeText(this, "❌ No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEmergencyToFirebase(latitude: Double, longitude: Double) {
        val emergency = Emergency(
            emergencyId = firestore.collection("emergencies").document().id,
            userId = userId,
            userName = userName,
            latitude = latitude,
            longitude = longitude,
            timestamp = Timestamp.now(),
            isActive = true,
            audioUrl = ""
        )

        firestore.collection("emergencies")
            .document(emergency.emergencyId)
            .set(emergency)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Alerta enviada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // También actualizar ubicación en Realtime Database con estado de emergencia
        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis(),
            "isEmergency" to true
        )

        realtimeDatabase.getReference("locations/$userId")
            .setValue(locationData)
    }

    private fun sendNotificationToFamily(latitude: Double, longitude: Double) {
        // Obtener contactos familiares
        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "family")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val connectedUserId = document.getString("connectedUserId") ?: continue

                    // Guardar notificación en Firestore
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

                    firestore.collection("notifications")
                        .add(notificationData)
                }

                Snackbar.make(
                    findViewById(android.R.id.content),
                    "✅ Tu familia ha sido notificada",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun showLocalNotification(latitude: Double, longitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
                return
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_nexo)
            .setContentTitle("🚨 Alerta de Seguridad Enviada")
            .setContentText("$userName ha compartido su ubicación con la familia")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$userName ha activado una alerta de seguridad. Tu familia ha recibido tu ubicación actual.\n\nLat: $latitude, Lon: $longitude")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Seguridad"
            val descriptionText = "Notificaciones de alerta y ubicación familiar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_routes -> {
                val intent = Intent(this, RoutesActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_family_location -> {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_chat -> {
                val intent = Intent(this, ChatListActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_contacts -> {
                val intent = Intent(this, ContactsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                val intent = Intent(this, ConfiguracionActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        val exitAlert = AlertDialog.Builder(this)
        exitAlert.setTitle("Salir")
        exitAlert.setMessage("¿Seguro que quieres salir de la aplicación?")

        exitAlert.setPositiveButton("Sí") { _: DialogInterface, _: Int ->
            finishAffinity()
        }

        exitAlert.setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }

        exitAlert.setCancelable(false)
        exitAlert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "✅ Permisos de ubicación concedidos", Toast.LENGTH_SHORT).show()
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap?.isMyLocationEnabled = true
                    }
                }
            }
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendEmergencyAlert()
                }
            }
        }
    }
}