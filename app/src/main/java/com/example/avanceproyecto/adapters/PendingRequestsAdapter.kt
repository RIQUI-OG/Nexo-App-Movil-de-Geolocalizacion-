package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.UserConnection
import com.google.android.material.button.MaterialButton

class PendingRequestsAdapter(
    private val pendingList: List<UserConnection>,
    private val onAcceptClick: (UserConnection) -> Unit,
    private val onDeclineClick: (UserConnection) -> Unit
) : RecyclerView.Adapter<PendingRequestsAdapter.PendingViewHolder>() {

    class PendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPendingUserName: TextView = itemView.findViewById(R.id.tvPendingUserName)
        val tvPendingUserEmail: TextView = itemView.findViewById(R.id.tvPendingUserEmail)
        val tvPendingCategory: TextView = itemView.findViewById(R.id.tvPendingCategory)
        val btnAcceptRequest: MaterialButton = itemView.findViewById(R.id.btnAcceptRequest)
        val btnDeclineRequest: MaterialButton = itemView.findViewById(R.id.btnDeclineRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_request, parent, false)
        return PendingViewHolder(view)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        val request = pendingList[position]

        holder.tvPendingUserName.text = request.connectedUserName
        holder.tvPendingUserEmail.text = request.connectedUserEmail
        holder.tvPendingCategory.text = "Categoría solicitada: ${request.type}"

        holder.btnAcceptRequest.setOnClickListener {
            onAcceptClick(request)
        }

        holder.btnDeclineRequest.setOnClickListener {
            onDeclineClick(request)
        }
    }

    override fun getItemCount(): Int = pendingList.size
}
