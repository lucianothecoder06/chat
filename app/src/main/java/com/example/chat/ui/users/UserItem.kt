package com.example.chat.ui.users

import com.example.chat.data.model.User

/** Una fila de la lista: el usuario y cuántos mensajes suyos no has leído. */
data class UserItem(val user: User, val unread: Int = 0)
