package com.example.chat.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.chat.MainActivity
import com.example.chat.ui.auth.LoginActivity

/**
 * Primera pantalla de la app (LAUNCHER). No tiene layout: solo decide a dónde ir y se cierra.
 * Mientras tanto Android muestra su splash del sistema (ícono sobre el fondo del tema).
 * El aviso de lint "CustomSplashScreen" se silencia porque esta Activity no dibuja un splash propio.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Con sesión guardada se entra directo; si no, al login
        val next = if (viewModel.isLoggedIn()) MainActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, next))
        finish() // "atrás" no debe volver al splash
    }
}
