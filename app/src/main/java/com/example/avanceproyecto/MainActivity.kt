package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Aplicar configuración de Modo Nocturno
            val sharedPrefs = getSharedPreferences("NexoSettings", MODE_PRIVATE)
            val isDarkModeOn = sharedPrefs.getBoolean("DARK_MODE", false)
            if (isDarkModeOn) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            auth = FirebaseAuth.getInstance()

            // Verificar si la sesión ya está iniciada
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("USER_ID", currentUser.uid)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
                return
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error durante inicialización de sesión", e)
        }

        setContentView(R.layout.activity_main)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
