package com.example.avanceproyecto.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter(
    private val messageList: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: LinearLayout = itemView as LinearLayout
        val cvSenderAvatar: CardView = itemView.findViewById(R.id.cvSenderAvatar)
        val bubbleLayout: LinearLayout = itemView.findViewById(R.id.bubbleLayout)
        val tvMessageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val tvMessageTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        val replyLayout: LinearLayout = itemView.findViewById(R.id.replyLayout)
        val tvRepliedName: TextView = itemView.findViewById(R.id.tvRepliedName)
        val tvRepliedText: TextView = itemView.findViewById(R.id.tvRepliedText)
        val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val isMe = message.senderId == currentUserId

        holder.tvMessageText.text = message.text
        holder.tvMessageTime.text = timeFormat.format(Date(message.timestamp))

        if (message.replyToText.isNotEmpty()) {
            holder.replyLayout.visibility = View.VISIBLE
            holder.tvRepliedName.text = message.replyToName
            holder.tvRepliedText.text = message.replyToText
            if (isMe) {
                holder.tvRepliedName.setTextColor(Color.WHITE)
                holder.tvRepliedText.setTextColor(Color.parseColor("#E0E0E0"))
            } else {
                holder.tvRepliedName.setTextColor(Color.parseColor("#156082"))
                holder.tvRepliedText.setTextColor(Color.parseColor("#616161"))
            }
        } else {
            holder.replyLayout.visibility = View.GONE
        }

        if (isMe) {
            // Mensaje enviado por mí (Alineado a la derecha)
            holder.rootLayout.gravity = Gravity.END or Gravity.BOTTOM
            holder.cvSenderAvatar.visibility = View.GONE
            holder.tvSenderName.visibility = View.GONE
            holder.bubbleLayout.setBackgroundResource(R.drawable.bg_bubble_sent)
            holder.tvMessageText.setTextColor(android.graphics.Color.WHITE)
            holder.tvMessageTime.setTextColor(android.graphics.Color.parseColor("#E0E0E0"))
        } else {
            // Mensaje recibido (Alineado a la izquierda)
            holder.rootLayout.gravity = Gravity.START or Gravity.BOTTOM
            holder.cvSenderAvatar.visibility = View.VISIBLE
            
            // Mostrar nombre del sender solo en grupos (podríamos checar si es grupo, o siempre mostrarlo)
            holder.tvSenderName.visibility = View.VISIBLE
            holder.tvSenderName.text = message.senderName
            
            holder.bubbleLayout.setBackgroundResource(R.drawable.bg_bubble_received)
            holder.tvMessageText.setTextColor(android.graphics.Color.parseColor("#212121"))
            holder.tvMessageTime.setTextColor(android.graphics.Color.parseColor("#757575"))
        }
    }

    override fun getItemCount(): Int = messageList.size
}
