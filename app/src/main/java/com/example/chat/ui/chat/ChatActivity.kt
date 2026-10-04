package com.example.chat.ui.chat

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.R
import com.example.chat.databinding.ActivityChatBinding
import com.example.chat.util.Resource
import com.google.android.material.snackbar.Snackbar

/** Conversación con una persona. Recibe los extras desde la lista de usuarios. */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sin sesión o sin los extras no hay conversación que mostrar
        val myUid = viewModel.myUid
        if (myUid == null || viewModel.chatId == null || viewModel.otherUid == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.title = viewModel.otherName
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter(myUid, viewModel.otherName)
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener { submit() }
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }

        observeViewModel()
    }

    private fun submit() {
        if (viewModel.send(binding.etMessage.text.toString())) {
            binding.etMessage.text?.clear()
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { state ->
            binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
            when (state) {
                is Resource.Loading -> binding.tvEmpty.visibility = View.GONE
                is Resource.Success -> {
                    // El callback corre cuando la lista ya se aplicó: ahí se baja al último mensaje
                    adapter.submitList(state.data) {
                        if (state.data.isNotEmpty()) binding.rvMessages.scrollToPosition(state.data.size - 1)
                    }
                    binding.tvEmpty.setText(R.string.chat_empty)
                    binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.tvEmpty.text = state.message
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        }
        viewModel.messageError.observe(this) { error ->
            binding.tilMessage.error = error?.let { getString(it) }
        }
        viewModel.sendError.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        const val EXTRA_OTHER_UID = "otherUid"
        const val EXTRA_OTHER_NAME = "otherName"
    }
}
