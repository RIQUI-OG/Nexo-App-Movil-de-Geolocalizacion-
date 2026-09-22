package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.avanceproyecto.adapters.ConversationsAdapter
import com.example.avanceproyecto.models.ChatConversation
import com.example.avanceproyecto.models.UserConnection
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ChatListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvConversations: RecyclerView
    private lateinit var emptyStateChatList: LinearLayout
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var swipeRefreshChatList: SwipeRefreshLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    private var userId: String = ""
    private var userName: String = "Usuario"
    private var myPhotoUrl: String = ""

    private val conversationsList = mutableListOf<ChatConversation>()
    private val userContactsList = mutableListOf<UserConnection>()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        auth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        setupToolbarAndDrawer()
        initViews()
        loadUserName()
        loadContacts()
        listenToConversations()
    }

    private fun setupToolbarAndDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout_chat_list)
        val navView: NavigationView = findViewById(R.id.nav_view_chat_list)
        val toolbar: Toolbar = findViewById(R.id.toolbarChatList)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Conversaciones"

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

    private fun initViews() {
        rvConversations = findViewById(R.id.rvConversations)
        emptyStateChatList = findViewById(R.id.emptyStateChatList)
        fabNewChat = findViewById(R.id.fabNewChat)
        swipeRefreshChatList = findViewById(R.id.swipeRefreshChatList)

        rvConversations.layoutManager = LinearLayoutManager(this)

        adapter = ConversationsAdapter(
            conversationsList = conversationsList,
            currentUserId = userId,
            onConversationClick = { conversation ->
                openChatActivity(conversation.chatId, conversation.title, conversation.isGroup)
            },
            onConversationLongClick = { conversation ->
                showDeleteConversationDialog(conversation)
            },
            resolveContactName = { contactId ->
                resolveContactName(contactId)
            }
        )

        rvConversations.adapter = adapter

        fabNewChat.setOnClickListener {
            showNewChatOrGroupOptions()
        }

        swipeRefreshChatList.setOnRefreshListener {
            loadContacts()
            listenToConversations()
            swipeRefreshChatList.isRefreshing = false
        }
    }

    private fun loadUserName() {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userName = doc.getString("name") ?: "Usuario"
                        myPhotoUrl = doc.getString("photoUrl") ?: ""

                        val navView: NavigationView = findViewById(R.id.nav_view_chat_list)
                        val headerView = navView.getHeaderView(0)
                        val tvProfileHeader = headerView?.findViewById<TextView>(R.id.tv_profile_header)
                        tvProfileHeader?.text = userName

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

    private fun loadContacts() {
        if (userId.isEmpty()) return

        firestore.collection("connections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                userContactsList.clear()
                for (doc in documents) {
                    val connection = doc.toObject(UserConnection::class.java)
                    userContactsList.add(connection)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun listenToConversations() {
        if (userId.isEmpty()) return

        realtimeDatabase.getReference("chat_conversations")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    conversationsList.clear()

                    for (itemSnapshot in snapshot.children) {
                        val conversation = try {
                            itemSnapshot.getValue(ChatConversation::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e("ChatListActivity", "Error parsing conversation: ${e.message}", e)
                            null
                        }

                        if (conversation != null) {
                            val isMyGroup = conversation.isGroup && (conversation.participants?.contains(userId) == true || conversation.participants.isNullOrEmpty())
                            val isMyIndividualChat = !conversation.isGroup && conversation.chatId.contains(userId)

                            if (isMyGroup || isMyIndividualChat) {
                                if (!conversation.isGroup) {
                                    val otherUserId = conversation.participants?.find { it != userId }
                                        ?: conversation.chatId.replace("chat_", "").replace("_", "").replace(userId, "")

                                    if (otherUserId.isNotEmpty()) {
                                        val localContact = userContactsList.find { it.connectedUserId == otherUserId }
                                        if (localContact != null && localContact.connectedUserName.isNotEmpty()) {
                                            conversation.title = localContact.connectedUserName
                                            if (localContact.photoUrl.isNotEmpty()) {
                                                conversation.photoUrl = localContact.photoUrl
                                            }
                                        }

                                        firestore.collection("users").document(otherUserId).get()
                                            .addOnSuccessListener { userDoc ->
                                                val realName = userDoc.getString("name") ?: ""
                                                val photo = userDoc.getString("photoUrl") ?: ""
                                                if (realName.isNotEmpty()) conversation.title = realName
                                                if (photo.isNotEmpty()) conversation.photoUrl = photo
                                                adapter.notifyDataSetChanged()
                                            }
                                    }
                                }

                                conversationsList.add(conversation)
                            }
                        }
                    }

                    conversationsList.sortByDescending { it.lastMessageTime }
                    adapter.notifyDataSetChanged()

                    if (conversationsList.isEmpty()) {
                        emptyStateChatList.visibility = View.VISIBLE
                        rvConversations.visibility = View.GONE
                    } else {
                        emptyStateChatList.visibility = View.GONE
                        rvConversations.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (error.code == DatabaseError.PERMISSION_DENIED) {
                        Toast.makeText(
                            this@ChatListActivity,
                            "🔒 Permiso denegado en Firebase. Agrega 'chat_conversations' a tus Reglas de Firebase Console.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@ChatListActivity, "Error al cargar conversaciones: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun resolveContactName(contactId: String): String {
        val contact = userContactsList.find { it.connectedUserId == contactId }
        return contact?.connectedUserName ?: "Usuario"
    }

    private fun showDeleteConversationDialog(conversation: ChatConversation) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar Chat")
            .setMessage("¿Deseas eliminar la conversación con '${conversation.title}'? Esta acción eliminará el historial para ti.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                realtimeDatabase.getReference("chat_conversations/${conversation.chatId}").removeValue()
                realtimeDatabase.getReference("chat_messages/${conversation.chatId}").removeValue()
                Toast.makeText(this, "Chat eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showNewChatOrGroupOptions() {
        val options = arrayOf("💬 Nuevo Chat Individual", "👥 Crear Nuevo Grupo")

        AlertDialog.Builder(this)
            .setTitle("➕ Nuevo Chat")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSelectContactForChatDialog()
                    1 -> showCreateGroupDialog()
                }
            }
            .show()
    }

    private fun showSelectContactForChatDialog() {
        if (userContactsList.isEmpty()) {
            Toast.makeText(this, "📝 Agrega contactos primero en 'Mis Contactos' para chatear", Toast.LENGTH_LONG).show()
            return
        }

        val contactNames = userContactsList.map { "${it.connectedUserName} (${it.type})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("💬 Selecciona un Contacto")
            .setItems(contactNames) { _, which ->
                val selectedContact = userContactsList[which]
                openChatActivity(
                    chatId = "",
                    contactName = selectedContact.connectedUserName,
                    isGroup = false,
                    contactId = selectedContact.connectedUserId
                )
            }
            .show()
    }

    private fun showCreateGroupDialog() {
        val input = EditText(this).apply {
            hint = "Nombre del Grupo (ej. Familia Pérez)"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("👥 Crear Grupo de Chat")
            .setView(input)
            .setPositiveButton("Crear") { dialog, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isEmpty()) {
                    Toast.makeText(this, "Ingresa un nombre para el grupo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                dialog.dismiss()
                val groupId = "group_${System.currentTimeMillis()}"

                val conversation = ChatConversation(
                    chatId = groupId,
                    title = groupName,
                    isGroup = true,
                    participants = listOf(userId),
                    lastMessage = "Grupo creado por $userName",
                    lastMessageTime = System.currentTimeMillis()
                )

                realtimeDatabase.getReference("chat_conversations/$groupId")
                    .setValue(conversation)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Grupo '$groupName' creado con éxito", Toast.LENGTH_SHORT).show()
                        openChatActivity(groupId, groupName, isGroup = true)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Error al crear grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openChatActivity(chatId: String, contactName: String, isGroup: Boolean, contactId: String = "") {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("CHAT_ID", chatId)
            putExtra("CONTACT_NAME", contactName)
            putExtra("CONTACT_ID", contactId)
            putExtra("IS_GROUP", isGroup)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
        loadContacts()
        listenToConversations()
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
                Toast.makeText(this, "Ya estás en Conversaciones", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_contacts -> {
                val intent = Intent(this, ContactsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
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
}
