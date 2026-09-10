package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.UserConnection

class FloatingContactsAdapter(
    private val contactsList: List<UserConnection>,
    private val onContactClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<FloatingContactsAdapter.FloatingViewHolder>() {

    class FloatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFloatingAvatar: TextView = itemView.findViewById(R.id.tvFloatingAvatar)
        val tvFloatingName: TextView = itemView.findViewById(R.id.tvFloatingName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloatingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_floating_contact, parent, false)
        return FloatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: FloatingViewHolder, position: Int) {
        val contact = contactsList[position]

        val shortName = if (contact.connectedUserName.contains(" ")) {
            contact.connectedUserName.split(" ")[0]
        } else {
            contact.connectedUserName
        }

        holder.tvFloatingName.text = shortName
        holder.tvFloatingAvatar.text = if (contact.type.equals("family", ignoreCase = true) || contact.type.equals("familia", ignoreCase = true)) "👨‍👩‍👧‍👦" else "👤"

        holder.itemView.setOnClickListener {
            onContactClick(contact)
        }
    }

    override fun getItemCount(): Int = contactsList.size
}
