package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.ChatConversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationsAdapter(
    private val conversationsList: List<ChatConversation>,
    private val onConversationClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatarIcon: TextView = itemView.findViewById(R.id.tvAvatarIcon)
        val tvConversationTitle: TextView = itemView.findViewById(R.id.tvConversationTitle)
        val tvConversationTime: TextView = itemView.findViewById(R.id.tvConversationTime)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = conversationsList[position]

        holder.tvConversationTitle.text = item.title.ifEmpty { "Chat" }
        holder.tvLastMessage.text = item.lastMessage.ifEmpty { "Sin mensajes aún" }
        holder.tvConversationTime.text = if (item.lastMessageTime > 0) timeFormat.format(Date(item.lastMessageTime)) else ""
        holder.tvAvatarIcon.text = if (item.isGroup) "👥" else "👤"

        holder.itemView.setOnClickListener {
            onConversationClick(item)
        }
    }

    override fun getItemCount(): Int = conversationsList.size
}
