package com.example.avanceproyecto.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.avanceproyecto.R
import com.example.avanceproyecto.utils.RouteOption
import com.example.avanceproyecto.utils.RouteStepDetail

class RouteOptionsAdapter(
    private val optionsList: List<RouteOption>,
    private val onOptionSelected: (RouteOption) -> Unit
) : RecyclerView.Adapter<RouteOptionsAdapter.OptionViewHolder>() {

    private var selectedPosition = 0

    class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRouteOption: CardView = itemView.findViewById(R.id.cardRouteOption)
        val tvTimes: TextView = itemView.findViewById(R.id.tvTimes)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val containerStepsBadges: LinearLayout = itemView.findViewById(R.id.containerStepsBadges)
        val tvStepSummaryFallback: TextView = itemView.findViewById(R.id.tvStepSummaryFallback)
        val tvFareAndDetails: TextView = itemView.findViewById(R.id.tvFareAndDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val option = optionsList[position]
        val isSelected = position == selectedPosition

        // Horarios
        if (option.departureTime.isNotEmpty() && option.arrivalTime.isNotEmpty()) {
            holder.tvTimes.text = "${option.departureTime} — ${option.arrivalTime}"
        } else {
            holder.tvTimes.text = option.summary
        }

        // Duración
        holder.tvDuration.text = option.durationText.ifEmpty { "Ruta ${position + 1}" }

        // Detalles de tarifa y distancia
        val detailsSb = StringBuilder()
        if (option.fareText.isNotEmpty()) {
            detailsSb.append(option.fareText).append(" • ")
        }
        if (option.distanceText.isNotEmpty()) {
            detailsSb.append(option.distanceText)
        }
        holder.tvFareAndDetails.text = if (detailsSb.isNotEmpty()) detailsSb.toString() else option.summary

        // Renderizar Pasos / Insignias de Medios de Transporte
        renderStepBadges(holder, option.stepDetails)

        // Estilos según selección
        if (isSelected) {
            holder.cardRouteOption.setCardBackgroundColor(Color.parseColor("#E1F5FE"))
            holder.cardRouteOption.cardElevation = 8f
            holder.tvDuration.setTextColor(Color.parseColor("#0288D1"))
        } else {
            holder.cardRouteOption.setCardBackgroundColor(Color.WHITE)
            holder.cardRouteOption.cardElevation = 3f
            holder.tvDuration.setTextColor(Color.parseColor("#156082"))
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onOptionSelected(option)
        }
    }

    private fun renderStepBadges(holder: OptionViewHolder, steps: List<RouteStepDetail>) {
        holder.containerStepsBadges.removeAllViews()

        if (steps.isEmpty()) {
            holder.tvStepSummaryFallback.visibility = View.VISIBLE
            holder.tvStepSummaryFallback.text = "🚶 ➔ 🚗 Ruta Directa"
            return
        }

        holder.tvStepSummaryFallback.visibility = View.GONE

        for (i in steps.indices) {
            val step = steps[i]

            // Flecha separadora
            if (i > 0) {
                val arrowTv = TextView(holder.itemView.context).apply {
                    text = " ➔ "
                    setTextColor(Color.parseColor("#757575"))
                    textSize = 12f
                }
                holder.containerStepsBadges.addView(arrowTv)
            }

            // Insignia de Transporte
            val badgeTv = TextView(holder.itemView.context).apply {
                val lineText = if (step.lineName.isNotEmpty()) " ${step.lineName}" else ""
                text = "${step.icon}$lineText"
                textSize = 12f
                setPadding(12, 6, 12, 6)
                gravity = Gravity.CENTER

                if (step.lineName.isNotEmpty()) {
                    val bgDrawable = GradientDrawable().apply {
                        cornerRadius = 10f
                        try {
                            setColor(Color.parseColor(step.lineColorHex))
                        } catch (e: Exception) {
                            setColor(Color.parseColor("#156082"))
                        }
                    }
                    background = bgDrawable
                    setTextColor(Color.WHITE)
                } else {
                    setTextColor(Color.parseColor("#424242"))
                }
            }

            holder.containerStepsBadges.addView(badgeTv)
        }
    }

    fun setSelectedPosition(position: Int) {
        if (position in optionsList.indices && position != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun getItemCount(): Int = optionsList.size
}
