package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLoginConfirm = findViewById<Button>(R.id.btnLoginConfirm)

        btnLoginConfirm.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

               val name = extractNameFromEmail(email) ?: "Usuario"


                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("USER_NAME", name)
                startActivity(intent)

            } else {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractNameFromEmail(email: String): String? {
        return try {
            val namePart = email.substringBefore("@")
            namePart.replaceFirstChar { it.uppercase() }  // Primera letra mayúscula
        } catch (e: Exception) {
            null
        }
    }
}