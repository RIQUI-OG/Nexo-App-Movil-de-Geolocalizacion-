package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.adapters.RoutesAdapter
import com.example.avanceproyecto.models.ActiveTrip
import com.example.avanceproyecto.models.ChatMessage
import com.example.avanceproyecto.models.Route
import com.example.avanceproyecto.models.RouteAlert
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class RoutesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerViewRoutes: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var fabAddRoute: FloatingActionButton

    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var userName: String = "Usuario"
    private val routesList = mutableListOf<Route>()
    private lateinit var adapter: RoutesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routes)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        setupNavigationDrawer()
        initViews()
        loadUserName()
        loadRoutesFromFirestore()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Rutas"

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
        val imgProfileHeader = headerView.findViewById<ImageView>(R.id.img_profile_header)
        imgProfileHeader?.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun initViews() {
        recyclerViewRoutes = findViewById(R.id.recyclerViewRoutes)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        fabAddRoute = findViewById(R.id.fabAddRoute)

        recyclerViewRoutes.layoutManager = LinearLayoutManager(this)

        adapter = RoutesAdapter(
            routesList,
            onStartClick = { route -> startRouteTrip(route) },
            onDeleteClick = { route -> confirmDeleteRoute(route) }
        )

        recyclerViewRoutes.adapter = adapter

        fabAddRoute.setOnClickListener {
            val intent = Intent(this, CreateRouteActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
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

    private fun loadRoutesFromFirestore() {
        if (userId.isEmpty()) return

        firestore.collection("routes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                routesList.clear()
                for (doc in documents) {
                    val route = doc.toObject(Route::class.java)
                    routesList.add(route)
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar rutas: ${e.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
    }

    private fun updateEmptyState() {
        if (routesList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerViewRoutes.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerViewRoutes.visibility = View.VISIBLE
        }
    }

    private fun startRouteTrip(route: Route) {
        AlertDialog.Builder(this)
            .setTitle("▶️ Iniciar Ruta")
            .setMessage("¿Deseas iniciar la ruta '${route.routeName}'?\n\nTus familiares recibirán una notificación y un mensaje en el Chat Familiar.")
            .setPositiveButton("Sí, Iniciar") { dialog, _ ->
                dialog.dismiss()

                val tripId = "trip_$userId"
                val activeTrip = ActiveTrip(
                    tripId = tripId,
                    userId = userId,
                    userName = userName,
                    routeId = route.routeId,
                    routeName = route.routeName,
                    transportMode = route.transportMode,
                    status = "IN_PROGRESS",
                    currentLat = route.startLat,
                    currentLng = route.startLng,
                    waypoints = route.waypoints,
                    deviationAlertSent = false,
                    startTime = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )

                // Guardar viaje activo en Firebase Realtime Database
                realtimeDatabase.getReference("active_trips/$userId")
                    .setValue(activeTrip)
                    .addOnSuccessListener {
                        sendNotificationToFamily(route)
                        sendFamilyGroupSystemMessage(route)

                        Toast.makeText(this, "🚀 ¡Ruta '${route.routeName}' iniciada!", Toast.LENGTH_LONG).show()

                        val intent = Intent(this, MapActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            putExtra("ACTIVE_TRIP_ID", tripId)
                            putExtra("ACTIVE_ROUTE_NAME", route.routeName)
                        }
                        startActivity(intent)
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendNotificationToFamily(route: Route) {
        val alertId = firestore.collection("route_alerts").document().id
        val alert = RouteAlert(
            alertId = alertId,
            tripId = "trip_$userId",
            userId = userId,
            userName = userName,
            routeName = route.routeName,
            type = "TRIP_STARTED",
            message = "🚀 $userName ha iniciado la ruta '${route.routeName}' (${route.transportMode})",
            latitude = route.startLat,
            longitude = route.startLng,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("route_alerts")
            .document(alertId)
            .set(alert)

        realtimeDatabase.getReference("route_alerts/$userId").setValue(alert)
    }

    private fun sendFamilyGroupSystemMessage(route: Route) {
        if (userId.isEmpty()) return

        val familyGroupId = "group_family_$userId"
        val msgRef = realtimeDatabase.getReference("chat_messages/$familyGroupId").push()
        val msgId = msgRef.key ?: System.currentTimeMillis().toString()

        val systemMessage = ChatMessage(
            messageId = msgId,
            chatId = familyGroupId,
            senderId = "SYSTEM_ROUTE",
            senderName = "🤖 Sistema Nexo",
            text = "🚀 $userName ha iniciado la ruta '${route.routeName}' (${route.transportMode}). Sigue su ubicación en tiempo real en el mapa.",
            timestamp = System.currentTimeMillis()
        )

        msgRef.setValue(systemMessage)

        val conversationUpdate = mapOf(
            "chatId" to familyGroupId,
            "lastMessage" to "🚀 $userName inició la ruta '${route.routeName}'",
            "lastMessageTime" to System.currentTimeMillis(),
            "title" to "👨‍👩‍👧‍👦 Chat Familiar",
            "isGroup" to true
        )

        realtimeDatabase.getReference("chat_conversations/$familyGroupId")
            .updateChildren(conversationUpdate)
    }

    private fun confirmDeleteRoute(route: Route) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Ruta")
            .setMessage("¿Estás seguro de eliminar la ruta '${route.routeName}'?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                firestore.collection("routes").document(route.routeId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ruta eliminada", Toast.LENGTH_SHORT).show()
                        loadRoutesFromFirestore()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadRoutesFromFirestore()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                finish()
            }
            R.id.nav_family_location -> {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_routes -> {
                Toast.makeText(this, "Ya estás en Rutas", Toast.LENGTH_SHORT).show()
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
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
