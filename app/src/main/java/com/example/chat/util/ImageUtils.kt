package com.example.chat.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Las imágenes viajan como texto Base64 dentro de Firestore (no hay Storage).
 * Un documento de Firestore admite 1 MiB y las reglas aceptan hasta ~700 KB de Base64,
 * por eso antes de subir se reduce el tamaño y se comprime en JPEG.
 */
object ImageUtils {

    const val MAX_SIDE = 512 // píxeles del lado más largo
    const val MAX_BASE64_LENGTH = 600_000 // margen bajo el límite de 700 KB de las reglas
    private val QUALITIES = intArrayOf(70, 50, 30)

    /**
     * Lee la imagen de la galería y la devuelve reducida, comprimida y en Base64 (sin saltos de línea).
     * Devuelve null si no se pudo leer o no cabe en el límite. Es trabajo pesado: llamar fuera del hilo principal.
     */
    fun encodeToBase64(resolver: ContentResolver, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // inSampleSize carga la imagen ya reducida: una foto de 12 MP no cabe entera en memoria
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_SIDE)
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null

            val bitmap = rotateByExif(scaleDown(decoded, MAX_SIDE), readOrientation(resolver, uri))
            compress(bitmap)
        } catch (e: Exception) {
            null
        }
    }

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

    /** Mayor potencia de 2 que deja el lado más largo todavía >= `maxSide`. */
    fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun readOrientation(resolver: ContentResolver, uri: Uri): Int =
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

    // Muchas fotos de cámara se guardan "acostadas" y el giro solo viene en los metadatos EXIF
    private fun rotateByExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Prueba calidades cada vez más bajas hasta que el Base64 cabe en el límite. */
    private fun compress(bitmap: Bitmap): String? {
        for (quality in QUALITIES) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (base64.length <= MAX_BASE64_LENGTH) return base64
        }
        return null
    }
}
