package com.example.chat.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Documento `users/{uid}`. El id del documento es el uid de Firebase Auth.
 * Todos los campos tienen valor por defecto porque Firestore necesita un constructor vacío.
 */
data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoBase64: String? = null, // foto de perfil comprimida, sin Storage
    val fcmToken: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
)
