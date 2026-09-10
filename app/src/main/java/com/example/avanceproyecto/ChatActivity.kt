package com.example.avanceproyecto

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.adapters.MessagesAdapter
import com.example.avanceproyecto.models.ChatMessage
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

    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    private var myUserId: String = ""
    private var myUserName: String = "Usuario"
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

        tvChatUser.text = contactName
        tvChatStatus.text = if (isGroup) "Grupo de chat" else "En línea"

        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupChatId() {
        if (chatId.isEmpty()) {
            if (contactId.isNotEmpty() && myUserId.isNotEmpty()) {
                // Generar chatId determinístico 1 a 1
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
                }
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
            text = text,
            timestamp = System.currentTimeMillis()
        )

        messageRef.setValue(chatMessage)
            .addOnSuccessListener {
                etMessage.setText("")

                val conversationData = mapOf(
                    "chatId" to chatId,
                    "lastMessage" to text,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "title" to contactName,
                    "isGroup" to isGroup
                )

                realtimeDatabase.getReference("chat_conversations/$chatId")
                    .setValue(conversationData)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error al enviar mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
