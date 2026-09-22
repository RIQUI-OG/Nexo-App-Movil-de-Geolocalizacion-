package com.example.avanceproyecto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.avanceproyecto.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object CustomMapMarkerHelper {

    /**
     * Genera un BitmapDescriptor personalizado para el marcador de Google Maps con la foto de perfil y nombre.
     */
    fun createCustomMarker(
        context: Context,
        name: String,
        photoUrl: String?,
        isFamily: Boolean,
        isEmergency: Boolean,
        isSelf: Boolean = false,
        onMarkerCreated: (BitmapDescriptor) -> Unit
    ) {
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.view_custom_marker, null)

            val tvName = view.findViewById<TextView>(R.id.tvMarkerName)
            val imgPhoto = view.findViewById<ImageView>(R.id.imgMarkerPhoto)
            val cardBorder = view.findViewById<CardView>(R.id.cardMarkerBorder)

            tvName.text = name

            val borderColor = when {
                isEmergency -> Color.parseColor("#D32F2F") // Rojo Emergencia
                isSelf -> Color.parseColor("#156082")      // Azul Propio
                isFamily -> Color.parseColor("#2E7D32")    // Verde Familia
                else -> Color.parseColor("#EF6C00")        // Naranja Amigo
            }

            cardBorder.setCardBackgroundColor(borderColor)
            tvName.backgroundTintList = android.content.res.ColorStateList.valueOf(borderColor)

            val renderAndReturn = {
                try {
                    view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                    val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    view.draw(canvas)

                    onMarkerCreated(BitmapDescriptorFactory.fromBitmap(bitmap))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            imgPhoto.setImageBitmap(resource)
                            renderAndReturn()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imgPhoto.setImageResource(R.drawable.ic_person)
                            renderAndReturn()
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            imgPhoto.setImageResource(R.drawable.ic_person)
                            renderAndReturn()
                        }
                    })
            } else {
                imgPhoto.setImageResource(R.drawable.ic_person)
                renderAndReturn()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
