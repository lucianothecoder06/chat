package com.example.chat.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formatea la hora de los mensajes:
 * hoy → "14:05", ayer → "Ayer 14:05", antes → "28/09/26 14:05".
 * `now` y `timeZone` tienen valor por defecto; los tests los fijan para no depender del reloj.
 */
object DateFormatter {

    fun formatMessageTime(
        timestampMillis: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val dayFormat = formatter("yyyyMMdd", timeZone)
        val messageDay = dayFormat.format(Date(timestampMillis))
        val today = dayFormat.format(Date(now))

        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dayFormat.format(calendar.time)

        val hour = formatter("HH:mm", timeZone).format(Date(timestampMillis))
        return when (messageDay) {
            today -> hour
            yesterday -> "Ayer $hour"
            else -> formatter("dd/MM/yy HH:mm", timeZone).format(Date(timestampMillis))
        }
    }

    private fun formatter(pattern: String, timeZone: TimeZone): SimpleDateFormat {
        val format = SimpleDateFormat(pattern, Locale.forLanguageTag("es"))
        format.timeZone = timeZone
        return format
    }
}
