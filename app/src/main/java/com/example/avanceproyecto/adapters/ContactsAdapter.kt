package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.UserConnection

class ContactsAdapter(
    private val contacts: List<UserConnection>,
    private val onOptionsClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val name: TextView = view.findViewById(R.id.tvContactName)
        val email: TextView = view.findViewById(R.id.tvContactEmail)
        val type: TextView = view.findViewById(R.id.tvContactType)
        val btnMore: ImageView = view.findViewById(R.id.btnMoreOptions)

        fun bind(connection: UserConnection) {
            name.text = connection.connectedUserName
            email.text = connection.connectedUserEmail

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
