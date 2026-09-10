package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Verificar si la sesión ya está iniciada
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkAndRedirectToHome(currentUser.uid)
            return
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

    private fun checkAndRedirectToHome(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val userName = if (document.exists()) {
                    document.getString("name") ?: "Usuario"
                } else {
                    "Usuario"
                }

                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("USER_NAME", userName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("USER_NAME", "Usuario")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
    }
}
