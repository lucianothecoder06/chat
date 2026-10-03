package com.example.chat.notifications

import com.example.chat.R
import com.example.chat.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe los mensajes de Firebase Cloud Messaging.
 * Android lo arranca solo (está declarado en el manifest), aunque la app esté cerrada.
 */
class ChatMessagingService : FirebaseMessagingService() {

    /** Firebase generó un token nuevo para este teléfono (primera vez, reinstalación, etc.). */
    override fun onNewToken(token: String) {
        AuthRepository().saveFcmToken(token)
    }

    /**
     * Llega un push. Con mensajes "data" (los que manda la Cloud Function de T17) se llama siempre,
     * con la app abierta o cerrada. Los de la consola ("notification") con la app cerrada los muestra
     * Android solo, usando el canal e ícono por defecto del manifest.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.notification_default_title)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        NotificationHelper.showMessage(this, title, body, message.data["senderId"])
    }
}
