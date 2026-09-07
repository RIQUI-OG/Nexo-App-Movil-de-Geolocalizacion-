package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class ConfiguracionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnParentalMode: MaterialButton
    private lateinit var btnDarkMode: MaterialButton
    private lateinit var btnAboutAlerts: MaterialButton
    private lateinit var btnSupportHelp: MaterialButton
    private lateinit var btnLogoutSettings: MaterialButton

    private lateinit var auth: FirebaseAuth
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        auth = FirebaseAuth.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        setupNavigationDrawer()
        initViews()
        setupListeners()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Configuración"

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
        btnParentalMode = findViewById(R.id.btnParentalMode)
        btnDarkMode = findViewById(R.id.btnDarkMode)
        btnAboutAlerts = findViewById(R.id.btnAboutAlerts)
        btnSupportHelp = findViewById(R.id.btnSupportHelp)
        btnLogoutSettings = findViewById(R.id.btnLogoutSettings)
    }

    private fun setupListeners() {
        btnParentalMode.setOnClickListener {
            Toast.makeText(this, "👨‍👩‍👧‍👦 Modo parental próximamente", Toast.LENGTH_SHORT).show()
        }

        btnDarkMode.setOnClickListener {
            Toast.makeText(this, "🌙 Modo nocturno próximamente", Toast.LENGTH_SHORT).show()
        }

        btnAboutAlerts.setOnClickListener {
            val intent = Intent(this, SobreAlertasActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnSupportHelp.setOnClickListener {
            Toast.makeText(this, "🎧 Soporte y Ayuda próximamente", Toast.LENGTH_SHORT).show()
        }

        btnLogoutSettings.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
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
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_contacts -> {
                val intent = Intent(this, ContactsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Ya estás en Configuración", Toast.LENGTH_SHORT).show()
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
