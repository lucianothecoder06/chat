package com.example.chat.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.model.User
import com.example.chat.data.repository.AuthRepository
import com.example.chat.util.Resource
import com.example.chat.util.Validators
import kotlinx.coroutines.launch

/**
 * Lógica de la pantalla de registro. Igual que LoginViewModel, pero con nombre
 * y confirmación de contraseña. Al registrarse, Firebase deja la sesión iniciada.
 */
class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    // Id del texto de error (R.string...) o null si el campo es válido
    private val _nameError = MutableLiveData<Int?>()
    val nameError: LiveData<Int?> = _nameError

    private val _emailError = MutableLiveData<Int?>()
    val emailError: LiveData<Int?> = _emailError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    private val _confirmError = MutableLiveData<Int?>()
    val confirmError: LiveData<Int?> = _confirmError

    // Resultado del registro: Loading, Success(usuario creado) o Error(mensaje en español)
    private val _registerState = MutableLiveData<Resource<User>>()
    val registerState: LiveData<Resource<User>> = _registerState

    fun register(name: String, email: String, password: String, confirmation: String) {
        if (_registerState.value is Resource.Loading) return // evita doble toque

        val nameErr = Validators.validateName(name)
        val emailErr = Validators.validateEmail(email)
        val passwordErr = Validators.validatePassword(password)
        val confirmErr = Validators.validatePasswordConfirmation(password, confirmation)
        _nameError.value = nameErr
        _emailError.value = emailErr
        _passwordError.value = passwordErr
        _confirmError.value = confirmErr
        if (nameErr != null || emailErr != null || passwordErr != null || confirmErr != null) return

        _registerState.value = Resource.Loading
        viewModelScope.launch {
            _registerState.value = repository.register(name, email, password)
        }
    }
}
