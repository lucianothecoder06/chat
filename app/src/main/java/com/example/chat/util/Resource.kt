package com.example.chat.util

/**
 * Estado de una operación asíncrona (Firebase) que el ViewModel expone a la vista.
 * La Activity hace un `when` sobre esto: mostrar progreso, pintar datos o mostrar el error.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
}
