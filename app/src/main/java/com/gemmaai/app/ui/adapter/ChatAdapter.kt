package com.gemmaai.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.gemmaai.app.R
import com.gemmaai.app.databinding.ItemMessageAssistantBinding
import com.gemmaai.app.databinding.ItemMessageUserBinding
import com.gemmaai.app.model.ChatMessage
import com.gemmaai.app.model.MessageRole
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

private const val VIEW_TYPE_USER = 0
private const val VIEW_TYPE_ASSISTANT = 1

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).role) {
            MessageRole.USER -> VIEW_TYPE_USER
            else -> VIEW_TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(
                ItemMessageUserBinding.inflate(inflater, parent, false)
            )
            else -> AssistantViewHolder(
                ItemMessageAssistantBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AssistantViewHolder -> holder.bind(message)
        }
    }

    // ─── User Message ViewHolder ──────────────────────────────────────
    class UserViewHolder(private val binding: ItemMessageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.messageText.text = message.text

            if (message.imageBitmap != null) {
                binding.attachedImage.visibility = View.VISIBLE
                binding.attachedImage.setImageBitmap(message.imageBitmap)
            } else {
                binding.attachedImage.visibility = View.GONE
            }

            if (message.text.isBlank()) {
                binding.messageText.visibility = View.GONE
            } else {
                binding.messageText.visibility = View.VISIBLE
            }
        }
    }

    // ─── Assistant Message ViewHolder ────────────────────────────────
    class AssistantViewHolder(private val binding: ItemMessageAssistantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val markwon: Markwon = Markwon.builder(binding.root.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(binding.root.context))
            .build()

        fun bind(message: ChatMessage) {
            if (message.isLoading) {
                binding.typingDots.visibility = View.VISIBLE
                binding.messageText.visibility = View.GONE
                binding.thoughtContainer.visibility = View.GONE
            } else {
                binding.typingDots.visibility = View.GONE
                
                val text = message.text
                val thinkStart = text.indexOf("<think>")
                val thinkEnd = text.indexOf("</think>")
                
                if (thinkStart != -1) {
                    binding.thoughtContainer.visibility = View.VISIBLE
                    val endIdx = if (thinkEnd != -1) thinkEnd else text.length
                    val thoughtText = text.substring(thinkStart + 7, endIdx).trim()
                    binding.thoughtText.text = thoughtText
                    
                    val remainingText = if (thinkEnd != -1) text.substring(thinkEnd + 8).trim() else ""
                    
                    if (remainingText.isNotBlank()) {
                        binding.messageText.visibility = View.VISIBLE
                        markwon.setMarkdown(binding.messageText, remainingText)
                    } else {
                        binding.messageText.visibility = View.GONE
                    }
                    
                    // Click listener for the dropdown toggle
                    binding.thoughtToggleBtn.setOnClickListener {
                        if (binding.thoughtText.visibility == View.VISIBLE) {
                            binding.thoughtText.visibility = View.GONE
                            binding.thoughtToggleIcon.text = "▼"
                        } else {
                            binding.thoughtText.visibility = View.VISIBLE
                            binding.thoughtToggleIcon.text = "▲"
                        }
                    }
                } else {
                    binding.thoughtContainer.visibility = View.GONE
                    binding.messageText.visibility = View.VISIBLE
                    if (text.isNotBlank()) {
                        markwon.setMarkdown(binding.messageText, text)
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) =
            old.text == new.text && old.isLoading == new.isLoading
    }
}
