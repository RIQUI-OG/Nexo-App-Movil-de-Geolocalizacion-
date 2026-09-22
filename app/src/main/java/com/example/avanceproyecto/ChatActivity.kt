package com.example.avanceproyecto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.avanceproyecto.adapters.MessagesAdapter
import com.example.avanceproyecto.models.ChatMessage
import com.example.avanceproyecto.models.UserConnection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: CardView
    private lateinit var btnBack: ImageButton
    private lateinit var tvChatUser: TextView
    private lateinit var tvChatStatus: TextView
    private lateinit var imgHeaderProfile: ImageView
    private lateinit var btnAddMember: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    private var myUserId: String = ""
    private var myUserName: String = "Usuario"
    private var myPhotoUrl: String = ""
    private var contactId: String = ""
    private var contactName: String = "Contacto"
    private var chatId: String = ""
    private var isGroup: Boolean = false

    private val messagesList = mutableListOf<ChatMessage>()
    private lateinit var adapter: MessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        myUserId = auth.currentUser?.uid ?: intent.getStringExtra("USER_ID") ?: ""
        contactId = intent.getStringExtra("CONTACT_ID") ?: ""
        contactName = intent.getStringExtra("CONTACT_NAME") ?: "Chat General"
        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        isGroup = intent.getBooleanExtra("IS_GROUP", false)

        initViews()
        setupChatId()
        loadMyUserName()
        resolveHeaderContactInfo()
        loadGroupParticipants()
        setupRecyclerView()

        if (contactId.isEmpty() && chatId.isEmpty() && !isGroup) {
            tvChatUser.text = "Chat"
            tvChatStatus.text = "Selecciona un contacto"
            Toast.makeText(this, "📝 Ve a Mis Contactos y selecciona 'Chat' en cualquier contacto para iniciar una conversación.", Toast.LENGTH_LONG).show()
        } else {
            listenToMessages()
        }
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        tvChatUser = findViewById(R.id.tvChatUser)
        tvChatStatus = findViewById(R.id.tvChatStatus)
        imgHeaderProfile = findViewById(R.id.imgHeaderProfile)
        btnAddMember = findViewById(R.id.btnAddMember)

        tvChatUser.text = contactName
        tvChatStatus.text = if (isGroup) "Grupo de chat" else "En línea"

        if (isGroup) {
            btnAddMember.visibility = View.VISIBLE
            btnAddMember.setOnClickListener {
                showAddMemberDialog()
            }
        }

        btnBack.setOnClickListener {
            handleBack()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun resolveHeaderContactInfo() {
        if (isGroup) return

        val otherUserId = if (contactId.isNotEmpty()) contactId else {
            chatId.replace("chat_", "").replace("_", "").replace(myUserId, "")
        }

        if (otherUserId.isNotEmpty() && otherUserId != myUserId) {
            firestore.collection("users").document(otherUserId).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        val realName = userDoc.getString("name") ?: ""
                        val photoUrl = userDoc.getString("photoUrl") ?: ""

                        if (realName.isNotEmpty()) {
                            contactName = realName
                            tvChatUser.text = realName
                        }

                        if (photoUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .into(imgHeaderProfile)
                        }
                    }
                }
        }
    }

    private fun handleBack() {
        if (isTaskRoot || intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            val homeIntent = Intent(this, HomeActivity::class.java).apply {
                putExtra("USER_ID", myUserId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)
        }
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handleBack()
    }

    private fun showAddMemberDialog() {
        if (myUserId.isEmpty() || chatId.isEmpty()) return

        firestore.collection("connections")
            .whereEqualTo("userId", myUserId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                val contacts = mutableListOf<UserConnection>()
                for (doc in documents) {
                    val connection = doc.toObject(UserConnection::class.java)
                    contacts.add(connection)
                }

                if (contacts.isEmpty()) {
                    Toast.makeText(this, "No tienes contactos para agregar.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                realtimeDatabase.getReference("chat_conversations/$chatId/participants")
                    .get().addOnSuccessListener { snapshot ->
                        val currentParticipants = snapshot.children.mapNotNull { it.getValue(String::class.java) }

                        val availableContacts = contacts.filter { !currentParticipants.contains(it.connectedUserId) }

                        if (availableContacts.isEmpty()) {
                            Toast.makeText(this, "Todos tus contactos ya están en este grupo.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val contactNames = availableContacts.map { it.connectedUserName }.toTypedArray()

                        AlertDialog.Builder(this)
                            .setTitle("Agregar miembro al grupo")
                            .setItems(contactNames) { _, which ->
                                val selectedContact = availableContacts[which]
                                addNewMemberToGroup(selectedContact)
                            }
                            .show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar contactos.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addNewMemberToGroup(contact: UserConnection) {
        val participantsRef = realtimeDatabase.getReference("chat_conversations/$chatId/participants")
        participantsRef.get().addOnSuccessListener { snapshot ->
            val currentList = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
            if (!currentList.contains(contact.connectedUserId)) {
                currentList.add(contact.connectedUserId)
                participantsRef.setValue(currentList)

                val messageRef = realtimeDatabase.getReference("chat_messages/$chatId").push()
                val chatMessage = ChatMessage(
                    messageId = messageRef.key ?: System.currentTimeMillis().toString(),
                    chatId = chatId,
                    senderId = "SYSTEM",
                    senderName = "Sistema",
                    text = "${contact.connectedUserName} fue agregado al grupo.",
                    timestamp = System.currentTimeMillis()
                )
                messageRef.setValue(chatMessage)

                realtimeDatabase.getReference("chat_conversations/$chatId/lastMessage").setValue("${contact.connectedUserName} se unió.")

                Toast.makeText(this, "${contact.connectedUserName} agregado al grupo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupChatId() {
        if (chatId.isEmpty()) {
            if (contactId.isNotEmpty() && myUserId.isNotEmpty()) {
                chatId = if (myUserId < contactId) "${myUserId}_${contactId}" else "${contactId}_${myUserId}"
            } else if (isGroup) {
                chatId = "group_general"
            } else {
                chatId = "general_chat"
            }
        }
    }

    private fun loadMyUserName() {
        if (myUserId.isNotEmpty() && !myUserId.startsWith("guest_")) {
            firestore.collection("users").document(myUserId).get()
                .addOnSuccessListener { doc ->
                    myUserName = doc.getString("name") ?: "Usuario"
                    myPhotoUrl = doc.getString("photoUrl") ?: ""
                }
        }
    }

    private fun loadGroupParticipants() {
        if (!isGroup || chatId.isEmpty()) return

        firestore.collection("connections")
            .whereEqualTo("userId", myUserId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                val contactsMap = documents.associate {
                    it.getString("connectedUserId") to it.getString("connectedUserName")
                }

                realtimeDatabase.getReference("chat_conversations/$chatId/participants")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val participantIds = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                            val names = participantIds.map { id ->
                                if (id == myUserId) "Tú"
                                else contactsMap[id] ?: "Usuario"
                            }
                            tvChatStatus.text = names.joinToString(", ")
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
    }

    private fun setupRecyclerView() {
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        adapter = MessagesAdapter(messagesList, myUserId)
        rvMessages.adapter = adapter
    }

    private fun listenToMessages() {
        if (chatId.isEmpty()) return

        realtimeDatabase.getReference("chat_messages/$chatId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()

                    for (messageSnapshot in snapshot.children) {
                        val message = try {
                            messageSnapshot.getValue(ChatMessage::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        if (message != null) {
                            messagesList.add(message)
                        }
                    }

                    adapter.notifyDataSetChanged()

                    if (messagesList.isNotEmpty()) {
                        rvMessages.smoothScrollToPosition(messagesList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (error.code == DatabaseError.PERMISSION_DENIED) {
                        Toast.makeText(
                            this@ChatActivity,
                            "🔒 Permisos de Realtime Database restringidos. Configura las Reglas en Firebase Console.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@ChatActivity, "Error al cargar chat: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || chatId.isEmpty()) return

        val messageRef = realtimeDatabase.getReference("chat_messages/$chatId").push()
        val messageId = messageRef.key ?: System.currentTimeMillis().toString()

        val chatMessage = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = myUserId,
            senderName = myUserName,
            senderPhotoUrl = myPhotoUrl,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        messageRef.setValue(chatMessage)
            .addOnSuccessListener {
                etMessage.setText("")

                val conversationData = mutableMapOf<String, Any>(
                    "chatId" to chatId,
                    "lastMessage" to text,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "isGroup" to isGroup
                )

                if (isGroup) {
                    conversationData["title"] = contactName
                }

                realtimeDatabase.getReference("chat_conversations/$chatId")
                    .updateChildren(conversationData)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al enviar mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
