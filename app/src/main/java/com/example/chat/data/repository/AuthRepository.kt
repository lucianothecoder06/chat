package com.example.chat.data.repository

import com.example.chat.data.model.User
import com.example.chat.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Registro, inicio y cierre de sesión con Firebase Auth (correo y contraseña).
 * Al registrarse también crea el documento `users/{uid}` para que aparezca en la lista de usuarios.
 * Las funciones `suspend` se llaman desde `viewModelScope.launch { ... }` en el ViewModel.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /** Uid del usuario con sesión abierta, o null. Firebase guarda la sesión aunque se cierre la app. */
    val currentUid: String?
        get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user!!.uid
            val user = User(uid = uid, name = name.trim(), email = email.trim())
            try {
                db.collection(USERS).document(uid).set(user).await()
            } catch (e: Exception) {
                // Sin perfil en Firestore la cuenta queda a medias: se borra para poder reintentar
                result.user?.delete()
                throw e
            }
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(GENERIC_ERROR, e)
        }
    }

    suspend fun login(email: String, password: String): Resource<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            Resource.Success(result.user!!.uid)
        } catch (e: Exception) {
            Resource.Error(GENERIC_ERROR, e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        const val USERS = "users"

        // T06 reemplaza este texto por mensajes en español según el código de error de Firebase
        private const val GENERIC_ERROR = "No se pudo completar la operación. Intenta de nuevo."
    }
}
