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
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.example.chat.R
import com.example.chat.ui.chat.ChatActivity
import com.example.chat.ui.splash.SplashActivity

/** Crea el canal de notificaciones y muestra la notificación de un mensaje nuevo. */
object NotificationHelper {

    const val CHANNEL_ID = "messages"

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

    fun showMessage(context: Context, title: String, body: String, senderId: String?, chatId: String?) {
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

        val pendingIntent = chatPendingIntent(context, notificationId, title, senderId, chatId)

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

    /**
     * Al tocar la notificación se abre el chat con quien escribió. La pila lleva la lista de usuarios
     * debajo (parentActivityName en el manifest), así "atrás" no saca de la app. Si el push no trae
     * los datos del chat, solo se abre la app (el Splash decide si hay sesión).
     */
    private fun chatPendingIntent(
        context: Context,
        requestCode: Int,
        senderName: String,
        senderId: String?,
        chatId: String?,
    ): PendingIntent {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        if (senderId == null || chatId == null) {
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return PendingIntent.getActivity(context, requestCode, intent, flags)
        }
        val chatIntent = Intent(context, ChatActivity::class.java)
        chatIntent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId)
        chatIntent.putExtra(ChatActivity.EXTRA_OTHER_UID, senderId)
        chatIntent.putExtra(ChatActivity.EXTRA_OTHER_NAME, senderName)
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(chatIntent)
            .getPendingIntent(requestCode, flags)!!
    }
}
