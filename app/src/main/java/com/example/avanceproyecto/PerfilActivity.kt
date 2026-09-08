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

import android.app.ProgressDialog
import com.example.avanceproyecto.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        progressDialog = ProgressDialog(this).apply {
            setMessage("Cargando perfil...")
            setCancelable(false)
        }

        initViews()
        setupListeners()
        loadUserData()
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
                // Guardar Cambios en Firebase
                val name = etProfileName.text.toString().trim()
                val email = etProfileEmail.text.toString().trim()
                val phone = etProfilePhone.text.toString().trim()

                if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                updateProfileData(name, email, phone)
            }
        }

        // Botón de Eliminar cuenta
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Cuenta")
                .setMessage("¿Estás seguro de eliminar tu cuenta? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    deleteUserAccount()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Enlace Cerrar sesión
        tvLogout.setOnClickListener {
            auth.signOut()
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

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No se encontró sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressDialog.show()

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    etProfileName.setText(user?.name ?: currentUser.displayName ?: "")
                    etProfileEmail.setText(user?.email ?: currentUser.email ?: "")
                    etProfilePhone.setText(user?.phone ?: "")
                } else {
                    etProfileEmail.setText(currentUser.email ?: "")
                    etProfileName.setText(currentUser.displayName ?: "")
                    Toast.makeText(this, "No se encontraron datos adicionales del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProfileData(name: String, email: String, phone: String) {
        val currentUser = auth.currentUser ?: return

        progressDialog.setMessage("Actualizando perfil...")
        progressDialog.show()

        val updates = mapOf(
            "name" to name,
            "email" to email,
            "phone" to phone
        )

        firestore.collection("users")
            .document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                progressDialog.dismiss()
                isEditing = false
                etProfileName.isEnabled = false
                etProfileEmail.isEnabled = false
                etProfilePhone.isEnabled = false
                btnEditProfile.text = "Editar"
                Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteUserAccount() {
        val currentUser = auth.currentUser ?: return

        progressDialog.setMessage("Eliminando cuenta...")
        progressDialog.show()

        firestore.collection("users")
            .document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                currentUser.delete()
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Cuenta eliminada con éxito", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error al eliminar usuario en Auth: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al eliminar datos de Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}