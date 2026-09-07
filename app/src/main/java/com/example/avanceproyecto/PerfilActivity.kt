package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText

class PerfilActivity : AppCompatActivity() {

    private lateinit var btnMenuBack: ImageButton
    private lateinit var imgEditPhoto: ShapeableImageView
    private lateinit var etProfileName: TextInputEditText
    private lateinit var etProfileEmail: TextInputEditText
    private lateinit var etProfilePhone: TextInputEditText
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton
    private lateinit var tvLogout: TextView
    private lateinit var tvContactAdmin: TextView

    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnMenuBack = findViewById(R.id.btnMenuBack)
        imgEditPhoto = findViewById(R.id.btnEditPhoto)
        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        tvLogout = findViewById(R.id.tvLogout)
        tvContactAdmin = findViewById(R.id.tvContactAdmin)
    }

    private fun setupListeners() {
        // Botón de menú (Regresar)
        btnMenuBack.setOnClickListener {
            finish()
        }

        // Ícono de edición de foto de perfil
        imgEditPhoto.setOnClickListener {
            Toast.makeText(this, "Abrir galería de imágenes", Toast.LENGTH_SHORT).show()
        }

        // Botón de Editar / Guardar
        btnEditProfile.setOnClickListener {
            if (!isEditing) {
                // Modo Edición
                isEditing = true
                etProfileName.isEnabled = true
                etProfileEmail.isEnabled = true
                etProfilePhone.isEnabled = true
                btnEditProfile.text = "Guardar"
            } else {
                // Guardar Cambios
                val name = etProfileName.text.toString().trim()
                val email = etProfileEmail.text.toString().trim()
                val phone = etProfilePhone.text.toString().trim()

                if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isEditing = false
                etProfileName.isEnabled = false
                etProfileEmail.isEnabled = false
                etProfilePhone.isEnabled = false
                btnEditProfile.text = "Editar"

                Toast.makeText(this, "Datos actualizados localmente", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de Eliminar cuenta
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Cuenta")
                .setMessage("¿Estás seguro de eliminar tu cuenta? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Enlace Cerrar sesión
        tvLogout.setOnClickListener {
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // Enlace Contactar con el administrador
        tvContactAdmin.setOnClickListener {
            Toast.makeText(this, "Abriendo cliente de correo...", Toast.LENGTH_SHORT).show()
        }
    }
}