package com.example.avanceproyecto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SoporteActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnContactSupportEmail: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var userId: String = ""
    private var userName: String = "Usuario"
    private var myPhotoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soporte)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        initViews()
        setupNavigationDrawer()
        loadUserData()
    }

    private fun initViews() {
        btnContactSupportEmail = findViewById(R.id.btnContactSupportEmail)

        btnContactSupportEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:seguridadfamilarnexo@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Soporte Nexo Seguridad")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se encontró aplicación de correo electrónico", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData() {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userName = doc.getString("name") ?: "Usuario"
                        myPhotoUrl = doc.getString("photoUrl") ?: ""

                        val navView: NavigationView = findViewById(R.id.nav_view_soporte)
                        val headerView = navView.getHeaderView(0)
                        val tvProfileHeader = headerView?.findViewById<TextView>(R.id.tv_profile_header)
                        tvProfileHeader?.text = userName

                        val imgProfileHeader = headerView?.findViewById<ImageView>(R.id.img_profile_header)
                        if (myPhotoUrl.isNotEmpty() && imgProfileHeader != null) {
                            Glide.with(this)
                                .load(myPhotoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .into(imgProfileHeader)
                        }
                    }
                }
        }
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout_soporte)
        val navView: NavigationView = findViewById(R.id.nav_view_soporte)
        val toolbar: Toolbar = findViewById(R.id.toolbarSoporte)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Soporte y Ayuda"

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
                val intent = Intent(this, RoutesActivity::class.java)
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
