package com.example.chat.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Documento `chats/{chatId}`. Los mensajes viven en la subcolección `messages`.
 * `lastMessage*` se guarda aquí para pintar la lista de chats sin leer los mensajes.
 */
data class Chat(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastMessageAt: Date? = null,
    val typing: Map<String, Boolean> = emptyMap(), // uid → está escribiendo ahora
) {
    /** Uid del otro participante en un chat 1 a 1. */
    fun otherUserId(myUid: String): String? = participants.firstOrNull { it != myUid }

    companion object {
        /** Id fijo para el chat entre dos usuarios: así no se crean chats duplicados. */
        fun idFor(uidA: String, uidB: String): String = listOf(uidA, uidB).sorted().joinToString("_")
    }
}
