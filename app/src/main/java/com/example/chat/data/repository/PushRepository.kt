package com.example.chat.data.repository

import android.util.Base64
import android.util.Log
import com.example.chat.BuildConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Envía el push de "mensaje nuevo" al teléfono del receptor con la API v1 de FCM.
 *
 * Lo normal sería hacerlo en una Cloud Function, pero eso exige el plan Blaze de Firebase.
 * Por eso lo envía la app de quien escribe, firmando con una cuenta de servicio que SOLO tiene
 * el rol "Firebase Cloud Messaging API Admin": si alguien extrae la llave del APK, solo puede
 * mandar notificaciones, no leer ni borrar datos.
 */
class PushRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /** No lanza excepciones: si el push falla, el mensaje ya quedó guardado y el chat sigue igual. */
    suspend fun notifyNewMessage(chatId: String, senderId: String, receiverId: String, body: String) {
        val account = serviceAccount
        if (account == null) {
            Log.w(TAG, "Falta app/fcm-service-account.json: no se envía el push")
            return
        }
        try {
            val receiverToken = db.collection(AuthRepository.USERS).document(receiverId)
                .get().await().getString(AuthRepository.FCM_TOKEN)
            if (receiverToken == null) {
                Log.w(TAG, "El receptor no tiene token FCM (nunca abrió la app o cerró sesión)")
                return
            }
            val senderName = db.collection(AuthRepository.USERS).document(senderId)
                .get().await().getString("name").orEmpty()

            // Red: nunca en el hilo principal
            withContext(Dispatchers.IO) {
                send(account, receiverToken, senderName, body, senderId, chatId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo enviar el push", e)
        }
    }

    private fun send(
        account: ServiceAccount,
        token: String,
        title: String,
        body: String,
        senderId: String,
        chatId: String,
    ) {
        // Mensaje "data": así ChatMessagingService.onMessageReceived se llama siempre,
        // con la app abierta o cerrada, y arma la notificación con NotificationHelper (T16)
        val data = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("senderId", senderId)
            .put("chatId", chatId)
        val message = JSONObject()
            .put("token", token)
            .put("data", data)
            .put("android", JSONObject().put("priority", "high")) // despierta al teléfono
        val payload = JSONObject().put("message", message).toString()

        // Token de acceso de Google (dura 1 hora; la librería lo renueva solo cuando vence)
        account.credentials.refreshIfExpired()
        val accessToken = account.credentials.accessToken.tokenValue

        val url = URL("https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(payload.toByteArray()) }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "FCM respondió $code: $error")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Datos que salen del JSON de la cuenta de servicio. */
    private class ServiceAccount(val projectId: String, val credentials: GoogleCredentials)

    companion object {
        private const val TAG = "PushRepository"
        private const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

        /**
         * Se lee una sola vez y se comparte, así el token de acceso se reutiliza entre mensajes.
         * Es null si la app se compiló sin el archivo de la llave.
         */
        private val serviceAccount: ServiceAccount? by lazy {
            if (BuildConfig.FCM_SERVICE_ACCOUNT.isEmpty()) return@lazy null
            val json = Base64.decode(BuildConfig.FCM_SERVICE_ACCOUNT, Base64.DEFAULT)
            ServiceAccount(
                projectId = JSONObject(String(json)).getString("project_id"),
                credentials = GoogleCredentials.fromStream(json.inputStream()).createScoped(FCM_SCOPE),
            )
        }
    }
}
