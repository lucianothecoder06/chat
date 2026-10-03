package com.example.chat.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.chat.R
import com.example.chat.ui.splash.SplashActivity

/** Crea el canal de notificaciones y muestra la notificación de un mensaje nuevo. */
object NotificationHelper {

    const val CHANNEL_ID = "messages"
    const val EXTRA_SENDER_ID = "senderId"

    /** Desde Android 8 (API 26) toda notificación necesita un canal. Crearlo dos veces no hace nada. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH, // sonido y aviso flotante
            )
            channel.description = context.getString(R.string.notification_channel_description)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showMessage(context: Context, title: String, body: String, senderId: String?) {
        // Sin permiso (Android 13+) no se puede mostrar nada
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)

        // Un id por remitente: varios mensajes de la misma persona reemplazan la notificación anterior
        val notificationId = (senderId ?: "chat").hashCode()

        // Al tocarla se abre la app (el Splash decide si hay sesión). Persona B puede leer
        // EXTRA_SENDER_ID para abrir directo el chat con quien escribió.
        val intent = Intent(context, SplashActivity::class.java)
        intent.putExtra(EXTRA_SENDER_ID, senderId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // texto largo completo al expandir
            .setPriority(NotificationCompat.PRIORITY_HIGH) // para Android 7 (sin canales)
            .setAutoCancel(true) // se quita al tocarla
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
