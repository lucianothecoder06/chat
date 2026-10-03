package com.example.chat.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.repository.AuthRepository
import com.example.chat.util.Resource
import com.example.chat.util.Validators
import kotlinx.coroutines.launch

/**
 * Lógica de la pantalla de login: valida el formulario y, si está bien, llama al repositorio.
 * La Activity solo observa estos LiveData; al rotar la pantalla el estado se conserva.
 */
class LoginViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    // Id del texto de error (R.string...) o null si el campo es válido
    private val _emailError = MutableLiveData<Int?>()
    val emailError: LiveData<Int?> = _emailError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    // Resultado del login: Loading, Success(uid) o Error(mensaje en español)
    private val _loginState = MutableLiveData<Resource<String>>()
    val loginState: LiveData<Resource<String>> = _loginState

    fun login(email: String, password: String) {
        if (_loginState.value is Resource.Loading) return // evita doble toque

        val emailErr = Validators.validateEmail(email)
        val passwordErr = Validators.validatePassword(password)
        _emailError.value = emailErr
        _passwordError.value = passwordErr
        if (emailErr != null || passwordErr != null) return

        _loginState.value = Resource.Loading
        viewModelScope.launch {
            _loginState.value = repository.login(email, password)
        }
    }
}
