package com.example.avanceproyecto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.avanceproyecto.utils.SupabaseStorageHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var btnMenuBack: ImageButton
    private lateinit var imgProfile: ShapeableImageView
    private lateinit var btnEditPhoto: ShapeableImageView
    private lateinit var etProfileName: TextInputEditText
    private lateinit var etProfileEmail: TextInputEditText
    private lateinit var etProfilePhone: TextInputEditText
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton
    private lateinit var tvLogout: TextView
    private lateinit var tvContactAdmin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var userId: String = ""
    private var isEditing = false

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadAndSetProfilePhoto(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        initViews()
        setupListeners()
        loadUserProfile()
    }

    private fun initViews() {
        btnMenuBack = findViewById(R.id.btnMenuBack)
        imgProfile = findViewById(R.id.imgProfile)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)
        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        tvLogout = findViewById(R.id.tvLogout)
        tvContactAdmin = findViewById(R.id.tvContactAdmin)
    }

    private fun loadUserProfile() {
        val currentUid = if (userId.isNotEmpty()) userId else auth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) return

        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: auth.currentUser?.email ?: ""
                    val phone = document.getString("phone") ?: ""
                    val photoUrl = document.getString("photoUrl") ?: ""

                    etProfileName.setText(name)
                    etProfileEmail.setText(email)
                    etProfilePhone.setText(phone)

                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(imgProfile)
                    }
                } else {
                    etProfileEmail.setText(auth.currentUser?.email ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        btnMenuBack.setOnClickListener {
            finish()
        }

        btnEditPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnEditProfile.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                etProfileName.isEnabled = true
                etProfileEmail.isEnabled = true
                etProfilePhone.isEnabled = true
                btnEditProfile.text = "Guardar Cambios"
            } else {
                saveProfileData()
            }
        }

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Cuenta")
                .setMessage("¿Estás seguro de eliminar tu cuenta? Esta acción desvinculará tu usuario.")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    val currentUid = if (userId.isNotEmpty()) userId else auth.currentUser?.uid ?: ""
                    if (currentUid.isNotEmpty()) {
                        firestore.collection("users").document(currentUid).delete()
                    }
                    auth.currentUser?.delete()
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

        tvLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }

        tvContactAdmin.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:soporte@nexoapp.com")
                putExtra(Intent.EXTRA_SUBJECT, "Soporte Nexo App - Perfil")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se encontró cliente de correo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAndSetProfilePhoto(imageUri: Uri) {
        Toast.makeText(this, "⏳ Cargando foto de perfil...", Toast.LENGTH_SHORT).show()

        Glide.with(this)
            .load(imageUri)
            .placeholder(R.drawable.ic_person)
            .into(imgProfile)

        val currentUid = if (userId.isNotEmpty()) userId else auth.currentUser?.uid ?: ""

        SupabaseStorageHelper.uploadProfilePhoto(
            context = this,
            imageUri = imageUri,
            userId = currentUid,
            callback = object : SupabaseStorageHelper.UploadCallback {
                override fun onSuccess(publicUrl: String) {
                    if (currentUid.isNotEmpty()) {
                        firestore.collection("users")
                            .document(currentUid)
                            .update("photoUrl", publicUrl)
                            .addOnSuccessListener {
                                Toast.makeText(this@PerfilActivity, "✅ Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(this@PerfilActivity, "⚠️ Foto actualizada localmente", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun saveProfileData() {
        val name = etProfileName.text.toString().trim()
        val email = etProfileEmail.text.toString().trim()
        val phone = etProfilePhone.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Por favor completa el nombre y correo", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUid = if (userId.isNotEmpty()) userId else auth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) return

        val updates = mapOf(
            "name" to name,
            "email" to email,
            "phone" to phone
        )

        firestore.collection("users")
            .document(currentUid)
            .update(updates)
            .addOnSuccessListener {
                isEditing = false
                etProfileName.isEnabled = false
                etProfileEmail.isEnabled = false
                etProfilePhone.isEnabled = false
                btnEditProfile.text = "Editar"

                Toast.makeText(this, "✅ Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
