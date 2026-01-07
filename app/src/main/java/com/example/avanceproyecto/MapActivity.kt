package com.example.avanceproyecto

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
import androidx.core.view.GravityCompat

class MapActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout

    private var currentLocation: Location? = null
    private var userId: String = ""
    private val contactMarkers = mutableMapOf<String, Marker>()
    private var myLocationMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Configurar toolbar y drawer
        setupToolbarAndDrawer()

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar LocationRequest para actualizaciones en tiempo real
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Actualizar cada 5 segundos
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateMyLocationOnMap(location)
                    updateLocationInFirebase(location)
                }
            }
        }

        // Obtener el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Solicitar permisos si es necesario
        requestLocationPermission()
    }

    private fun setupToolbarAndDrawer() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Ubicaciones"

        drawerLayout = findViewById(R.id.drawer_layout_map)
        val navView: NavigationView = findViewById(R.id.nav_view_map)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                finish()
            }
            R.id.nav_schedule_trip -> {
                // Navegar a programar viaje
            }
            R.id.nav_family_location -> {
                Toast.makeText(this, "Ya estás en ubicaciones", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Configuración próximamente", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                auth.signOut()
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    private fun updateLocationInFirebase(location: Location) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "isEmergency" to false
        )

        realtimeDatabase.getReference("locations/$userId")
            .setValue(locationData)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configuraciones del mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Habilitar mi ubicación si hay permisos
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }

        // Si ya tenemos la ubicación, centrar el mapa
        currentLocation?.let {
            centerMapOnLocation(it)
        } ?: run {
            // Ubicación por defecto (México City) si no hay GPS
            val defaultLocation = LatLng(19.432608, -99.133209)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        }

        // Cargar ubicaciones de contactos
        loadContactsLocations()
    }

    private fun updateMyLocationOnMap(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (myLocationMarker == null) {
            // Crear nuevo marcador para mi ubicación
            myLocationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Tu ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            // Centrar cámara solo la primera vez
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            // Actualizar posición del marcador existente
            myLocationMarker?.position = latLng
        }
    }

    private fun centerMapOnLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun loadContactsLocations() {
        // Obtener lista de contactos
        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val connectedUserId = document.getString("connectedUserId") ?: continue
                    val connectedUserName = document.getString("connectedUserName") ?: "Contacto"
                    val type = document.getString("type") ?: "friend"

                    // Escuchar cambios en tiempo real de la ubicación de cada contacto
                    listenToContactLocation(connectedUserId, connectedUserName, type)
                }
            }
    }

    private fun listenToContactLocation(contactId: String, contactName: String, type: String) {
        realtimeDatabase.getReference("locations/$contactId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latitude = snapshot.child("latitude").getValue(Double::class.java)
                    val longitude = snapshot.child("longitude").getValue(Double::class.java)
                    val isEmergency = snapshot.child("isEmergency").getValue(Boolean::class.java) ?: false

                    if (latitude != null && longitude != null) {
                        updateContactMarker(contactId, contactName, latitude, longitude, type, isEmergency)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MapActivity,
                        "Error al obtener ubicación de $contactName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateContactMarker(
        contactId: String,
        contactName: String,
        latitude: Double,
        longitude: Double,
        type: String,
        isEmergency: Boolean
    ) {
        val latLng = LatLng(latitude, longitude)

        // Determinar color del marcador según el tipo y estado
        val markerColor = when {
            isEmergency -> BitmapDescriptorFactory.HUE_RED
            type == "family" -> BitmapDescriptorFactory.HUE_GREEN
            else -> BitmapDescriptorFactory.HUE_ORANGE
        }

        val title = if (isEmergency) "🚨 EMERGENCIA - $contactName" else contactName
        val snippet = if (type == "family") "👨‍👩‍👧‍👦 Familiar" else "👤 Amigo"

        if (contactMarkers.containsKey(contactId)) {
            // Actualizar marcador existente
            contactMarkers[contactId]?.apply {
                position = latLng
                this.title = title
            }
        } else {
            // Crear nuevo marcador
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            if (marker != null) {
                contactMarkers[contactId] = marker
            }
        }
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
                    startLocationUpdates()

                    if (::mMap.isInitialized) {
                        try {
                            mMap.isMyLocationEnabled = true
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}