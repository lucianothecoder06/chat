package com.example.chat.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateFormatterTest {

    private val tz = TimeZone.getTimeZone("America/Santiago")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private val now = at(2026, 10, 1, 18, 30)

    @Test
    fun mismoDia_muestraSoloHora() {
        assertEquals("09:05", DateFormatter.formatMessageTime(at(2026, 10, 1, 9, 5), now, tz))
    }

    @Test
    fun diaAnterior_muestraAyer() {
        assertEquals("Ayer 23:59", DateFormatter.formatMessageTime(at(2026, 9, 30, 23, 59), now, tz))
    }

    @Test
    fun masAntiguo_muestraFechaCompleta() {
        assertEquals("28/09/26 14:00", DateFormatter.formatMessageTime(at(2026, 9, 28, 14, 0), now, tz))
    }
}
