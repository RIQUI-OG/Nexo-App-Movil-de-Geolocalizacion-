package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.UserConnection

class FloatingContactsAdapter(
    private val contactsList: List<UserConnection>,
    private val onContactClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<FloatingContactsAdapter.FloatingViewHolder>() {

    class FloatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFloatingAvatar: ImageView = itemView.findViewById(R.id.imgFloatingAvatar)
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

        // Cargar foto de perfil con Glide o imagen por defecto
        if (contact.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(contact.photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(holder.imgFloatingAvatar)
        } else {
            holder.imgFloatingAvatar.setImageResource(R.drawable.ic_person)
        }

        holder.itemView.setOnClickListener {
            onContactClick(contact)
        }
    }

    override fun getItemCount(): Int = contactsList.size
}
