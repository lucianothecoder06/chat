package com.example.chat.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Documento `chats/{chatId}/messages/{id}`. Puede llevar texto, una imagen en Base64 o ambos. */
data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageBase64: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
)
