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



class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"

        val tvWelcomeTitle = findViewById<TextView>(R.id.tvWelcomeTitle)
        tvWelcomeTitle.text = "¡BIENVENIDO(A), $userName!"

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

            Toast.makeText(
                this,
                "¡Hasta pronto!",
                Toast.LENGTH_SHORT
            ).show()
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
}
