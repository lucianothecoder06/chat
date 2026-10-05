package com.example.chat.ui.users

import com.example.chat.data.model.User

/**
 * Una fila de la lista: el usuario, cuántos mensajes suyos no has leído y el último mensaje del chat
 * (vacío si todavía no hablaron). `lastFromMe` sirve para anteponer "Tú: ".
 */
data class UserItem(
    val user: User,
    val unread: Int = 0,
    val lastMessage: String = "",
    val lastFromMe: Boolean = false,
)
