package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.avanceproyecto.adapters.ContactsAdapter
import com.example.avanceproyecto.adapters.PendingRequestsAdapter
import com.example.avanceproyecto.models.ChatConversation
import com.example.avanceproyecto.models.UserConnection
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ContactsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var fabAddContact: FloatingActionButton
    private lateinit var btnPendingRequests: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var myUserName: String = "Usuario"
    private var myUserEmail: String = ""
    private var myPhotoUrl: String = ""

    private val contactsList = mutableListOf<UserConnection>()
    private val pendingRequestsList = mutableListOf<UserConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""
        myUserEmail = auth.currentUser?.email ?: ""

        setupNavigationDrawer()
        setupRecyclerView()
        setupFab()
        setupPendingRequestsButton()
        loadMyUserInfo()
        loadContacts()
        checkPendingRequests()
    }

    private fun loadMyUserInfo() {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        myUserName = doc.getString("name") ?: "Usuario"
                        myPhotoUrl = doc.getString("photoUrl") ?: ""

                        val navView: NavigationView = findViewById(R.id.nav_view)
                        val headerView = navView.getHeaderView(0)
                        val tvProfileHeader = headerView?.findViewById<TextView>(R.id.tv_profile_header)
                        tvProfileHeader?.text = myUserName

                        val imgProfileHeader = headerView?.findViewById<ImageView>(R.id.img_profile_header)
                        if (myPhotoUrl.isNotEmpty() && imgProfileHeader != null) {
                            Glide.with(this)
                                .load(myPhotoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .into(imgProfileHeader)
                        }
                    }
                }
        }
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
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
                val intent = Intent(this, ChatListActivity::class.java)
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
                AlertDialog.Builder(this)
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de cerrar sesión?")
                    .setPositiveButton("Sí") { _, _ ->
                        auth.signOut()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupPendingRequestsButton() {
        btnPendingRequests = findViewById(R.id.btnPendingRequests)
        btnPendingRequests.setOnClickListener {
            showPendingRequestsDialog()
        }
    }

    private fun checkPendingRequests() {
        if (userId.isEmpty()) return

        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                pendingRequestsList.clear()

                for (doc in documents) {
                    val request = doc.toObject(UserConnection::class.java)
                    pendingRequestsList.add(request)
                }

                if (pendingRequestsList.isNotEmpty()) {
                    btnPendingRequests.text = "📩 Solicitudes Pendientes (${pendingRequestsList.size})"
                } else {
                    btnPendingRequests.text = "Solicitudes Pendientes (0)"
                }
            }
    }

    private fun showPendingRequestsDialog() {
        if (pendingRequestsList.isEmpty()) {
            Toast.makeText(this, "ℹ️ No tienes solicitudes de contacto pendientes", Toast.LENGTH_SHORT).show()
            return
        }

        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#156082"))
        }

        val titleTv = TextView(this).apply {
            text = "📩 Solicitudes Pendientes (${pendingRequestsList.size})"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        containerLayout.addView(titleTv)

        val pendingRv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
        }

        var dialog: AlertDialog? = null

        val pendingAdapter = PendingRequestsAdapter(
            pendingRequestsList,
            onAcceptClick = { request ->
                dialog?.dismiss()
                acceptPendingRequest(request)
            },
            onDeclineClick = { request ->
                dialog?.dismiss()
                declinePendingRequest(request)
            }
        )

        pendingRv.adapter = pendingAdapter
        containerLayout.addView(pendingRv)

        dialog = AlertDialog.Builder(this)
            .setView(containerLayout)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun acceptPendingRequest(request: UserConnection) {
        firestore.collection("connections")
            .document(request.connectionId)
            .update("status", "accepted")
            .addOnSuccessListener {
                val reverseType = if (request.type == "Niño") "Tutor" else request.type
                createReverseConnection(request.connectedUserId, reverseType)

                if (request.type.equals("Familia", ignoreCase = true) || request.type.equals("family", ignoreCase = true)) {
                    syncFamilyGroupChat(request.connectedUserId, request.connectedUserName)
                }

                Toast.makeText(this, "✅ Solicitud aceptada de ${request.connectedUserName}", Toast.LENGTH_SHORT).show()
                loadContacts()
                checkPendingRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al aceptar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declinePendingRequest(request: UserConnection) {
        firestore.collection("connections")
            .document(request.connectionId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                checkPendingRequests()
            }
    }

    private fun createReverseConnection(senderUserId: String, type: String) {
        val connectionId = firestore.collection("connections").document().id
        val connection = UserConnection(
            connectionId = connectionId,
            userId = senderUserId,
            connectedUserId = userId,
            connectedUserName = myUserName,
            connectedUserEmail = myUserEmail,
            type = type,
            status = "accepted",
            createdAt = Timestamp.now()
        )

        firestore.collection("connections")
            .document(connectionId)
            .set(connection)
    }

    private fun syncFamilyGroupChat(connectedUserId: String, connectedUserName: String) {
        if (userId.isEmpty() || connectedUserId.isEmpty()) return

        val groupIdA = "group_family_$userId"
        val refA = realtimeDatabase.getReference("chat_conversations/$groupIdA")
        syncGroup(refA, userId, connectedUserId, "👨‍👩‍👧‍👦 Familia de $myUserName", connectedUserName)

        val groupIdB = "group_family_$connectedUserId"
        val refB = realtimeDatabase.getReference("chat_conversations/$groupIdB")
        syncGroup(refB, connectedUserId, userId, "👨‍👩‍👧‍👦 Familia de $connectedUserName", myUserName)
    }

    private fun syncGroup(ref: DatabaseReference, ownerId: String, newMemberId: String, groupTitle: String, newMemberName: String) {
        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val conversation = snapshot.getValue(ChatConversation::class.java)
                if (conversation != null) {
                    val currentList = conversation.participants?.toMutableList() ?: mutableListOf()
                    if (!currentList.contains(newMemberId)) {
                        currentList.add(newMemberId)
                    }
                    if (!currentList.contains(ownerId)) {
                        currentList.add(ownerId)
                    }

                    ref.child("participants").setValue(currentList)
                    ref.child("lastMessage").setValue("👋 $newMemberName se unió al Chat Familiar")
                    ref.child("lastMessageTime").setValue(System.currentTimeMillis())
                }
            } else {
                val newFamilyGroup = ChatConversation(
                    chatId = ref.key ?: "",
                    title = groupTitle,
                    isGroup = true,
                    participants = listOf(ownerId, newMemberId),
                    lastMessage = "👋 Chat Familiar creado con $newMemberName",
                    lastMessageTime = System.currentTimeMillis()
                )
                ref.setValue(newFamilyGroup)
            }
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
                R.id.rbNino -> "Niño"
                R.id.rbOtros -> "Otros"
                else -> ""
            }

            if (email.isEmpty()) {
                etEmail.error = "Ingresa el correo registrado"
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            sendContactRequest(name, email, category)
        }

        dialog.show()
    }

    private fun sendContactRequest(inputName: String, email: String, category: String) {
        val currentEmail = auth.currentUser?.email ?: myUserEmail
        if (email.equals(currentEmail, ignoreCase = true)) {
            Toast.makeText(this, "⚠️ No puedes enviarte una solicitud a ti mismo", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "🔍 Buscando usuario '$email'...", Toast.LENGTH_SHORT).show()

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(
                        this,
                        "❌ No se encontró ningún usuario registrado con el correo '$email'. Verifica que esté registrado en la app.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val foundUserDoc = querySnapshot.documents[0]
                val foundUserId = foundUserDoc.id
                val foundUserName = foundUserDoc.getString("name") ?: inputName.ifEmpty { "Usuario" }

                val connectionId = firestore.collection("connections").document().id

                val reverseType = when (category) {
                    "Niño" -> "Tutor"
                    "Tutor" -> "Niño"
                    else -> category
                }

                val connectionForReceiver = UserConnection(
                    connectionId = connectionId,
                    userId = foundUserId,
                    connectedUserId = userId,
                    connectedUserName = myUserName,
                    connectedUserEmail = myUserEmail,
                    type = reverseType,
                    status = "pending",
                    createdAt = Timestamp.now()
                )

                firestore.collection("connections")
                    .document(connectionId)
                    .set(connectionForReceiver)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "📩 Solicitud enviada a $foundUserName ($email)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Error al enviar solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al buscar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadContacts() {
        if (userId.isEmpty()) return

        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                contactsList.clear()

                if (documents.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
                    emptyStateLayout?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val totalDocs = documents.size()
                var processedDocs = 0

                for (document in documents) {
                    val connection = document.toObject(UserConnection::class.java)
                    val connectedUserId = connection.connectedUserId

                    if (connectedUserId.isNotEmpty()) {
                        firestore.collection("users").document(connectedUserId).get()
                            .addOnSuccessListener { userDoc ->
                                val photo = userDoc.getString("photoUrl") ?: ""
                                val updatedConnection = connection.copy(photoUrl = photo)
                                contactsList.add(updatedConnection)

                                processedDocs++
                                if (processedDocs == totalDocs) {
                                    adapter.notifyDataSetChanged()
                                    val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
                                    emptyStateLayout?.visibility = if (contactsList.isEmpty()) View.VISIBLE else View.GONE
                                    recyclerView.visibility = if (contactsList.isEmpty()) View.GONE else View.VISIBLE
                                }
                            }
                            .addOnFailureListener {
                                contactsList.add(connection)
                                processedDocs++
                                if (processedDocs == totalDocs) {
                                    adapter.notifyDataSetChanged()
                                    val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
                                    emptyStateLayout?.visibility = if (contactsList.isEmpty()) View.VISIBLE else View.GONE
                                    recyclerView.visibility = if (contactsList.isEmpty()) View.GONE else View.VISIBLE
                                }
                            }
                    } else {
                        contactsList.add(connection)
                        processedDocs++
                        if (processedDocs == totalDocs) {
                            adapter.notifyDataSetChanged()
                            val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
                            emptyStateLayout?.visibility = if (contactsList.isEmpty()) View.VISIBLE else View.GONE
                            recyclerView.visibility = if (contactsList.isEmpty()) View.GONE else View.VISIBLE
                        }
                    }
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
        val btnStartChat = dialogView.findViewById<Button>(R.id.btnStartChat)
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

        btnStartChat?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CONTACT_ID", connection.connectedUserId)
                putExtra("CONTACT_NAME", connection.connectedUserName)
                putExtra("IS_GROUP", false)
            }
            startActivity(intent)
        }

        btnViewMap.setOnClickListener {
            dialog.dismiss()
            viewOnMap(connection)
        }

        btnSaveChanges.setOnClickListener {
            val updatedName = etName.text.toString().trim()
            val updatedCategory = when {
                dialogView.findViewById<RadioButton>(R.id.rbEditFamilia)?.isChecked == true -> "Familia"
                dialogView.findViewById<RadioButton>(R.id.rbEditAmigos)?.isChecked == true -> "Amigos"
                else -> "Otros"
            }

            if (updatedName.isEmpty()) {
                etName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }

            dialog.dismiss()
            updateConnectionInFirestore(connection, updatedName, updatedCategory)
        }

        btnDeleteContact.setOnClickListener {
            dialog.dismiss()
            confirmDeleteContact(connection)
        }

        dialog.show()
    }

    private fun updateConnectionInFirestore(connection: UserConnection, newName: String, newCategory: String) {
        firestore.collection("connections")
            .document(connection.connectionId)
            .update(
                mapOf(
                    "connectedUserName" to newName,
                    "type" to newCategory
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Contacto actualizado", Toast.LENGTH_SHORT).show()
                loadContacts()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteContact(connection: UserConnection) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Estás seguro de eliminar a '${connection.connectedUserName}' de tus contactos?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()

                firestore.collection("connections")
                    .document(connection.connectionId)
                    .delete()
                    .addOnSuccessListener {
                        deleteReverseConnection(connection.connectedUserId)
                        Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                        loadContacts()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun viewOnMap(connection: UserConnection) {
        val intent = Intent(this, MapActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("FOCUS_CONTACT_ID", connection.connectedUserId)
            putExtra("FOCUS_CONTACT_NAME", connection.connectedUserName)
        }
        startActivity(intent)
    }

    private fun deleteReverseConnection(connectedUserId: String) {
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

    override fun onResume() {
        super.onResume()
        loadMyUserInfo()
        loadContacts()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
