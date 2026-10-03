package com.example.chat.data.repository

import com.example.chat.data.model.User
import android.util.Log
import com.example.chat.util.Resource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Registro, inicio y cierre de sesión con Firebase Auth (correo y contraseña).
 * Al registrarse también crea el documento `users/{uid}` para que aparezca en la lista de usuarios.
 * Las funciones `suspend` se llaman desde `viewModelScope.launch { ... }` en el ViewModel.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) {

    /** Uid del usuario con sesión abierta, o null. Firebase guarda la sesión aunque se cierre la app. */
    val currentUid: String?
        get() = auth.currentUser?.uid

    /** Correo del usuario con sesión abierta, o null. */
    val currentEmail: String?
        get() = auth.currentUser?.email

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
            Resource.Error(errorMessage(e), e)
        }
    }

    suspend fun login(email: String, password: String): Resource<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            Resource.Success(result.user!!.uid)
        } catch (e: Exception) {
            Resource.Error(errorMessage(e), e)
        }
    }

    fun logout() {
        // Se invalida el token de este teléfono para que no le lleguen pushes del usuario que salió.
        // Al volver a entrar, Firebase genera uno nuevo y se guarda con refreshFcmToken().
        messaging.deleteToken()
        auth.signOut()
    }

    /** Pide el token FCM de este teléfono y lo guarda en `users/{uid}`. Se llama al entrar a la app. */
    fun refreshFcmToken() {
        messaging.token.addOnSuccessListener { token ->
            Log.d(TAG, "Token FCM: $token") // útil para mandar una prueba desde la consola
            saveFcmToken(token)
        }
    }

    /**
     * Guarda el token en `users/{uid}.fcmToken`; la Cloud Function (T17) lo lee para saber a qué
     * teléfono mandar el push. Sin sesión no hace nada: el token se guardará al iniciar sesión.
     */
    fun saveFcmToken(token: String) {
        val uid = currentUid ?: return
        db.collection(USERS).document(uid).update(FCM_TOKEN, token)
            .addOnFailureListener { e -> Log.w(TAG, "No se pudo guardar el token FCM", e) }
    }

    /**
     * Traduce la excepción de Firebase a un mensaje en español para el usuario.
     * El orden importa: `FirebaseAuthWeakPasswordException` es hija de
     * `FirebaseAuthInvalidCredentialsException`, por eso va primero.
     */
    private fun errorMessage(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red e intenta de nuevo."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e intenta de nuevo."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usa al menos 6 caracteres."
        is FirebaseAuthInvalidUserException ->
            if (e.errorCode == "ERROR_USER_DISABLED") "Esta cuenta está deshabilitada."
            else "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_EMAIL") "El correo no es válido."
            else "Correo o contraseña incorrectos."
        is FirebaseAuthException ->
            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") "El inicio de sesión con correo no está habilitado en Firebase."
            else GENERIC_ERROR
        else -> GENERIC_ERROR
    }

    companion object {
        const val USERS = "users"
        const val FCM_TOKEN = "fcmToken"
        private const val TAG = "AuthRepository"
        private const val GENERIC_ERROR = "No se pudo completar la operación. Intenta de nuevo."
    }
}
