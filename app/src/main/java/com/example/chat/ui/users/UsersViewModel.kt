package com.example.chat.ui.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.model.User
import com.example.chat.data.repository.AuthRepository
import com.example.chat.data.repository.UserRepository
import com.example.chat.util.Resource
import kotlinx.coroutines.launch

/**
 * Lista de usuarios con los que se puede chatear, más las acciones de la barra
 * (foto de perfil, cerrar sesión).
 */
class UsersViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<User>>>()
    val users: LiveData<Resource<List<User>>> = _users

    // Foto propia en Base64 (o null); se muestra en la barra superior
    private val _myPhoto = MutableLiveData<String?>()
    val myPhoto: LiveData<String?> = _myPhoto

    // Se emite solo si falla guardar la foto; la Activity muestra el texto
    private val _photoError = MutableLiveData<String>()
    val photoError: LiveData<String> = _photoError

    /** Uid del usuario con sesión; se usa para armar el id del chat. */
    val currentUid: String? get() = authRepository.currentUid

    init {
        val uid = authRepository.currentUid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUsers(uid).collect { _users.value = it }
            }
            viewModelScope.launch {
                userRepository.observeUser(uid).collect { _myPhoto.value = it?.photoBase64 }
            }
        }
    }

    /** Recibe la foto ya comprimida; al guardarse, `myPhoto` cambia solo gracias al listener. */
    fun updatePhoto(photoBase64: String) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            val result = userRepository.updatePhoto(uid, photoBase64)
            if (result is Resource.Error) _photoError.value = result.message
        }
    }

    fun refreshFcmToken() {
        authRepository.refreshFcmToken()
    }

    fun logout() {
        authRepository.logout()
    }
}
