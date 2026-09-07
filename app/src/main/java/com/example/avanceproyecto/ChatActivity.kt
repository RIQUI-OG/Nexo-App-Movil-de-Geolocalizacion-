package com.example.avanceproyecto

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: CardView
    private lateinit var btnBack: ImageButton
    private lateinit var tvChatUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        tvChatUser = findViewById(R.id.tvChatUser)

        val contactName = intent.getStringExtra("CONTACT_NAME") ?: "Contacto"
        tvChatUser.text = contactName

        rvMessages.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                etMessage.setText("")
            }
        }
    }
}
