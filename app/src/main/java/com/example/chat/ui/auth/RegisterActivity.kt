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
import com.example.chat.databinding.ActivityRegisterBinding
import com.example.chat.ui.users.UsersActivity
import com.example.chat.util.Resource

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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
        binding.btnRegister.setOnClickListener { submit() }

        // Tecla "Listo" del teclado en el último campo
        binding.etConfirm.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }

        // "¿Ya tienes cuenta?" vuelve al login, que sigue abajo en la pila
        binding.btnGoLogin.setOnClickListener { finish() }

        // Al corregir un campo se quita su error
        binding.etName.doAfterTextChanged { binding.tilName.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etConfirm.doAfterTextChanged { binding.tilConfirm.error = null }
    }

    private fun submit() {
        viewModel.register(
            binding.etName.text.toString(),
            binding.etEmail.text.toString(),
            binding.etPassword.text.toString(),
            binding.etConfirm.text.toString(),
        )
    }

    private fun observeViewModel() {
        viewModel.nameError.observe(this) { error ->
            binding.tilName.error = error?.let { getString(it) }
        }
        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error?.let { getString(it) }
        }
        viewModel.passwordError.observe(this) { error ->
            binding.tilPassword.error = error?.let { getString(it) }
        }
        viewModel.confirmError.observe(this) { error ->
            binding.tilConfirm.error = error?.let { getString(it) }
        }
        viewModel.registerState.observe(this) { state ->
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
        binding.btnRegister.isEnabled = !loading
        if (loading) binding.tvError.visibility = View.GONE
    }

    private fun goToHome() {
        // Firebase ya dejó la sesión iniciada; se limpia la pila para que "atrás" no vuelva al registro ni al login
        val intent = Intent(this, UsersActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
