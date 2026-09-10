package com.example.avanceproyecto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.adapters.RouteOptionsAdapter
import com.example.avanceproyecto.models.Route
import com.example.avanceproyecto.models.RoutePoint
import com.example.avanceproyecto.utils.GoogleDirectionsHelper
import com.example.avanceproyecto.utils.RouteOption
import com.google.android.gms.location.FusedLocationProviderClient
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
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var etRouteName: TextInputEditText
    private lateinit var chipUseCurrentLocation: Chip
    private lateinit var etOrigin: AutoCompleteTextView
    private lateinit var etDestination: AutoCompleteTextView
    private lateinit var btnSwapLocations: ImageView
    private lateinit var rgTransportMode: RadioGroup
    private lateinit var btnCalculateRoute: MaterialButton
    private lateinit var rvRouteOptions: RecyclerView
    private lateinit var tvRouteInfo: TextView
    private lateinit var btnClearPoints: MaterialButton
    private lateinit var btnSaveRoute: MaterialButton

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userId: String = ""

    private val waypointsList = mutableListOf<LatLng>()
    private val markerList = mutableListOf<Marker>()
    private val badgeMarkerMap = mutableMapOf<Int, Marker>()
    private val polylineMap = mutableMapOf<Int, Polyline>()

    private var availableRouteOptions = mutableListOf<RouteOption>()
    private var selectedRouteOption: RouteOption? = null
    private var optionsAdapter: RouteOptionsAdapter? = null

    private var currentOriginLatLng: LatLng? = null
    private var startAddressName: String = ""
    private var endAddressName: String = ""

    companion object {
        private const val LOCATION_PERMISSION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_route)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        setupToolbar()
        initViews()
        setupMap()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbarCreateRoute)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear Ruta - Google Maps"
    }

    private fun initViews() {
        etRouteName = findViewById(R.id.etRouteName)
        chipUseCurrentLocation = findViewById(R.id.chipUseCurrentLocation)
        etOrigin = findViewById(R.id.etOrigin)
        etDestination = findViewById(R.id.etDestination)
        btnSwapLocations = findViewById(R.id.btnSwapLocations)
        rgTransportMode = findViewById(R.id.rgTransportMode)
        btnCalculateRoute = findViewById(R.id.btnCalculateRoute)
        rvRouteOptions = findViewById(R.id.rvRouteOptions)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        btnClearPoints = findViewById(R.id.btnClearPoints)
        btnSaveRoute = findViewById(R.id.btnSaveRoute)

        rvRouteOptions.layoutManager = LinearLayoutManager(this)

        chipUseCurrentLocation.setOnClickListener {
            setOriginToCurrentLocation()
        }

        btnSwapLocations.setOnClickListener {
            val temp = etOrigin.text.toString()
            etOrigin.setText(etDestination.text.toString())
            etDestination.setText(temp)
        }

        btnCalculateRoute.setOnClickListener {
            calculateGoogleRoute()
        }

        btnClearPoints.setOnClickListener {
            clearMapPoints()
        }

        btnSaveRoute.setOnClickListener {
            saveRouteToFirestore()
        }
    }

    private fun setOriginToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val myLatLng = LatLng(location.latitude, location.longitude)
                    currentOriginLatLng = myLatLng
                    startAddressName = "Mi ubicación actual"
                    etOrigin.setText("📍 Mi ubicación actual")

                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f))
                    Toast.makeText(this, "📍 Origen fijado en tu ubicación GPS actual", Toast.LENGTH_SHORT).show()

                    val destStr = etDestination.text.toString().trim()
                    if (destStr.isNotEmpty()) {
                        calculateGoogleRoute()
                    }
                } else {
                    Toast.makeText(this, "⚠️ No se pudo obtener la ubicación GPS actual. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.createRouteMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap?.uiSettings?.isZoomControlsEnabled = true
        mMap?.uiSettings?.isCompassEnabled = true

        val defaultLocation = LatLng(19.432608, -99.133209) // CDMX
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        mMap?.setOnMapClickListener { latLng ->
            addPointFromMapClick(latLng)
        }

        mMap?.setOnPolylineClickListener { polyline ->
            val optionId = polyline.tag as? Int ?: return@setOnPolylineClickListener
            selectRouteOption(optionId)
        }

        mMap?.setOnMarkerClickListener { marker ->
            val optionId = marker.tag as? Int
            if (optionId != null) {
                selectRouteOption(optionId)
                true
            } else {
                false
            }
        }
    }

    private fun addPointFromMapClick(latLng: LatLng) {
        if (waypointsList.size >= 2) {
            clearMapPoints()
        }

        waypointsList.add(latLng)

        val markerTitle = if (waypointsList.size == 1) "📍 Inicio" else "🏁 Destino"
        val markerHue = if (waypointsList.size == 1) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED

        val marker = mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(markerTitle)
                .icon(BitmapDescriptorFactory.defaultMarker(markerHue))
        )

        marker?.let { markerList.add(it) }

        if (waypointsList.size == 1) {
            etOrigin.setText("${latLng.latitude}, ${latLng.longitude}")
            currentOriginLatLng = latLng
            tvRouteInfo.text = "📍 Punto de partida seleccionado. Ahora toca el destino."
        } else if (waypointsList.size == 2) {
            etDestination.setText("${latLng.latitude}, ${latLng.longitude}")
            calculateRouteFromPoints(waypointsList[0], waypointsList[1])
        }
    }

    private fun calculateGoogleRoute() {
        val originStr = etOrigin.text.toString().trim()
        val destStr = etDestination.text.toString().trim()

        if (originStr.isEmpty() || destStr.isEmpty()) {
            Toast.makeText(this, "Escribe el origen y destino (o usa tu ubicación actual)", Toast.LENGTH_SHORT).show()
            return
        }

        tvRouteInfo.text = "⏳ Buscando todas las alternativas de transporte..."

        val originLatLng = if (originStr.contains("ubicación actual", ignoreCase = true) && currentOriginLatLng != null) {
            currentOriginLatLng
        } else {
            GoogleDirectionsHelper.geocodeAddress(this, originStr)
        }

        val destLatLng = GoogleDirectionsHelper.geocodeAddress(this, destStr)

        if (originLatLng != null && destLatLng != null) {
            calculateRouteFromPoints(originLatLng, destLatLng)
        } else {
            Toast.makeText(this, "No se encontraron las direcciones. Intenta ser más específico.", Toast.LENGTH_LONG).show()
            tvRouteInfo.text = "⚠️ No se pudo geocodificar la dirección. Usa el mapa o tu ubicación actual."
        }
    }

    private fun calculateRouteFromPoints(origin: LatLng, destination: LatLng) {
        val transportMode = getSelectedTransportMode()
        val apiKey = getApiKeyFromManifest()

        GoogleDirectionsHelper.fetchRoute(
            origin = origin,
            destination = destination,
            transportMode = transportMode,
            apiKey = apiKey,
            callback = object : GoogleDirectionsHelper.DirectionsCallback {
                override fun onSuccess(routeOptions: List<RouteOption>) {
                    availableRouteOptions.clear()
                    availableRouteOptions.addAll(routeOptions)

                    if (routeOptions.isNotEmpty()) {
                        selectedRouteOption = routeOptions[0]
                        if (startAddressName.isEmpty()) {
                            startAddressName = routeOptions[0].startAddress
                        }
                        endAddressName = routeOptions[0].endAddress

                        setupRouteOptionsList()
                        drawAllRouteOptionsOnMap(origin, destination)

                        tvRouteInfo.text = "🛣️ Se encontraron ${routeOptions.size} opción(es) de ruta. Toca una para seleccionarla."
                        Toast.makeText(this@CreateRouteActivity, "✅ ${routeOptions.size} opciones disponibles", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(errorMessage: String) {
                    tvRouteInfo.text = "⚠️ Ruta directa trazada."
                }
            }
        )
    }

    private fun setupRouteOptionsList() {
        optionsAdapter = RouteOptionsAdapter(availableRouteOptions) { option ->
            selectRouteOption(option.id)
        }

        rvRouteOptions.adapter = optionsAdapter
        rvRouteOptions.visibility = View.VISIBLE
    }

    private fun selectRouteOption(optionId: Int) {
        val option = availableRouteOptions.find { it.id == optionId } ?: return
        selectedRouteOption = option

        optionsAdapter?.setSelectedPosition(optionId)

        for ((id, polyline) in polylineMap) {
            if (id == optionId) {
                polyline.color = Color.parseColor("#156082")
                polyline.width = 14f
                polyline.zIndex = 2f
            } else {
                polyline.color = Color.parseColor("#78909C")
                polyline.width = 10f
                polyline.zIndex = 1f
            }
        }

        val detailsText = if (option.departureTime.isNotEmpty()) {
            "📍 Opción Elegida: ${option.departureTime} - ${option.arrivalTime} • ⏱️ ${option.durationText}"
        } else {
            "📍 Opción Elegida: ${option.summary} • ⏱️ ${option.durationText}"
        }

        tvRouteInfo.text = detailsText
    }

    private fun drawAllRouteOptionsOnMap(origin: LatLng, destination: LatLng) {
        mMap?.clear()
        markerList.clear()
        polylineMap.clear()
        badgeMarkerMap.clear()

        val startMarker = mMap?.addMarker(
            MarkerOptions()
                .position(origin)
                .title("📍 Partida")
                .snippet(startAddressName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        val endMarker = mMap?.addMarker(
            MarkerOptions()
                .position(destination)
                .title("🏁 Destino")
                .snippet(endAddressName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        startMarker?.let { markerList.add(it) }
        endMarker?.let { markerList.add(it) }

        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(origin)
        boundsBuilder.include(destination)

        for (option in availableRouteOptions) {
            val latLngs = option.waypoints.map { LatLng(it.latitude, it.longitude) }
            for (p in latLngs) {
                boundsBuilder.include(p)
            }

            val isSelected = option.id == (selectedRouteOption?.id ?: 0)
            val polylineColor = if (isSelected) Color.parseColor("#156082") else Color.parseColor("#78909C")
            val polylineWidth = if (isSelected) 14f else 10f
            val polylineZIndex = if (isSelected) 2f else 1f

            val polyline = mMap?.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(polylineWidth)
                    .color(polylineColor)
                    .zIndex(polylineZIndex)
                    .clickable(true)
                    .geodesic(true)
            )

            if (polyline != null) {
                polyline.tag = option.id
                polylineMap[option.id] = polyline
            }

            if (latLngs.size > 2) {
                val midIndex = latLngs.size / 2
                val midLatLng = latLngs[midIndex]

                val badgeTitle = "🚌 ${option.durationText}"
                val badgeMarker = mMap?.addMarker(
                    MarkerOptions()
                        .position(midLatLng)
                        .title(badgeTitle)
                        .snippet(option.summary)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                            if (isSelected) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_VIOLET
                        ))
                )

                if (badgeMarker != null) {
                    badgeMarker.tag = option.id
                    badgeMarkerMap[option.id] = badgeMarker
                }
            }
        }

        try {
            mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        } catch (e: Exception) {
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 13f))
        }
    }

    private fun clearMapPoints() {
        waypointsList.clear()
        for (m in markerList) {
            m.remove()
        }
        markerList.clear()
        polylineMap.clear()
        badgeMarkerMap.clear()
        availableRouteOptions.clear()
        selectedRouteOption = null
        currentOriginLatLng = null
        startAddressName = ""
        rvRouteOptions.visibility = View.GONE
        mMap?.clear()
        etOrigin.setText("")
        etDestination.setText("")
        tvRouteInfo.text = "👉 Escribe origen y destino (o usa tu ubicación actual)"
        Toast.makeText(this, "Mapa limpiado", Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedTransportMode(): String {
        val selectedId = rgTransportMode.checkedRadioButtonId
        val radioButton = findViewById<RadioButton>(selectedId)
        val text = radioButton?.text?.toString() ?: "Transporte"

        return when {
            text.contains("Auto", ignoreCase = true) -> "Auto"
            text.contains("Caminando", ignoreCase = true) -> "Caminando"
            text.contains("Bicicleta", ignoreCase = true) -> "Bicicleta"
            else -> "Transporte Público"
        }
    }

    private fun getApiKeyFromManifest(): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveRouteToFirestore() {
        val routeName = etRouteName.text.toString().trim()
        val transportMode = getSelectedTransportMode()

        if (routeName.isEmpty()) {
            etRouteName.error = "Ingresa un nombre para la ruta"
            return
        }

        val chosenOption = selectedRouteOption
        if (chosenOption == null || chosenOption.waypoints.isEmpty()) {
            Toast.makeText(this, "⚠️ Debes calcular y seleccionar una opción de ruta antes de guardar", Toast.LENGTH_LONG).show()
            return
        }

        btnSaveRoute.isEnabled = false

        val routeId = firestore.collection("routes").document().id
        val startPoint = chosenOption.waypoints.first()
        val endPoint = chosenOption.waypoints.last()

        val startName = if (startAddressName.isNotEmpty()) startAddressName else if (chosenOption.startAddress.isNotEmpty()) chosenOption.startAddress else "Partida"
        val endName = if (chosenOption.endAddress.isNotEmpty()) chosenOption.endAddress else "Destino"

        val route = Route(
            routeId = routeId,
            userId = userId,
            routeName = "$routeName (${chosenOption.summary})",
            transportMode = transportMode,
            startName = startName,
            startLat = startPoint.latitude,
            startLng = startPoint.longitude,
            endName = endName,
            endLat = endPoint.latitude,
            endLng = endPoint.longitude,
            waypoints = chosenOption.waypoints,
            createdAt = System.currentTimeMillis()
        )

        firestore.collection("routes")
            .document(routeId)
            .set(route)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Ruta '${route.routeName}' guardada con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSaveRoute.isEnabled = true
                Toast.makeText(this, "❌ Error al guardar la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setOriginToCurrentLocation()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
