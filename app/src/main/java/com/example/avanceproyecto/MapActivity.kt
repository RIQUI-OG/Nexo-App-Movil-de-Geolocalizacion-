package com.example.avanceproyecto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.avanceproyecto.adapters.FloatingContactsAdapter
import com.example.avanceproyecto.models.ActiveTrip
import com.example.avanceproyecto.models.RouteAlert
import com.example.avanceproyecto.models.RoutePoint
import com.example.avanceproyecto.models.UserConnection
import com.example.avanceproyecto.utils.CustomMapMarkerHelper
import com.example.avanceproyecto.utils.MarkerOffsetHelper
import com.example.avanceproyecto.utils.RouteTrackingHelper
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class MapActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout

    // UI para Panel Flotante de Contactos
    private lateinit var cardFloatingContactsPanel: CardView
    private lateinit var btnToggleFloatingPanel: ImageButton
    private lateinit var spinnerGroupFilter: Spinner
    private lateinit var rvFloatingContacts: RecyclerView
    private var isPanelExpanded = true

    // UI para Viaje Activo
    private lateinit var cardActiveTripBanner: CardView
    private lateinit var tvTripBannerTitle: TextView
    private lateinit var tvTripBannerStatus: TextView
    private lateinit var btnFinishTrip: MaterialButton

    private var currentLocation: Location? = null
    private var userId: String = ""
    private var userName: String = "Usuario"
    private var myPhotoUrl: String? = null
    private var focusContactId: String? = null
    private var focusContactName: String? = null

    private val contactMarkers = mutableMapOf<String, Marker>()
    private val contactLocationsMap = mutableMapOf<String, LatLng>()
    private val contactTripListeners = mutableMapOf<String, ValueEventListener>()
    private val contactLocationListeners = mutableMapOf<String, ValueEventListener>()
    private var myLocationMarker: Marker? = null

    private val allAcceptedContactsList = mutableListOf<UserConnection>()
    private val filteredContactsList = mutableListOf<UserConnection>()
    private lateinit var floatingContactsAdapter: FloatingContactsAdapter

    // Viaje activo y dibujado de ruta
    private var currentActiveTrip: ActiveTrip? = null
    private var myRoutePolyline: Polyline? = null
    private val contactPolylines = mutableMapOf<String, Polyline>()
    private val notifiedDeviations = mutableSetOf<String>()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    companion object {
        private const val TAG = "MapActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            userId = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        }

        focusContactId = intent.getStringExtra("FOCUS_CONTACT_ID")
        focusContactName = intent.getStringExtra("FOCUS_CONTACT_NAME")

        setupToolbarAndDrawer()
        initViews()
        loadUserName()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateMyLocationOnMap(location)
                    updateLocationInFirebase(location)
                    checkRouteDeviation(location)
                }
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestLocationPermission()
    }

    private fun initViews() {
        cardActiveTripBanner = findViewById(R.id.cardActiveTripBanner)
        tvTripBannerTitle = findViewById(R.id.tvTripBannerTitle)
        tvTripBannerStatus = findViewById(R.id.tvTripBannerStatus)
        btnFinishTrip = findViewById(R.id.btnFinishTrip)

        btnFinishTrip.setOnClickListener {
            confirmFinishActiveTrip()
        }

        cardFloatingContactsPanel = findViewById(R.id.cardFloatingContactsPanel)
        btnToggleFloatingPanel = findViewById(R.id.btnToggleFloatingPanel)
        spinnerGroupFilter = findViewById(R.id.spinnerGroupFilter)
        rvFloatingContacts = findViewById(R.id.rvFloatingContacts)

        rvFloatingContacts.layoutManager = LinearLayoutManager(this)
        floatingContactsAdapter = FloatingContactsAdapter(filteredContactsList) { contact ->
            focusOnContactLocation(contact)
        }
        rvFloatingContacts.adapter = floatingContactsAdapter

        btnToggleFloatingPanel.setOnClickListener {
            toggleFloatingContactsPanel()
        }

        setupSpinnerFilter()
    }

    private fun setupSpinnerFilter() {
        val filterOptions = arrayOf("Familia", "Amigos", "Todos")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filterOptions)
        spinnerGroupFilter.adapter = spinnerAdapter

        spinnerGroupFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterFloatingContacts(filterOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun toggleFloatingContactsPanel() {
        if (isPanelExpanded) {
            isPanelExpanded = false
            cardFloatingContactsPanel.visibility = View.GONE
            btnToggleFloatingPanel.setImageResource(android.R.drawable.ic_media_next)
        } else {
            isPanelExpanded = true
            cardFloatingContactsPanel.visibility = View.VISIBLE
            btnToggleFloatingPanel.setImageResource(android.R.drawable.ic_media_previous)
        }
    }

    private fun filterFloatingContacts(category: String) {
        filteredContactsList.clear()

        when (category.lowercase()) {
            "familia", "family" -> {
                filteredContactsList.addAll(allAcceptedContactsList.filter {
                    it.type.equals("familia", ignoreCase = true) || it.type.equals("family", ignoreCase = true)
                })
            }
            "amigos", "friend" -> {
                filteredContactsList.addAll(allAcceptedContactsList.filter {
                    !it.type.equals("familia", ignoreCase = true) && !it.type.equals("family", ignoreCase = true)
                })
            }
            else -> {
                filteredContactsList.addAll(allAcceptedContactsList)
            }
        }

        floatingContactsAdapter.notifyDataSetChanged()
    }

    private fun focusOnContactLocation(contact: UserConnection) {
        val latLng = contactLocationsMap[contact.connectedUserId]

        if (latLng != null && ::mMap.isInitialized) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            contactMarkers[contact.connectedUserId]?.showInfoWindow()
            Toast.makeText(this, "📍 Enfocando ubicación de ${contact.connectedUserName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Conectando con la señal GPS de ${contact.connectedUserName}...", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadUserName() {
        if (userId.isNotEmpty() && !userId.startsWith("guest_")) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: "Usuario"
                    myPhotoUrl = doc.getString("photoUrl")

                    val navView: NavigationView = findViewById(R.id.nav_view_map)
                    val headerView = navView.getHeaderView(0)
                    val imgProfileHeader = headerView?.findViewById<ImageView>(R.id.img_profile_header)
                    if (!myPhotoUrl.isNullOrEmpty() && imgProfileHeader != null) {
                        Glide.with(this)
                            .load(myPhotoUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(imgProfileHeader)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al cargar nombre de usuario", e)
                }
        }
    }

    private fun setupToolbarAndDrawer() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (focusContactName != null) {
            supportActionBar?.title = "📍 $focusContactName"
        } else {
            supportActionBar?.title = "Ubicación en Tiempo Real"
        }

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

        val headerView = navView.getHeaderView(0)
        val imgProfileHeader = headerView?.findViewById<ImageView>(R.id.img_profile_header)
        imgProfileHeader?.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                finish()
            }
            R.id.nav_routes -> {
                val intent = Intent(this, RoutesActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_family_location -> {
                Toast.makeText(this, "Ya estás en la pantalla de ubicación", Toast.LENGTH_SHORT).show()
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
                AlertDialog.Builder(this)
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de cerrar sesión?")
                    .setPositiveButton("Sí") { _, _ ->
                        auth.signOut()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()
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
                android.os.Looper.getMainLooper()
            )
        }
    }

    private fun updateLocationInFirebase(location: Location) {
        if (userId.isEmpty() || userId.startsWith("guest_")) return

        try {
            val locationData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis(),
                "isEmergency" to false
            )

            realtimeDatabase.getReference("locations/$userId")
                .setValue(locationData)

            currentActiveTrip?.let { trip ->
                if (trip.status == "IN_PROGRESS" || trip.status == "DEVIATED") {
                    val updates = mapOf(
                        "currentLat" to location.latitude,
                        "currentLng" to location.longitude,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    realtimeDatabase.getReference("active_trips/$userId")
                        .updateChildren(updates)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando ubicación en Firebase", e)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        currentLocation?.let {
            if (focusContactId == null) {
                centerMapOnLocation(it)
            }
        } ?: run {
            val defaultLocation = LatLng(19.432608, -99.133209)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13f))
        }

        try {
            loadContactsLocations()
            listenToMyActiveTrip()
            listenToContactsActiveTrips()
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar observadores de mapa", e)
        }
    }

    private fun listenToMyActiveTrip() {
        if (userId.isEmpty() || userId.startsWith("guest_")) return

        try {
            realtimeDatabase.getReference("active_trips/$userId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            hideMyActiveTrip()
                            return
                        }

                        val trip = try {
                            snapshot.getValue(ActiveTrip::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando ActiveTrip", e)
                            null
                        }

                        if (trip != null && (trip.status == "IN_PROGRESS" || trip.status == "DEVIATED")) {
                            currentActiveTrip = trip
                            showMyActiveTrip(trip)
                        } else {
                            hideMyActiveTrip()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando viaje activo", e)
        }
    }

    private fun showMyActiveTrip(trip: ActiveTrip) {
        cardActiveTripBanner.visibility = View.VISIBLE
        tvTripBannerTitle.text = "🚀 Ruta en Curso: ${trip.routeName}"

        if (trip.status == "DEVIATED") {
            tvTripBannerStatus.text = "⚠️ ¡ALERTA: Te has salido de la ruta trazada!"
            tvTripBannerStatus.setTextColor(Color.parseColor("#FF5252"))
            cardActiveTripBanner.setCardBackgroundColor(Color.parseColor("#B71C1C"))
        } else {
            tvTripBannerStatus.text = "🟢 Trayecto seguro monitoreado por tu familia"
            tvTripBannerStatus.setTextColor(Color.parseColor("#A7FFEB"))
            cardActiveTripBanner.setCardBackgroundColor(Color.parseColor("#156082"))
        }

        drawMyRoutePolyline(trip.waypoints, trip.status == "DEVIATED")
    }

    private fun hideMyActiveTrip() {
        currentActiveTrip = null
        cardActiveTripBanner.visibility = View.GONE
        myRoutePolyline?.remove()
        myRoutePolyline = null
    }

    private fun drawMyRoutePolyline(waypoints: List<RoutePoint>, isDeviated: Boolean) {
        if (!::mMap.isInitialized) return
        myRoutePolyline?.remove()

        if (waypoints.size >= 2) {
            val latLngs = waypoints.map { LatLng(it.latitude, it.longitude) }
            val polylineColor = if (isDeviated) Color.RED else Color.parseColor("#156082")

            myRoutePolyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(14f)
                    .color(polylineColor)
                    .geodesic(true)
            )

            if (currentLocation == null) {
                val boundsBuilder = LatLngBounds.Builder()
                for (p in latLngs) {
                    boundsBuilder.include(p)
                }
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkRouteDeviation(location: Location) {
        val trip = currentActiveTrip ?: return
        if (trip.status != "IN_PROGRESS" || trip.waypoints.isEmpty()) return

        val currentLatLng = LatLng(location.latitude, location.longitude)
        val minDistance = RouteTrackingHelper.minDistanceFromRoute(currentLatLng, trip.waypoints)
        val threshold = RouteTrackingHelper.getThresholdForTransportMode(trip.transportMode)

        if (minDistance > threshold && !trip.deviationAlertSent) {
            triggerDeviationAlert(trip, location)
        }
    }

    private fun triggerDeviationAlert(trip: ActiveTrip, location: Location) {
        val alertMessage = "⚠️ ¡ALERTA! $userName se ha salido de la ruta '${trip.routeName}'"

        val updates = mapOf(
            "status" to "DEVIATED",
            "deviationAlertSent" to true
        )

        realtimeDatabase.getReference("active_trips/$userId")
            .updateChildren(updates)

        val alertId = firestore.collection("route_alerts").document().id
        val alert = RouteAlert(
            alertId = alertId,
            tripId = trip.tripId,
            userId = userId,
            userName = userName,
            routeName = trip.routeName,
            type = "DEVIATION",
            message = alertMessage,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("route_alerts").document(alertId).set(alert)
        realtimeDatabase.getReference("route_alerts/$userId").setValue(alert)

        Toast.makeText(this, "⚠️ ¡ALERTA! Te has desviado de tu ruta. Tus contactos han sido notificados.", Toast.LENGTH_LONG).show()
    }

    private fun confirmFinishActiveTrip() {
        val trip = currentActiveTrip ?: return

        AlertDialog.Builder(this)
            .setTitle("🏁 Desactivar / Finalizar Ruta")
            .setMessage("¿Deseas finalizar la ruta actual? Se notificará a tus familiares que has concluido tu trayecto de forma segura.")
            .setPositiveButton("Sí, Finalizar") { dialog, _ ->
                dialog.dismiss()

                realtimeDatabase.getReference("active_trips/$userId/status")
                    .setValue("COMPLETED")
                    .addOnSuccessListener {
                        sendCompletionAlertToFamily(trip)
                        hideMyActiveTrip()
                    }

                Toast.makeText(this@MapActivity, "✅ Ruta desactivada y finalizada con éxito", Toast.LENGTH_LONG).show()
                val homeIntent = Intent(this@MapActivity, HomeActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(homeIntent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendCompletionAlertToFamily(trip: ActiveTrip) {
        val alertId = firestore.collection("route_alerts").document().id
        val alert = RouteAlert(
            alertId = alertId,
            tripId = trip.tripId,
            userId = userId,
            userName = userName,
            routeName = trip.routeName,
            type = "TRIP_COMPLETED",
            message = "✅ $userName ha concluido la ruta '${trip.routeName}' y llegó seguro a su destino.",
            latitude = currentLocation?.latitude ?: 0.0,
            longitude = currentLocation?.longitude ?: 0.0,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("route_alerts").document(alertId).set(alert)
        realtimeDatabase.getReference("route_alerts/$userId").setValue(alert)
    }

    private fun listenToContactsActiveTrips() {
        if (userId.isEmpty() || userId.startsWith("guest_")) return

        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val contactId = doc.getString("connectedUserId") ?: continue
                    val contactName = doc.getString("connectedUserName") ?: "Contacto"

                    val listener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                contactPolylines[contactId]?.remove()
                                contactPolylines.remove(contactId)
                                return
                            }

                            val trip = try {
                                snapshot.getValue(ActiveTrip::class.java)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parseando viaje de contacto", e)
                                null
                            } ?: return

                            if (trip.status == "IN_PROGRESS" || trip.status == "DEVIATED") {
                                drawContactRoutePolyline(contactId, trip)

                                if (trip.status == "DEVIATED" && !notifiedDeviations.contains(contactId)) {
                                    notifiedDeviations.add(contactId)
                                    showContactDeviationDialog(contactName, trip.routeName)
                                }
                            } else {
                                contactPolylines[contactId]?.remove()
                                contactPolylines.remove(contactId)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                    contactTripListeners[contactId] = listener
                    realtimeDatabase.getReference("active_trips/$contactId").addValueEventListener(listener)
                }
            }
    }

    private fun drawContactRoutePolyline(contactId: String, trip: ActiveTrip) {
        if (!::mMap.isInitialized) return
        contactPolylines[contactId]?.remove()

        if (trip.waypoints.size >= 2) {
            val latLngs = trip.waypoints.map { LatLng(it.latitude, it.longitude) }
            val color = if (trip.status == "DEVIATED") Color.RED else Color.MAGENTA

            val polyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(10f)
                    .color(color)
                    .geodesic(true)
            )

            contactPolylines[contactId] = polyline
        }
    }

    private fun showContactDeviationDialog(contactName: String, routeName: String) {
        AlertDialog.Builder(this)
            .setTitle("🚨 ¡ALERTA DE DESVIACIÓN DE CONTACTO!")
            .setMessage("⚠️ Tu contacto $contactName se ha desviado de su ruta '$routeName'. Por favor comunícate con él o revisa su posición en el mapa.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateMyLocationOnMap(location: Location) {
        if (!::mMap.isInitialized) return
        val myRawLatLng = LatLng(location.latitude, location.longitude)
        contactLocationsMap[userId] = myRawLatLng

        // Aplicar dispersión radial incluyendo la propia ubicación para evitar traslapes
        val offsetMap = MarkerOffsetHelper.applyOffsetToOverlappingLocations(contactLocationsMap)
        val myAdjustedLatLng = offsetMap[userId] ?: myRawLatLng

        CustomMapMarkerHelper.createCustomMarker(
            context = this,
            name = userName,
            photoUrl = myPhotoUrl,
            isFamily = false,
            isEmergency = false,
            isSelf = true
        ) { markerIcon ->
            if (myLocationMarker == null) {
                myLocationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(myAdjustedLatLng)
                        .title("Tu ubicación: $userName")
                        .icon(markerIcon)
                )

                if (focusContactId == null) {
                    centerMapOnLocation(location)
                }
            } else {
                myLocationMarker?.position = myAdjustedLatLng
                myLocationMarker?.setIcon(markerIcon)
            }
        }
    }

    private fun centerMapOnLocation(location: Location) {
        if (!::mMap.isInitialized) return
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }

    private fun loadContactsLocations() {
        if (userId.isEmpty() || userId.startsWith("guest_")) return

        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                allAcceptedContactsList.clear()

                if (documents.isEmpty()) {
                    Toast.makeText(this@MapActivity, "📍 Mostrando tu ubicación actual. Agrega contactos para ver sus posiciones.", Toast.LENGTH_SHORT).show()
                    filterFloatingContacts("Familia")
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val connection = document.toObject(UserConnection::class.java)
                    val connectedUserId = connection.connectedUserId

                    firestore.collection("users").document(connectedUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val photo = userDoc.getString("photoUrl") ?: ""
                            val updatedConnection = connection.copy(photoUrl = photo)
                            allAcceptedContactsList.add(updatedConnection)
                            filterFloatingContacts(spinnerGroupFilter.selectedItem?.toString() ?: "Familia")

                            if (focusContactId == null || connectedUserId == focusContactId) {
                                listenToContactLocation(connectedUserId, updatedConnection.connectedUserName, updatedConnection.type)
                            }
                        }
                        .addOnFailureListener {
                            allAcceptedContactsList.add(connection)
                            filterFloatingContacts(spinnerGroupFilter.selectedItem?.toString() ?: "Familia")

                            if (focusContactId == null || connectedUserId == focusContactId) {
                                listenToContactLocation(connectedUserId, connection.connectedUserName, connection.type)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error cargando contactos", e)
            }
    }

    private fun listenToContactLocation(contactId: String, contactName: String, type: String) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val latitude = try { snapshot.child("latitude").getValue(Double::class.java) } catch (e: Exception) { null }
                val longitude = try { snapshot.child("longitude").getValue(Double::class.java) } catch (e: Exception) { null }
                val isEmergency = try { snapshot.child("isEmergency").getValue(Boolean::class.java) } catch (e: Exception) { false } ?: false

                if (latitude != null && longitude != null) {
                    val rawLatLng = LatLng(latitude, longitude)
                    contactLocationsMap[contactId] = rawLatLng

                    // Asegurar que mi propia ubicación esté en el mapa para dispersión combinada
                    currentLocation?.let {
                        contactLocationsMap[userId] = LatLng(it.latitude, it.longitude)
                    }

                    // Aplicar dispersión radial considerando usuario + contactos
                    val offsetMap = MarkerOffsetHelper.applyOffsetToOverlappingLocations(contactLocationsMap)

                    offsetMap[userId]?.let { myAdjusted ->
                        myLocationMarker?.position = myAdjusted
                    }

                    val adjustedLatLng = offsetMap[contactId] ?: rawLatLng

                    updateContactMarker(contactId, contactName, adjustedLatLng.latitude, adjustedLatLng.longitude, type, isEmergency)

                    if (contactId == focusContactId && ::mMap.isInitialized) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(adjustedLatLng, 16f))
                        contactMarkers[contactId]?.showInfoWindow()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        contactLocationListeners[contactId] = listener
        realtimeDatabase.getReference("locations/$contactId").addValueEventListener(listener)
    }

    private fun updateContactMarker(
        contactId: String,
        contactName: String,
        latitude: Double,
        longitude: Double,
        type: String,
        isEmergency: Boolean
    ) {
        if (!::mMap.isInitialized) return
        val latLng = LatLng(latitude, longitude)

        val contactObj = allAcceptedContactsList.find { it.connectedUserId == contactId }
        val photoUrl = contactObj?.photoUrl
        val isFamily = type.equals("familia", ignoreCase = true) || type.equals("family", ignoreCase = true)

        CustomMapMarkerHelper.createCustomMarker(
            context = this,
            name = contactName,
            photoUrl = photoUrl,
            isFamily = isFamily,
            isEmergency = isEmergency,
            isSelf = false
        ) { markerIcon ->
            if (contactMarkers.containsKey(contactId)) {
                contactMarkers[contactId]?.apply {
                    position = latLng
                    setIcon(markerIcon)
                }
            } else {
                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("📍 $contactName")
                        .icon(markerIcon)
                )

                if (marker != null) {
                    contactMarkers[contactId] = marker
                }
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
                    Toast.makeText(this, "❌ Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        for ((contactId, listener) in contactLocationListeners) {
            realtimeDatabase.getReference("locations/$contactId").removeEventListener(listener)
        }
        for ((contactId, listener) in contactTripListeners) {
            realtimeDatabase.getReference("active_trips/$contactId").removeEventListener(listener)
        }
        contactLocationListeners.clear()
        contactTripListeners.clear()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
