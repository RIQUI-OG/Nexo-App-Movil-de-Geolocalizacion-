package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.UserConnection

class ContactsAdapter(
    private val contacts: List<UserConnection>,
    private val onContactClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvContactEmail)
        val tvType: TextView = itemView.findViewById(R.id.tvContactType)

        fun bind(connection: UserConnection) {
            tvName.text = connection.connectedUserName
            tvEmail.text = connection.connectedUserEmail
            tvType.text = if (connection.type == "family") "👨‍👩‍👧‍👦 Familiar" else "👤 Amigo"

            itemView.setOnClickListener {
                onContactClick(connection)
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