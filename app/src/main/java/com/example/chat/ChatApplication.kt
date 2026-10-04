package com.example.chat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.chat.util.ThemePreference

/** Se ejecuta una vez al abrir la app, antes que cualquier Activity. */
class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Aplicar el tema guardado aquí evita que la primera pantalla aparezca un instante con el otro tema
        AppCompatDelegate.setDefaultNightMode(ThemePreference.load(this))
    }
}
