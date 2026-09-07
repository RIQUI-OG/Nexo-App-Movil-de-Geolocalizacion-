package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.adapters.ContactsAdapter
import com.example.avanceproyecto.models.UserConnection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ContactsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var fabAddContact: FloatingActionButton

    private var userId: String = ""
    private val contactsList = mutableListOf<UserConnection>()

    companion object {
        private const val TAG = "ContactsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        setupNavigationDrawer()
        setupRecyclerView()
        setupFab()
        setupPendingRequestsButton()
        loadContacts()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mis Contactos"

        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navView.getHeaderView(0)
        val imgProfileHeader = headerView.findViewById<ImageView>(R.id.img_profile_header)
        imgProfileHeader?.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                finish()
            }
            R.id.nav_family_location -> {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_routes -> {
                val intent = Intent(this, RoutesActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_chat -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_contacts -> {
                Toast.makeText(this, "Ya estás en Contactos", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                val intent = Intent(this, ConfiguracionActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupPendingRequestsButton() {
        val btnPendingRequests = findViewById<Button>(R.id.btnPendingRequests)
        btnPendingRequests?.setOnClickListener {
            Toast.makeText(this, "Solicitudes Pendientes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewContacts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ContactsAdapter(contactsList) { connection ->
            showConnectionOptions(connection)
        }

        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        fabAddContact = findViewById(R.id.fabAddContact)
        fabAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etContactEmail)
        val rgCategory = dialogView.findViewById<RadioGroup>(R.id.rgCategory)
        val btnSendRequest = dialogView.findViewById<Button>(R.id.btnSendRequest)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSendRequest.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val selectedCategoryId = rgCategory.checkedRadioButtonId
            val category = when (selectedCategoryId) {
                R.id.rbFamilia -> "Familia"
                R.id.rbAmigos -> "Amigos"
                R.id.rbOtros -> "Otros"
                else -> ""
            }

            if (name.isEmpty()) {
                etName.error = "Ingresa el nombre"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Ingresa el correo"
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            Toast.makeText(this, "Solicitud de amistad enviada a $name", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun createReverseConnection(connectedUserId: String, type: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: ""
                val userEmail = userDoc.getString("email") ?: ""

                val connectionId = firestore.collection("connections").document().id
                val reverseConnection = UserConnection(
                    connectionId = connectionId,
                    userId = connectedUserId,
                    connectedUserId = userId,
                    connectedUserName = userName,
                    connectedUserEmail = userEmail,
                    type = type,
                    status = "accepted",
                    createdAt = Timestamp.now()
                )

                firestore.collection("connections")
                    .document(connectionId)
                    .set(reverseConnection)
            }
    }

    private fun loadContacts() {
        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                contactsList.clear()

                for (document in documents) {
                    val connection = document.toObject(UserConnection::class.java)
                    contactsList.add(connection)
                }

                adapter.notifyDataSetChanged()

                if (contactsList.isEmpty()) {
                    Toast.makeText(this, "📝 No tienes contactos aún. ¡Agrega a tu familia y amigos!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al cargar contactos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConnectionOptions(connection: UserConnection) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_contact, null)
        val tvHeaderTitle = dialogView.findViewById<TextView>(R.id.tvHeaderTitle)
        val tvHeaderSubTitle = dialogView.findViewById<TextView>(R.id.tvHeaderSubTitle)
        val btnViewMap = dialogView.findViewById<Button>(R.id.btnViewMap)
        val etName = dialogView.findViewById<EditText>(R.id.etEditContactName)
        val btnSaveChanges = dialogView.findViewById<Button>(R.id.btnSaveChanges)
        val btnDeleteContact = dialogView.findViewById<Button>(R.id.btnDeleteContact)

        tvHeaderTitle.text = connection.connectedUserName
        tvHeaderSubTitle.text = connection.connectedUserEmail.ifEmpty { "Contacto" }
        etName.setText(connection.connectedUserName)

        when (connection.type.lowercase()) {
            "family", "familia" -> dialogView.findViewById<RadioButton>(R.id.rbEditFamilia)?.isChecked = true
            "friend", "amigos", "amigo" -> dialogView.findViewById<RadioButton>(R.id.rbEditAmigos)?.isChecked = true
            else -> dialogView.findViewById<RadioButton>(R.id.rbEditOtros)?.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnViewMap.setOnClickListener {
            dialog.dismiss()
            viewOnMap(connection)
        }

        btnSaveChanges.setOnClickListener {
            val updatedName = etName.text.toString().trim()
            if (updatedName.isEmpty()) {
                etName.error = "Ingresa el nombre"
                return@setOnClickListener
            }

            dialog.dismiss()
            Toast.makeText(this, "Contacto $updatedName actualizado localmente", Toast.LENGTH_SHORT).show()
        }

        btnDeleteContact.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setMessage("¿Estás seguro de eliminar a ${connection.connectedUserName}?")
                .setPositiveButton("Sí, eliminar") { confirmDialog, _ ->
                    confirmDialog.dismiss()
                    dialog.dismiss()
                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        dialog.show()
    }

    private fun viewOnMap(connection: UserConnection) {
        Log.d(TAG, "viewOnMap called")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Contact ID: ${connection.connectedUserId}")
        Log.d(TAG, "Contact Name: ${connection.connectedUserName}")

        try {
            val intent = Intent(this, MapActivity::class.java).apply {
                putExtra("USER_ID", userId)
                putExtra("FOCUS_CONTACT_ID", connection.connectedUserId)
                putExtra("FOCUS_CONTACT_NAME", connection.connectedUserName)
            }

            Log.d(TAG, "Starting MapActivity...")
            startActivity(intent)

            Toast.makeText(
                this,
                "🗺️ Abriendo ubicación de ${connection.connectedUserName}...",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MapActivity", e)
            Toast.makeText(this, "❌ Error al abrir mapa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun changeConnectionType(connection: UserConnection) {
        val newType = if (connection.type == "family") "friend" else "family"
        val newTypeText = if (newType == "family") "Familiar" else "Amigo"

        AlertDialog.Builder(this)
            .setTitle("Cambiar tipo de contacto")
            .setMessage("¿Cambiar a ${connection.connectedUserName} como ${newTypeText}?")
            .setPositiveButton("Sí, cambiar") { _, _ ->
                firestore.collection("connections")
                    .document(connection.connectionId)
                    .update("type", newType)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "✅ ${connection.connectedUserName} ahora es tu ${newTypeText}",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadContacts()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteConnection(connection: UserConnection) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar Contacto")
            .setMessage("¿Estás seguro de eliminar a ${connection.connectedUserName}?\n\nYa no podrán ver su ubicación mutua.")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                firestore.collection("connections")
                    .document(connection.connectionId)
                    .delete()
                    .addOnSuccessListener {
                        // También eliminar la conexión inversa
                        deleteReverseConnection(connection.connectedUserId)

                        Toast.makeText(this, "✅ Contacto eliminado", Toast.LENGTH_SHORT).show()
                        loadContacts()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteReverseConnection(connectedUserId: String) {
        // Buscar y eliminar la conexión inversa
        firestore.collection("connections")
            .whereEqualTo("userId", connectedUserId)
            .whereEqualTo("connectedUserId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}