package com.example.chat.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de conversación. Por ahora solo define los extras que recibe desde la lista de usuarios;
 * la lista de mensajes y el envío se agregan en la tarea del chat.
 */
class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(EXTRA_OTHER_NAME)
    }

    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        const val EXTRA_OTHER_UID = "otherUid"
        const val EXTRA_OTHER_NAME = "otherName"
    }
}
