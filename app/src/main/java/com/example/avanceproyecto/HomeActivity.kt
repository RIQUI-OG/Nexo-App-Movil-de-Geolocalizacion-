package com.example.avanceproyecto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.DialogInterface
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class HomeActivity : AppCompatActivity() {

    // ID único para el canal de notificaciones
    private val CHANNEL_ID = "nexo_alerts_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"


        val tvWelcomeTitle = findViewById<TextView>(R.id.tvWelcomeTitle)
        tvWelcomeTitle.text = "¡BIENVENIDO(A), $userName!"

        val btnSendAlert = findViewById<Button>(R.id.btnSendAlert)
        btnSendAlert.setOnClickListener {
            sendFamilyAlert(userName)
        }

        createNotificationChannel()
    }

    private fun sendFamilyAlert(userName: String) {
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

    // Función simulada para obtener ubicación (en un proyecto real usarías GPS)
    private fun getSimulatedLocation(): String {
        val locations = listOf(
            "Casa - Zona Residencial Norte",
            "Trabajo - Centro Comercial Plaza",
            "Parque Central",
            "Avenida Principal 123",
            "Zona Universitaria"
        )
        return locations.random()
    }

    override fun onBackPressed() {
        showExitDialog()
    }

    private fun showExitDialog() {
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

    // Manejar la respuesta de los permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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