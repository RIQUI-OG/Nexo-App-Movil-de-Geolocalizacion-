package com.example.avanceproyecto.utils

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "🔒 Nexo: Protección Parental Activada (Prevención de desinstalación)", Toast.LENGTH_LONG).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        sendParentalAlert("⚠️ Intento de desactivación de Modo Parental")
        return "⚠️ ATENCIÓN: Desactivar esta opción permitirá desinstalar Nexo Seguridad y se notificará a los padres. ¿Estás seguro?"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "🔓 Nexo: Protección Parental Desactivada", Toast.LENGTH_SHORT).show()
        sendParentalAlert("❌ Modo Parental Desactivado exitosamente desde Ajustes")
    }

    private fun sendParentalAlert(message: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("connections")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("type", "Tutor")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (doc in querySnapshot) {
                    val tutorId = doc.getString("connectedUserId")
                    if (tutorId != null) {
                        val alertData = hashMapOf(
                            "senderId" to currentUserId,
                            "type" to "ParentalControl",
                            "message" to message,
                            "timestamp" to System.currentTimeMillis(),
                            "status" to "active"
                        )
                        firestore.collection("alerts")
                            .document(tutorId)
                            .collection("user_alerts")
                            .add(alertData)
                    }
                }
            }
    }
}