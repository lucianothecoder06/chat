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

/** Lista de usuarios con los que se puede chatear, más las acciones de la barra (cerrar sesión). */
class UsersViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<User>>>()
    val users: LiveData<Resource<List<User>>> = _users

    /** Uid del usuario con sesión; se usa para armar el id del chat. */
    val currentUid: String? get() = authRepository.currentUid

    init {
        val uid = authRepository.currentUid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUsers(uid).collect { _users.value = it }
            }
        }
    }

    fun refreshFcmToken() {
        authRepository.refreshFcmToken()
    }

    fun logout() {
        authRepository.logout()
    }
}
