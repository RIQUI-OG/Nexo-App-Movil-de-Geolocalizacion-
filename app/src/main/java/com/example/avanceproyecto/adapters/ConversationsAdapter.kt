package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.ChatConversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationsAdapter(
    private val conversationsList: List<ChatConversation>,
    private val currentUserId: String,
    private val onConversationClick: (ChatConversation) -> Unit,
    private val onConversationLongClick: (ChatConversation) -> Unit,
    private val resolveContactName: (String) -> String
) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgConversationAvatar: ImageView = itemView.findViewById(R.id.imgConversationAvatar)
        val tvAvatarIcon: TextView = itemView.findViewById(R.id.tvAvatarIcon)
        val tvConversationTitle: TextView = itemView.findViewById(R.id.tvConversationTitle)
        val tvConversationTime: TextView = itemView.findViewById(R.id.tvConversationTime)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)
        val tvGroupMembers: TextView = itemView.findViewById(R.id.tvGroupMembers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = conversationsList[position]

        val titleToDisplay = if (!item.isGroup) {
            val otherUserId = item.participants?.find { it != currentUserId }
                ?: item.chatId.replace("chat_", "").replace("_", "").replace(currentUserId, "")
            val resolvedName = resolveContactName(otherUserId)
            if (resolvedName.isNotEmpty() && resolvedName != "Usuario") resolvedName else item.title
        } else {
            item.title
        }

        holder.tvConversationTitle.text = titleToDisplay.ifEmpty { "Chat" }
        holder.tvLastMessage.text = item.lastMessage.ifEmpty { "Sin mensajes aún" }
        holder.tvConversationTime.text = if (item.lastMessageTime > 0) timeFormat.format(Date(item.lastMessageTime)) else ""

        if (item.isGroup) {
            holder.imgConversationAvatar.visibility = View.GONE
            holder.tvAvatarIcon.visibility = View.VISIBLE
            holder.tvAvatarIcon.text = "👨‍👩‍👧‍👦"
        } else {
            holder.tvAvatarIcon.visibility = View.GONE
            holder.imgConversationAvatar.visibility = View.VISIBLE

            if (item.photoUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(item.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.imgConversationAvatar)
            } else {
                holder.imgConversationAvatar.setImageResource(R.drawable.ic_person)
            }
        }

        val unreadCount = item.unreadCounts[currentUserId] ?: 0
        if (unreadCount > 0) {
            holder.tvUnreadCount.visibility = View.VISIBLE
            holder.tvUnreadCount.text = unreadCount.toString()
        } else {
            holder.tvUnreadCount.visibility = View.GONE
        }

        val participantsList = item.participants
        if (item.isGroup && !participantsList.isNullOrEmpty()) {
            holder.tvGroupMembers.visibility = View.VISIBLE
            val memberNames = participantsList.map { resolveContactName(it) }.joinToString(", ")
            holder.tvGroupMembers.text = memberNames
        } else {
            holder.tvGroupMembers.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val finalConversation = item.copy(title = titleToDisplay)
            onConversationClick(finalConversation)
        }

        holder.itemView.setOnLongClickListener {
            onConversationLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = conversationsList.size
}
