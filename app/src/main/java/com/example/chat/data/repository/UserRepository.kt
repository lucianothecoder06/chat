package com.example.chat.data.repository

import com.example.chat.data.model.User
import com.example.chat.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Lectura de los perfiles guardados en `users/{uid}`. */
class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Lista de usuarios en tiempo real, sin incluir a `myUid` y ordenada por nombre.
     * El listener se quita solo cuando el Flow deja de recolectarse.
     */
    fun observeUsers(myUid: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection(AuthRepository.USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("No se pudo cargar la lista de usuarios.", error))
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects<User>().orEmpty()
                    .filter { it.uid != myUid }
                    .sortedBy { it.name.lowercase() }
                trySend(Resource.Success(users))
            }
        awaitClose { registration.remove() }
    }
}
