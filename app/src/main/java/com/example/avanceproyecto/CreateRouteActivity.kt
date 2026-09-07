package com.example.avanceproyecto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class CreateRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var etRouteName: TextInputEditText
    private lateinit var etStartLocation: TextInputEditText
    private lateinit var etEndLocation: TextInputEditText
    private lateinit var btnSaveRoute: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_route)

        setupToolbar()
        initViews()
        setupMap()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbarCreateRoute)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear Nueva Ruta"
    }

    private fun initViews() {
        etRouteName = findViewById(R.id.etRouteName)
        etStartLocation = findViewById(R.id.etStartLocation)
        etEndLocation = findViewById(R.id.etEndLocation)
        btnSaveRoute = findViewById(R.id.btnSaveRoute)

        btnSaveRoute.setOnClickListener {
            val routeName = etRouteName.text.toString().trim()
            val start = etStartLocation.text.toString().trim()
            val end = etEndLocation.text.toString().trim()

            if (routeName.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos para guardar la ruta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Ruta '$routeName' guardada localmente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.createRouteMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLocation = LatLng(19.432608, -99.133209) // Ciudad de México por defecto
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
