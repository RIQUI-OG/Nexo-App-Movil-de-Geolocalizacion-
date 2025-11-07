package com.example.avanceproyecto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmailRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnRegisterConfirm = findViewById<Button>(R.id.btnRegisterConfirm)

        btnRegisterConfirm.setOnClickListener {
            if (etName.text.isNotEmpty() && etEmail.text.isNotEmpty() &&
                etPassword.text.isNotEmpty() && etPhone.text.isNotEmpty()) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
