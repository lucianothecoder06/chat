package com.example.chat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.chat.MainActivity
import com.example.chat.databinding.ActivityLoginBinding
import com.example.chat.util.Resource

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { submit() }

        // Tecla "Listo" del teclado en el campo de contraseña
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }

        // Al corregir un campo se quita su error
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
    }

    private fun submit() {
        viewModel.login(
            binding.etEmail.text.toString(),
            binding.etPassword.text.toString(),
        )
    }

    private fun observeViewModel() {
        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error?.let { getString(it) }
        }
        viewModel.passwordError.observe(this) { error ->
            binding.tilPassword.error = error?.let { getString(it) }
        }
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> goToHome()
                is Resource.Error -> {
                    showLoading(false)
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        if (loading) binding.tvError.visibility = View.GONE
    }

    private fun goToHome() {
        // Se limpia la pila para que "atrás" no regrese al login
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
