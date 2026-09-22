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

class ContactsAdapter(
    private val contacts: List<UserConnection>,
    private val onOptionsClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgPhoto: ImageView = view.findViewById(R.id.imgContactPhoto)
        val name: TextView = view.findViewById(R.id.tvContactName)
        val email: TextView = view.findViewById(R.id.tvContactEmail)
        val type: TextView = view.findViewById(R.id.tvContactType)
        val btnMore: ImageView = view.findViewById(R.id.btnMoreOptions)

        fun bind(connection: UserConnection) {
            name.text = connection.connectedUserName
            email.text = connection.connectedUserEmail

            // Cargar foto de perfil con Glide o mostrar icono por defecto
            if (connection.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(connection.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgPhoto)
            } else {
                imgPhoto.setImageResource(R.drawable.ic_person)
            }

            val isFamily = connection.type.equals("family", ignoreCase = true) ||
                           connection.type.equals("familia", ignoreCase = true)
            val isNino = connection.type.equals("niño", ignoreCase = true)
            val isTutor = connection.type.equals("tutor", ignoreCase = true)

            type.text = when {
                isNino -> "🧒 Niño (Protegido)"
                isTutor -> "🛡️ Tutor"
                isFamily -> "👨‍👩‍👧‍👦 Familiar"
                else -> "👤 Amigo"
            }

            btnMore.setOnClickListener {
                onOptionsClick(connection)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size
}
