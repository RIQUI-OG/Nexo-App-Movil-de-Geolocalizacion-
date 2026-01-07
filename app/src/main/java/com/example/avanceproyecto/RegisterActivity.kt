package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import com.example.avanceproyecto.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.app.ProgressDialog

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        progressDialog = ProgressDialog(this).apply {
            setMessage("Creando cuenta...")
            setCancelable(false)
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmailRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnRegisterConfirm = findViewById<Button>(R.id.btnRegisterConfirm)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        btnRegisterConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (validateInputs(name, email, password, phone)) {
                registerUser(name, email, password, phone)
            }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, password: String, phone: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor ingresa un correo válido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        if (phone.isEmpty() || phone.length < 10) {
            Toast.makeText(this, "Por favor ingresa un teléfono válido", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String, phone: String) {
        progressDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    val user = User(
                        userId = userId,
                        email = email,
                        name = name,
                        phone = phone,
                        photoUrl = "",
                        fcmToken = "",
                        createdAt = Timestamp.now()
                    )

                    saveUserToFirestore(user)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Error al crear cuenta: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(user: User) {
        firestore.collection("users")
            .document(user.userId)
            .set(user)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "✅ Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("USER_NAME", user.name)
                intent.putExtra("USER_ID", user.userId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Error al guardar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}