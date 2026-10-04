package com.example.chat.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.R
import com.example.chat.data.model.Message
import com.example.chat.util.DateFormatter
import com.example.chat.util.ImageUtils

/**
 * Burbujas del chat: las propias a la derecha y las del otro a la izquierda (dos layouts distintos).
 * Cada burbuja muestra el nombre de quien escribió, el texto o la imagen, y la hora.
 */
class MessageAdapter(
    private val myUid: String,
    private val otherName: String,
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == myUid) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == TYPE_SENT) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, isSent = message.senderId == myUid)
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSender: TextView = view.findViewById(R.id.tvSender)
        private val tvText: TextView = view.findViewById(R.id.tvText)
        private val ivImage: ImageView = view.findViewById(R.id.ivImage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(message: Message, isSent: Boolean) {
            tvSender.text = if (isSent) itemView.context.getString(R.string.chat_you) else otherName

            tvText.text = message.text
            tvText.visibility = if (message.text.isEmpty()) View.GONE else View.VISIBLE

            val image = ImageUtils.decodeBase64(message.imageBase64)
            ivImage.setImageBitmap(image)
            ivImage.visibility = if (image == null) View.GONE else View.VISIBLE

            tvTime.text = message.createdAt?.let { DateFormatter.formatMessageTime(it.time) }.orEmpty()
        }
    }

    private companion object {
        const val TYPE_SENT = 0
        const val TYPE_RECEIVED = 1

        val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(old: Message, new: Message) = old.id == new.id
            override fun areContentsTheSame(old: Message, new: Message) = old == new
        }
    }
}
