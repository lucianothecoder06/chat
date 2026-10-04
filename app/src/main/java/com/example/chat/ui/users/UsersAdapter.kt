package com.example.chat.ui.users

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.data.model.User
import com.example.chat.databinding.ItemUserBinding

/** Fila de la lista de usuarios. `onClick` se ejecuta al tocar un usuario. */
class UsersAdapter(
    private val onClick: (User) -> Unit,
) : ListAdapter<User, UsersAdapter.UserViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.name
            binding.tvEmail.text = user.email
            binding.root.setOnClickListener { onClick(user) }

            val photo = decodePhoto(user.photoBase64)
            if (photo != null) {
                binding.ivAvatar.setImageBitmap(photo)
                binding.tvInitial.text = ""
            } else {
                binding.ivAvatar.setImageDrawable(null)
                binding.tvInitial.text = user.name.trim().take(1).uppercase()
            }
        }

        private fun decodePhoto(base64: String?) = base64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: IllegalArgumentException) {
                null // Base64 mal formado: se muestra la inicial
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid
            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    }
}
