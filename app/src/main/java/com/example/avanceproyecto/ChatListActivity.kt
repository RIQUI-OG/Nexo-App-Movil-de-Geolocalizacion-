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
    private lateinit var emptyStateChatList: View
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var swipeRefreshChatList: SwipeRefreshLayout

    private val auth = FirebaseAuth.getInstance()
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    private var userId: String = ""
    private var userName: String = "Usuario"
    private val conversationsList = mutableListOf<ChatConversation>()
    private val userContactsList = mutableListOf<UserConnection>()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

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
            resolveContactName = { id ->
                if (id == userId) "Tú"
                else userContactsList.find { it.connectedUserId == id }?.connectedUserName ?: "Usuario"
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
                    userName = doc.getString("name") ?: "Usuario"
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
                            null
                        }

                        if (conversation != null) {
                            // If it's a group, we check if userId is in participants OR if participants is empty (fallback).
                            // If it's individual, we check if chatId contains userId.
                            val isMyGroup = conversation.isGroup && (conversation.participants?.contains(userId) == true || conversation.participants.isNullOrEmpty())
                            val isMyIndividualChat = !conversation.isGroup && conversation.chatId.contains(userId)

                            if (isMyGroup || isMyIndividualChat) {
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

    private fun showDeleteConversationDialog(conversation: ChatConversation) {
        AlertDialog.Builder(this)
            .setTitle("Borrar conversación")
            .setMessage("¿Estás seguro de que deseas borrar esta conversación para siempre?")
            .setPositiveButton("Borrar") { _, _ ->
                realtimeDatabase.getReference("chat_conversations/${conversation.chatId}").removeValue()
                realtimeDatabase.getReference("chat_messages/${conversation.chatId}").removeValue()
                Toast.makeText(this, "Conversación borrada", Toast.LENGTH_SHORT).show()
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
