package com.example.chat.ui.users

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.chat.R
import com.example.chat.data.model.Chat
import com.example.chat.data.model.User
import com.example.chat.databinding.ActivityUsersBinding
import com.example.chat.notifications.NotificationHelper
import com.example.chat.ui.auth.LoginActivity
import com.example.chat.ui.chat.ChatActivity
import com.example.chat.util.ImageUtils
import com.example.chat.util.Resource
import com.example.chat.util.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pantalla principal: lista de usuarios registrados. Tocar uno abre el chat con esa persona. */
class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private val adapter = UsersAdapter { user -> openChat(user) }

    // Si el usuario niega el permiso la app funciona igual, solo que sin notificaciones
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Selector del sistema, igual que en el chat: no pide permisos
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) savePhoto(uri)
        }

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
        // La foto propia va a la izquierda de la barra; tocarla (o el menú) permite cambiarla
        binding.toolbar.setNavigationOnClickListener { choosePhoto() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_photo -> {
                    choosePhoto()
                    true
                }
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
        viewModel.myPhoto.observe(this) { photo -> showMyPhoto(photo) }
        viewModel.photoError.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }

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

    private fun choosePhoto() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /** Reducir y comprimir la imagen es pesado, así que se hace en un hilo de fondo. */
    private fun savePhoto(uri: Uri) {
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) { ImageUtils.encodeToBase64(contentResolver, uri) }
            if (base64 == null) {
                Snackbar.make(binding.root, R.string.chat_image_error, Snackbar.LENGTH_LONG).show()
            } else {
                viewModel.updatePhoto(base64)
            }
        }
    }

    /** Foto propia recortada en círculo en la barra; sin foto, un ícono de persona. */
    private fun showMyPhoto(photoBase64: String?) {
        val photo = ImageUtils.decodeBase64(photoBase64)
        binding.toolbar.navigationIcon = if (photo == null) {
            ContextCompat.getDrawable(this, R.drawable.ic_account_circle)
        } else {
            val size = resources.getDimensionPixelSize(R.dimen.toolbar_avatar_size)
            val square = ThumbnailUtils.extractThumbnail(photo, size, size) // recorta al centro y reduce
            RoundedBitmapDrawableFactory.create(resources, square).apply { isCircular = true }
        }
        binding.toolbar.navigationContentDescription = getString(R.string.action_photo)
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
