package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.avanceproyecto.utils.AdminReceiver
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

        updateDarkModeButtonText()
        updateParentalModeButtonText()
    }

    override fun onResume() {
        super.onResume()
        updateParentalModeButtonText()
    }

    private fun updateParentalModeButtonText() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            btnParentalMode.text = "🛡️ Desactivar Modo Parental"
        } else {
            btnParentalMode.text = "🛡️ Activar Modo Parental"
        }
    }

    private fun toggleParentalMode() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        val sharedPrefs = getSharedPreferences("NexoSettings", MODE_PRIVATE)

        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }
            val input1 = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                hint = "Ingresa un PIN de 4 dígitos"
            }
            val input2 = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                hint = "Confirma tu PIN"
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20 }
            }
            layout.addView(input1)
            layout.addView(input2)

            android.app.AlertDialog.Builder(this)
                .setTitle("Configurar PIN Parental")
                .setMessage("Crea un PIN numérico para evitar que este modo sea desactivado desde la app sin permiso.")
                .setView(layout)
                .setPositiveButton("Guardar y Activar") { _, _ ->
                    val pin1 = input1.text.toString()
                    val pin2 = input2.text.toString()
                    if (pin1.length >= 4) {
                        if (pin1 == pin2) {
                            sharedPrefs.edit().putString("PARENTAL_PIN", pin1).apply()
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "Nexo Modo Parental Fuerte:\n- Evita que la aplicación sea desinstalada por menores.\n- Protege el servicio de ubicación en segundo plano."
                                )
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Los PIN no coinciden, inténtalo de nuevo", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "El PIN debe tener al menos 4 dígitos", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            val input = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                hint = "Ingresa el PIN de seguridad"
            }
            android.app.AlertDialog.Builder(this)
                .setTitle("Desactivar Modo Parental")
                .setMessage("Ingresa el PIN de seguridad para desactivarlo.")
                .setView(input)
                .setPositiveButton("Desactivar") { _, _ ->
                    val pin = input.text.toString()
                    val savedPin = sharedPrefs.getString("PARENTAL_PIN", "")
                    if (pin == savedPin || savedPin.isNullOrEmpty()) {
                        devicePolicyManager.removeActiveAdmin(adminComponent)
                        sharedPrefs.edit().remove("PARENTAL_PIN").apply()
                        Toast.makeText(this, "Modo Parental Desactivado", Toast.LENGTH_SHORT).show()
                        updateParentalModeButtonText()
                    } else {
                        Toast.makeText(this, "PIN Incorrecto", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun toggleDarkMode() {
        val sharedPrefs = getSharedPreferences("NexoSettings", MODE_PRIVATE)
        val isDarkModeOn = sharedPrefs.getBoolean("DARK_MODE", false)
        
        val newMode = !isDarkModeOn
        sharedPrefs.edit().putBoolean("DARK_MODE", newMode).apply()
        
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        updateDarkModeButtonText()
    }
    
    private fun updateDarkModeButtonText() {
        val sharedPrefs = getSharedPreferences("NexoSettings", MODE_PRIVATE)
        val isDarkModeOn = sharedPrefs.getBoolean("DARK_MODE", false)
        
        if (isDarkModeOn) {
            btnDarkMode.text = "☀️ Desactivar Modo Nocturno"
        } else {
            btnDarkMode.text = "🌙 Activar Modo Nocturno"
        }
    }

    private fun setupListeners() {
        btnParentalMode.setOnClickListener {
            toggleParentalMode()
        }

        btnDarkMode.setOnClickListener {
            toggleDarkMode()
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
