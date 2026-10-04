package com.example.chat.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.model.Message
import com.example.chat.data.repository.AuthRepository
import com.example.chat.data.repository.ChatRepository
import com.example.chat.util.Resource
import com.example.chat.util.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lógica de una conversación. Los extras del Intent (chatId, otherUid...) llegan en el
 * SavedStateHandle, por eso no hace falta una fábrica propia.
 */
class ChatViewModel @JvmOverloads constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
) : ViewModel() {

    val myUid: String? = authRepository.currentUid
    val chatId: String? = savedStateHandle[ChatActivity.EXTRA_CHAT_ID]
    val otherUid: String? = savedStateHandle[ChatActivity.EXTRA_OTHER_UID]
    val otherName: String = savedStateHandle[ChatActivity.EXTRA_OTHER_NAME] ?: ""

    private val _messages = MutableLiveData<Resource<List<Message>>>()
    val messages: LiveData<Resource<List<Message>>> = _messages

    // Id del texto de error (R.string...) si el mensaje es inválido, o null
    private val _messageError = MutableLiveData<Int?>()
    val messageError: LiveData<Int?> = _messageError

    // Se emite solo si falla el envío; la Activity muestra el texto
    private val _sendError = MutableLiveData<String>()
    val sendError: LiveData<String> = _sendError

    // Escucha de Firestore; se guarda para cancelarla en onCleared
    private var messagesJob: Job? = null

    init {
        if (chatId != null) {
            messagesJob = viewModelScope.launch {
                chatRepository.observeMessages(chatId).collect { _messages.value = it }
            }
        }
    }

    /** Devuelve true si el mensaje era válido y se mandó a enviar (la vista puede limpiar el campo). */
    fun send(text: String): Boolean {
        val error = Validators.validateMessage(text)
        _messageError.value = error
        if (error != null) return false

        val chat = chatId ?: return false
        val sender = myUid ?: return false
        val receiver = otherUid ?: return false
        viewModelScope.launch {
            val result = chatRepository.sendMessage(chat, sender, receiver, text)
            if (result is Resource.Error) _sendError.value = result.message
        }
        return true
    }

    override fun onCleared() {
        // Al cancelar el Flow, ChatRepository quita el snapshot listener (awaitClose)
        messagesJob?.cancel()
    }
}
