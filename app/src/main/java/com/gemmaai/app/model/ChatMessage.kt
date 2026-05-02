package com.gemmaai.app.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Represents a single chat message.
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: MessageRole,
    val text: String,
    val imageUri: Uri? = null,
    val imageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * Holds all user-customizable settings for the AI persona.
 */
data class AiPersona(
    val name: String = "Aura",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 1024,
    val topK: Int = 40,
    val topP: Float = 0.95f
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are Aura, a helpful, creative and friendly AI assistant running locally on this device. 
You are powered by Google's Gemma 4 model.
IMPORTANT: You must ALWAYS wrap your internal thinking process in <think> and </think> tags before providing your final answer.
Example:
<think>
The user is asking for the capital of France.
</think>
The capital of France is Paris.

Always be respectful and helpful."""
    }
}

/**
 * State for the model loading process.
 */
sealed class ModelLoadState {
    object Idle : ModelLoadState()
    data class Loading(val progress: String) : ModelLoadState()
    object Ready : ModelLoadState()
    data class Error(val message: String) : ModelLoadState()
}

/**
 * State for a chat response generation.
 */
sealed class GenerationState {
    object Idle : GenerationState()
    object Generating : GenerationState()
    data class Streaming(val text: String) : GenerationState()
    data class Done(val text: String) : GenerationState()
    data class Error(val message: String) : GenerationState()
}
