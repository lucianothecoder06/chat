package com.example.chat.data.repository

import com.example.chat.data.model.Message
import com.example.chat.util.Resource
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Mensajes de una conversación: `chats/{chatId}` y su subcolección `messages`.
 * `sendMessage` es `suspend` (se llama desde `viewModelScope.launch`) y `observeMessages` es un Flow.
 */
class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Guarda el mensaje y actualiza el resumen del chat (`lastMessage...`) en una sola escritura.
     * Si el chat todavía no existe, esta misma escritura lo crea con sus dos participantes.
     * La hora la pone el servidor (`@ServerTimestamp`), no el reloj del teléfono.
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String,
        imageBase64: String? = null,
    ): Resource<Unit> {
        if (text.isBlank() && imageBase64 == null) {
            return Resource.Error("El mensaje no puede estar vacío.")
        }
        return try {
            val chatRef = db.collection(CHATS).document(chatId)
            val messageRef = chatRef.collection(MESSAGES).document()
            val message = Message(senderId = senderId, text = text.trim(), imageBase64 = imageBase64)

            val summary = mapOf(
                PARTICIPANTS to participantsOf(senderId, receiverId),
                LAST_MESSAGE to previewOf(message),
                LAST_SENDER_ID to senderId,
                LAST_MESSAGE_AT to FieldValue.serverTimestamp(),
                TYPING to mapOf(senderId to false), // al enviar deja de "escribir"
            )

            db.batch()
                .set(chatRef, summary, SetOptions.merge())
                .set(messageRef, message)
                .commit()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("No se pudo enviar el mensaje. Intenta de nuevo.", e)
        }
    }

    /**
     * Mensajes del chat en tiempo real, del más antiguo al más nuevo.
     * El listener se quita solo cuando el Flow deja de recolectarse.
     */
    fun observeMessages(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection(CHATS).document(chatId).collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("No se pudieron cargar los mensajes.", error))
                    return@addSnapshotListener
                }
                // ESTIMATE: un mensaje recién enviado aún no tiene hora del servidor (sería null);
                // se usa la hora local para que aparezca de inmediato y en su lugar.
                val messages = snapshot?.toObjects(Message::class.java, ServerTimestampBehavior.ESTIMATE).orEmpty()
                trySend(Resource.Success(messages))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Avisa en `chats/{chatId}.typing.{myUid}` si el usuario está escribiendo.
     * No es `suspend` ni espera respuesta: es un aviso pasajero y si falla no pasa nada.
     * Usa merge, así también funciona antes del primer mensaje (cuando el chat aún no existe).
     */
    fun setTyping(chatId: String, myUid: String, otherUid: String, typing: Boolean) {
        val data = mapOf(
            PARTICIPANTS to participantsOf(myUid, otherUid),
            TYPING to mapOf(myUid to typing),
        )
        db.collection(CHATS).document(chatId).set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "No se pudo actualizar el estado de escritura", e) }
    }

    /** true mientras la otra persona está escribiendo en este chat. */
    fun observeTyping(chatId: String, otherUid: String): Flow<Boolean> = callbackFlow {
        val registration = db.collection(CHATS).document(chatId)
            .addSnapshotListener { snapshot, error ->
                // Un error aquí no debe romper el chat: simplemente no se muestra el indicador
                trySend(error == null && snapshot?.get(FieldPath.of(TYPING, otherUid)) == true)
            }
        awaitClose { registration.remove() }
    }

    // Siempre en el mismo orden (como el id del chat), si no las reglas verían los participantes "cambiados"
    private fun participantsOf(uidA: String, uidB: String): List<String> = listOf(uidA, uidB).sorted()

    /** Texto corto que se guarda en el chat para la lista de conversaciones. */
    private fun previewOf(message: Message): String =
        if (message.text.isNotEmpty()) message.text else "📷 Foto"

    companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
        private const val PARTICIPANTS = "participants"
        private const val LAST_MESSAGE = "lastMessage"
        private const val LAST_SENDER_ID = "lastSenderId"
        private const val LAST_MESSAGE_AT = "lastMessageAt"
        private const val TYPING = "typing"
        private const val TAG = "ChatRepository"
        private const val CREATED_AT = "createdAt"
    }
}
