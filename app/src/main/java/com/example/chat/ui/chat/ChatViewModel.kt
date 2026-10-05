package com.example.chat.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.model.Message
import com.example.chat.data.repository.AuthRepository
import com.example.chat.data.repository.ChatRepository
import com.example.chat.data.repository.PushRepository
import com.example.chat.util.Resource
import com.example.chat.util.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lógica de una conversación. Los extras del Intent (chatId, otherUid...) llegan en el
 * SavedStateHandle, por eso no hace falta una fábrica propia.
 */
class ChatViewModel @JvmOverloads constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
    private val pushRepository: PushRepository = PushRepository(),
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

    // true mientras la otra persona está escribiendo
    private val _otherTyping = MutableLiveData(false)
    val otherTyping: LiveData<Boolean> = _otherTyping

    // Escuchas de Firestore; se guardan para cancelarlas en onCleared
    private var messagesJob: Job? = null
    private var typingJob: Job? = null

    // Estado de escritura propio: se avisa solo al cambiar, no en cada letra
    private var iAmTyping = false
    private var idleJob: Job? = null

    init {
        if (chatId != null) {
            messagesJob = viewModelScope.launch {
                chatRepository.observeMessages(chatId).collect { _messages.value = it }
            }
            if (otherUid != null) {
                typingJob = viewModelScope.launch {
                    chatRepository.observeTyping(chatId, otherUid).collect { _otherTyping.value = it }
                }
            }
        }
    }

    /** Se llama en cada cambio del campo de texto. Avisa "escribiendo" y lo quita tras 3 s sin teclear. */
    fun onTextChanged(text: String) {
        if (text.isBlank()) {
            stopTyping()
            return
        }
        setTyping(true)
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            delay(TYPING_IDLE_MILLIS)
            setTyping(false)
        }
    }

    /** Quita el aviso (al enviar, al salir de la pantalla o al vaciar el campo). */
    fun stopTyping() {
        idleJob?.cancel()
        setTyping(false)
    }

    private fun setTyping(typing: Boolean) {
        if (iAmTyping == typing) return
        val chat = chatId ?: return
        val me = myUid ?: return
        val other = otherUid ?: return
        iAmTyping = typing
        chatRepository.setTyping(chat, me, other, typing)
    }

    /** Devuelve true si el mensaje era válido y se mandó a enviar (la vista puede limpiar el campo). */
    fun send(text: String): Boolean {
        val error = Validators.validateMessage(text)
        _messageError.value = error
        if (error != null) return false

        val chat = chatId ?: return false
        val sender = myUid ?: return false
        val receiver = otherUid ?: return false
        idleJob?.cancel()
        iAmTyping = false // sendMessage ya deja typing en false
        viewModelScope.launch {
            val result = chatRepository.sendMessage(chat, sender, receiver, text)
            if (result is Resource.Error) _sendError.value = result.message
            else notifyReceiver(chat, sender, receiver, text.trim())
        }
        return true
    }

    /** Manda una imagen ya reducida y en Base64 (ver ImageUtils.encodeToBase64), sin texto. */
    fun sendImage(imageBase64: String) {
        val chat = chatId ?: return
        val sender = myUid ?: return
        val receiver = otherUid ?: return
        viewModelScope.launch {
            val result = chatRepository.sendMessage(chat, sender, receiver, text = "", imageBase64 = imageBase64)
            if (result is Resource.Error) _sendError.value = result.message
            else notifyReceiver(chat, sender, receiver, PHOTO_PREVIEW)
        }
    }

    /**
     * Push al receptor (T17), solo si el mensaje se guardó bien.
     * NonCancellable: si el usuario sale del chat justo después de enviar, el ViewModel se destruye
     * pero el push se termina de mandar igual.
     */
    private suspend fun notifyReceiver(chat: String, sender: String, receiver: String, body: String) {
        withContext(NonCancellable) {
            pushRepository.notifyNewMessage(chat, sender, receiver, body)
        }
    }

    override fun onCleared() {
        // Al cancelar el Flow, ChatRepository quita el snapshot listener (awaitClose)
        messagesJob?.cancel()
        typingJob?.cancel()
        stopTyping() // no es una corrutina: no se cancela con el ViewModel
    }

    private companion object {
        const val TYPING_IDLE_MILLIS = 3000L
        const val PHOTO_PREVIEW = "📷 Foto" // mismo texto que ChatRepository pone en lastMessage
    }
}
