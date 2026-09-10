package com.example.avanceproyecto.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.avanceproyecto.ChatListActivity
import com.example.avanceproyecto.MapActivity
import com.example.avanceproyecto.R

object NotificationHelper {

    const val CHANNEL_ALERTS_ID = "nexo_security_alerts_channel"
    const val CHANNEL_CHAT_ID = "nexo_chat_channel"
    const val CHANNEL_SERVICE_ID = "nexo_foreground_service_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para Alertas de Seguridad y Emergencias (Prioridad Máxima)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Alertas de Seguridad y Rutas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de desviaciones de ruta, emergencias e inicio de viajes"
                enableVibration(true)
            }

            // Canal para Mensajes de Chat (Estilo WhatsApp)
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                "Mensajes de Chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos mensajes de chat en tiempo real"
                enableVibration(true)
            }

            // Canal para el Servicio en Segundo Plano
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Servicio de Ubicación Continuo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación permanente de seguimiento seguro de ubicación"
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Muestra una notificación de Chat (Estilo WhatsApp)
     */
    fun showChatNotification(context: Context, senderName: String, messageText: String, chatId: String) {
        createNotificationChannels(context)

        val intent = Intent(context, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Muestra una notificación de Alerta de Seguridad (Desviación / Emergencia / Inicio de Ruta)
     */
    fun showSecurityAlertNotification(context: Context, title: String, message: String) {
        createNotificationChannels(context)

        val intent = Intent(context, MapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.logo_nexo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
