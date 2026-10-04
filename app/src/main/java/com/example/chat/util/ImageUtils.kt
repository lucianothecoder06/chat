package com.example.chat.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/** Las imágenes viajan como texto Base64 dentro de Firestore (no hay Storage). */
object ImageUtils {

    /** Convierte el Base64 guardado en un Bitmap; devuelve null si no hay imagen o está mal formada. */
    fun decodeBase64(base64: String?): Bitmap? {
        if (base64.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
