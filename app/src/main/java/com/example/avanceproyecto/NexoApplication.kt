package com.example.avanceproyecto

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class NexoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Habilitar persistencia offline para que los chats y rutas carguen más rápido y funcionen sin internet
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}