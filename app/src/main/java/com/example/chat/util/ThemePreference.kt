package com.example.chat.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * Tema elegido por el usuario: según el sistema, claro u oscuro.
 * Se guarda en SharedPreferences porque AppCompat no lo recuerda al cerrar la app.
 */
object ThemePreference {

    private const val PREFS = "settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    /** En el mismo orden que el arreglo `theme_options` de strings.xml. */
    val MODES = intArrayOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
    )

    fun load(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /** Guarda el modo y lo aplica: AppCompat recrea las Activities abiertas con los colores nuevos. */
    fun save(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt(KEY_NIGHT_MODE, mode)
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
