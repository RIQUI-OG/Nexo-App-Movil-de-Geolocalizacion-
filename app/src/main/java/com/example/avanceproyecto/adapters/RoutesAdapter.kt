package com.example.avanceproyecto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.models.Route
import com.google.android.material.button.MaterialButton

class RoutesAdapter(
    private val routesList: List<Route>,
    private val onStartClick: (Route) -> Unit,
    private val onDeleteClick: (Route) -> Unit
) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTransportIcon: TextView = itemView.findViewById(R.id.tvTransportIcon)
        val tvRouteName: TextView = itemView.findViewById(R.id.tvRouteName)
        val tvTransportMode: TextView = itemView.findViewById(R.id.tvTransportMode)
        val tvRouteDetails: TextView = itemView.findViewById(R.id.tvRouteDetails)
        val btnStartRoute: MaterialButton = itemView.findViewById(R.id.btnStartRoute)
        val btnDeleteRoute: ImageView = itemView.findViewById(R.id.btnDeleteRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routesList[position]

        holder.tvRouteName.text = route.routeName
        holder.tvTransportMode.text = "Modo: ${route.transportMode}"

        val icon = when (route.transportMode.lowercase()) {
            "caminando", "walking" -> "🚶"
            "auto", "driving" -> "🚗"
            "bicicleta", "bicycling" -> "🚴"
            "transporte público", "transporte", "transit" -> "🚌"
            else -> "🗺️"
        }
        holder.tvTransportIcon.text = icon

        val startStr = if (route.startName.isNotEmpty()) route.startName else "Inicio"
        val endStr = if (route.endName.isNotEmpty()) route.endName else "Destino"
        holder.tvRouteDetails.text = "📍 $startStr ➔ 🏁 $endStr"

        holder.btnStartRoute.setOnClickListener {
            onStartClick(route)
        }

        holder.btnDeleteRoute.setOnClickListener {
            onDeleteClick(route)
        }
    }

    override fun getItemCount(): Int = routesList.size
}
