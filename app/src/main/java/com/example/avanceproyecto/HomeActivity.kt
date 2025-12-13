package com.example.avanceproyecto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
// IMPORTACIONES NECESARIAS PARA EL MENU
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
// FIN DE IMPORTACIONES PARA EL MENU

import android.content.DialogInterface
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat // Necesario para cerrar el Drawer

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener { // Implementar Listener

    private lateinit var drawerLayout: DrawerLayout

    // ID único para el canal de notificaciones
    private val CHANNEL_ID = "nexo_alerts_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Configurar la Toolbar como ActionBar de la Actividad
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Puedes establecer un título si lo deseas
        supportActionBar?.title = "Nexo Seguridad"


        // 2. Enlazar el DrawerLayout y el NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this) // Establecer el listener para los clics en el menú

        // 3. Configurar el Toggle (el ícono de 3 barras)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open, // Necesitas estos strings en res/values/strings.xml
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
        val tvWelcomeTitle = findViewById<TextView>(R.id.tvWelcomeTitle)
        tvWelcomeTitle.text = "¡BIENVENIDO(A), $userName!"

        val btnSendAlert = findViewById<Button>(R.id.btnSendAlert)
        btnSendAlert.setOnClickListener {
            sendFamilyAlert(userName)
        }

        val btnOpenMap = findViewById<Button>(R.id.btnOpenMap)
        btnOpenMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        val btnScheduleTrip = findViewById<Button>(R.id.btnScheduleTrip)
        btnScheduleTrip.setOnClickListener {
            startActivity(Intent(this, TripScheduleActivity::class.java))
        }

        createNotificationChannel()
    }

    // Implementación del método de la interfaz NavigationView.OnNavigationItemSelectedListener
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Manejar la navegación aquí
        when (item.itemId) {
            R.id.nav_home -> {
                // Ya estamos en Home, no hacer nada o simplemente cerrar el drawer
                Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
            }




            R.id.nav_schedule_trip -> {
                startActivity(Intent(this, TripScheduleActivity::class.java))
            }
            R.id.nav_family_location -> {
                // TODO: Iniciar la actividad de Ubicación Familiar
                Toast.makeText(this, "Abrir Ubicación Familiar", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                // TODO: Iniciar la actividad de Configuración
                Toast.makeText(this, "Abrir Configuración", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                // TODO: Lógica de Cerrar Sesión (ej. ir a la pantalla de Login)
                Toast.makeText(this, "Cerrar Sesión", Toast.LENGTH_SHORT).show()
            }
        }

        // Cierra el cajón después de la selección
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Modificar onBackPressed para cerrar el Navigation Drawer si está abierto
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            showExitDialog() // Reutilizamos tu lógica de salir
        }
    }

    // [Mantener tus otras funciones (sendFamilyAlert, createNotificationChannel, etc.) aquí...]
    private fun sendFamilyAlert(userName: String) {
        // [CÓDIGO DE sendFamilyAlert AQUÍ...]
        // Verificar permisos para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Solicitar permiso
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                return
            }
        }

        // Crear y mostrar la notificación
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_nexo)  // Usa tu logo o un icono de alerta
            .setContentTitle("🚨 Alerta de Seguridad Enviada")
            .setContentText("$userName ha compartido su ubicación con la familia")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$userName ha activado una alerta de seguridad. Tu familia ha recibido tu ubicación actual y será notificada.\n\nUbicación compartida: ${getSimulatedLocation()}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }

        // Mostrar confirmación en la app
        Snackbar.make(
            findViewById(android.R.id.content),
            "✅ Alerta enviada a tu familia",
            Snackbar.LENGTH_LONG
        ).show()

        Toast.makeText(this, "Tu familia ha sido notificada con tu ubicación", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        // [CÓDIGO DE createNotificationChannel AQUÍ...]
        // Crear el canal solo para Android 8.0+
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

    private fun getSimulatedLocation(): String {
        // [CÓDIGO DE getSimulatedLocation AQUÍ...]
        val locations = listOf(
            "Casa - Zona Residencial Norte",
            "Trabajo - Centro Comercial Plaza",
            "Parque Central",
            "Avenida Principal 123",
            "Zona Universitaria"
        )
        return locations.random()
    }

    private fun showExitDialog() {
        // [CÓDIGO DE showExitDialog AQUÍ...]
        val exitAlert = AlertDialog.Builder(this)
        exitAlert.setTitle("Salir")
        exitAlert.setMessage("¿Seguro que quieres salir de la aplicación?")

        exitAlert.setPositiveButton("Sí") { dialogInterface: DialogInterface, _: Int ->
            finishAffinity()
            Toast.makeText(this, "¡Hasta pronto!", Toast.LENGTH_SHORT).show()
        }

        exitAlert.setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            Snackbar.make(
                findViewById(android.R.id.content),
                "Continuamos en la app",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        exitAlert.setCancelable(false)
        exitAlert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // [CÓDIGO DE onRequestPermissionsResult AQUÍ...]
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
                sendFamilyAlert(userName)
            } else {
                Toast.makeText(this, "Se necesitan permisos para enviar alertas", Toast.LENGTH_SHORT).show()
            }
        }
    }
}