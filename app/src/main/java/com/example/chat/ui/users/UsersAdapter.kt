package com.example.chat.ui.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.R
import com.example.chat.data.model.User
import com.example.chat.databinding.ItemUserBinding
import com.example.chat.util.ImageUtils

/** Fila de la lista de usuarios. `onClick` se ejecuta al tocar un usuario. */
class UsersAdapter(
    private val onClick: (User) -> Unit,
) : ListAdapter<UserItem, UsersAdapter.UserViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserItem) {
            val user = item.user
            binding.tvName.text = user.name
            // Con conversación se ve el último mensaje; si no, el correo para reconocer al usuario
            binding.tvEmail.text = when {
                item.lastMessage.isEmpty() -> user.email
                item.lastFromMe -> binding.root.context.getString(R.string.users_last_from_me, item.lastMessage)
                else -> item.lastMessage
            }
            binding.root.setOnClickListener { onClick(user) }

            binding.tvBadge.text = if (item.unread > MAX_BADGE) "$MAX_BADGE+" else item.unread.toString()
            binding.tvBadge.visibility = if (item.unread > 0) View.VISIBLE else View.GONE

            val photo = ImageUtils.decodeBase64(user.photoBase64)
            if (photo != null) {
                binding.ivAvatar.setImageBitmap(photo)
                binding.tvInitial.text = ""
            } else {
                binding.ivAvatar.setImageDrawable(null)
                binding.tvInitial.text = user.name.trim().take(1).uppercase()
            }
        }
    }

    private companion object {
        const val MAX_BADGE = 99

        val DIFF = object : DiffUtil.ItemCallback<UserItem>() {
            override fun areItemsTheSame(old: UserItem, new: UserItem) = old.user.uid == new.user.uid
            override fun areContentsTheSame(old: UserItem, new: UserItem) = old == new
        }
    }
}
