package com.example.avanceproyecto

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.adapters.ContactsAdapter
import com.example.avanceproyecto.models.UserConnection
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ContactsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var fabAddContact: FloatingActionButton

    private var userId: String = ""
    private val contactsList = mutableListOf<UserConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Contactos"

        firestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: ""

        setupRecyclerView()
        setupFab()
        loadContacts()
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
        val etEmail = dialogView.findViewById<EditText>(R.id.etContactEmail)
        val btnFamily = dialogView.findViewById<Button>(R.id.btnAddAsFamily)
        val btnFriend = dialogView.findViewById<Button>(R.id.btnAddAsFriend)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Contacto")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        btnFamily.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                addContact(email, "family")
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Ingresa un correo", Toast.LENGTH_SHORT).show()
            }
        }

        btnFriend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                addContact(email, "friend")
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Ingresa un correo", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addContact(email: String, type: String) {
        // Buscar usuario por correo
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val connectedUserId = userDoc.id
                val connectedUserName = userDoc.getString("name") ?: ""
                val connectedUserEmail = userDoc.getString("email") ?: ""

                if (connectedUserId == userId) {
                    Toast.makeText(this, "No puedes agregarte a ti mismo", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Verificar si ya existe la conexión
                firestore.collection("connections")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("connectedUserId", connectedUserId)
                    .get()
                    .addOnSuccessListener { existing ->
                        if (!existing.isEmpty) {
                            Toast.makeText(this, "Ya tienes agregado a este contacto", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Crear nueva conexión
                        val connectionId = firestore.collection("connections").document().id
                        val connection = UserConnection(
                            connectionId = connectionId,
                            userId = userId,
                            connectedUserId = connectedUserId,
                            connectedUserName = connectedUserName,
                            connectedUserEmail = connectedUserEmail,
                            type = type,
                            status = "accepted",
                            createdAt = Timestamp.now()
                        )

                        firestore.collection("connections")
                            .document(connectionId)
                            .set(connection)
                            .addOnSuccessListener {
                                // También crear la conexión inversa para que sea bidireccional
                                createReverseConnection(connectedUserId, type)

                                Toast.makeText(
                                    this,
                                    "✅ Contacto agregado como ${if (type == "family") "Familiar" else "Amigo"}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                loadContacts()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    Toast.makeText(this, "No tienes contactos aún", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar contactos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConnectionOptions(connection: UserConnection) {
        val options = arrayOf("Ver en mapa", "Cambiar tipo", "Eliminar contacto")

        AlertDialog.Builder(this)
            .setTitle(connection.connectedUserName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewOnMap(connection)
                    1 -> changeConnectionType(connection)
                    2 -> deleteConnection(connection)
                }
            }
            .show()
    }

    private fun viewOnMap(connection: UserConnection) {
        // Aquí implementarías abrir el mapa centrado en ese contacto
        Toast.makeText(this, "Abriendo mapa de ${connection.connectedUserName}", Toast.LENGTH_SHORT).show()
    }

    private fun changeConnectionType(connection: UserConnection) {
        val newType = if (connection.type == "family") "friend" else "family"

        firestore.collection("connections")
            .document(connection.connectionId)
            .update("type", newType)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Ahora ${connection.connectedUserName} es tu ${if (newType == "family") "Familiar" else "Amigo"}",
                    Toast.LENGTH_SHORT
                ).show()
                loadContacts()
            }
    }

    private fun deleteConnection(connection: UserConnection) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Estás seguro de eliminar a ${connection.connectedUserName}?")
            .setPositiveButton("Eliminar") { _, _ ->
                firestore.collection("connections")
                    .document(connection.connectionId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                        loadContacts()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}