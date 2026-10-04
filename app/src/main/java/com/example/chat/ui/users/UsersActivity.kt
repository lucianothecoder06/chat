package com.example.chat.ui.users

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.chat.R
import com.example.chat.data.model.Chat
import com.example.chat.data.model.User
import com.example.chat.databinding.ActivityUsersBinding
import com.example.chat.notifications.NotificationHelper
import com.example.chat.ui.auth.LoginActivity
import com.example.chat.ui.chat.ChatActivity
import com.example.chat.util.Resource
import com.example.chat.util.ThemePreference

/** Pantalla principal: lista de usuarios registrados. Tocar uno abre el chat con esa persona. */
class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private val adapter = UsersAdapter { user -> openChat(user) }

    // Si el usuario niega el permiso la app funciona igual, solo que sin notificaciones
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_theme -> {
                    showThemeDialog()
                    true
                }
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        NotificationHelper.createChannel(this)
        askNotificationPermission()
        viewModel.refreshFcmToken()

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.users.observe(this) { state ->
            binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
            when (state) {
                is Resource.Loading -> binding.tvMessage.visibility = View.GONE
                is Resource.Success -> {
                    adapter.submitList(state.data)
                    binding.tvMessage.setText(R.string.users_empty)
                    binding.tvMessage.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.tvMessage.text = state.message
                    binding.tvMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    /** Intent explícito al chat con el id de la conversación y los datos de la otra persona. */
    private fun openChat(user: User) {
        val myUid = viewModel.currentUid ?: return
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, Chat.idFor(myUid, user.uid))
        intent.putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
        intent.putExtra(ChatActivity.EXTRA_OTHER_NAME, user.name)
        startActivity(intent)
    }

    /** Desde Android 13 (API 33) mostrar notificaciones requiere pedir permiso en tiempo de ejecución. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Diálogo con las 3 opciones de tema; la opción actual aparece marcada. */
    private fun showThemeDialog() {
        val current = ThemePreference.MODES.indexOf(ThemePreference.load(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_theme)
            .setSingleChoiceItems(R.array.theme_options, current) { dialog, which ->
                dialog.dismiss()
                ThemePreference.save(this, ThemePreference.MODES[which])
            }
            .show()
    }

    private fun logout() {
        viewModel.logout()
        // Se limpia la pila para que "atrás" no regrese a una pantalla con sesión
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
