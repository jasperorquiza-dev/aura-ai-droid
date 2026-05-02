package com.gemmaai.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gemmaai.app.model.AiPersona
import com.gemmaai.app.model.ChatMessage
import com.gemmaai.app.model.GenerationState
import com.gemmaai.app.model.MessageRole
import com.gemmaai.app.model.ModelLoadState
import com.gemmaai.app.utils.GemmaInferenceEngine
import com.gemmaai.app.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = GemmaInferenceEngine(application)
    val prefsManager = PreferencesManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _modelState = MutableStateFlow<ModelLoadState>(ModelLoadState.Idle)
    val modelState: StateFlow<ModelLoadState> = _modelState.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _selectedImage = MutableStateFlow<Pair<Uri, Bitmap>?>(null)
    val selectedImage: StateFlow<Pair<Uri, Bitmap>?> = _selectedImage.asStateFlow()

    private var streamingMessageId: Long = -1L
    private var currentPersona: AiPersona = prefsManager.loadPersona()

    init {
        loadModel()
    }

    /**
     * Load the Gemma model asynchronously.
     */
    fun loadModel() {
        viewModelScope.launch {
            _modelState.value = ModelLoadState.Loading("Initializing...")
            val result = engine.initialize(currentPersona) { progress ->
                _modelState.value = ModelLoadState.Loading(progress)
            }
            if (result.isSuccess) {
                _modelState.value = ModelLoadState.Ready
                addWelcomeMessage()
            } else {
                _modelState.value = ModelLoadState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            role = MessageRole.ASSISTANT,
            text = "Hi! I'm **${currentPersona.name}**, your offline AI assistant powered by Gemma 4. " +
                    "I'm running entirely on your device — no internet needed! 🌟\n\n" +
                    "I can:\n" +
                    "- 💬 Chat and answer complex questions\n" +
                    "- 💻 Help you write and debug code\n" +
                    "- 📄 Read and summarize text\n\n" +
                    "What would you like to talk about?"
        )
        _messages.value = listOf(welcome)
    }

    /**
     * Send a message and get an AI response.
     */
    fun sendMessage(text: String) {
        if (text.isBlank() && _selectedImage.value == null) return
        if (_generationState.value is GenerationState.Generating) return

        val image = _selectedImage.value
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            text = text.trim(),
            imageUri = image?.first,
            imageBitmap = image?.second
        )
        _messages.value = _messages.value + userMsg

        // Clear selected image
        _selectedImage.value = null

        // Add streaming placeholder
        val streamingMsg = ChatMessage(
            id = System.currentTimeMillis() + 1,
            role = MessageRole.ASSISTANT,
            text = "",
            isLoading = true
        )
        streamingMessageId = streamingMsg.id
        _messages.value = _messages.value + streamingMsg
        _generationState.value = GenerationState.Generating

        viewModelScope.launch {
            val responseBuilder = StringBuilder()
            engine.generateResponseStreaming(text.trim(), image?.second)
                .catch { e ->
                    updateStreamingMessage(
                        "⚠️ Error generating response: ${e.message}",
                        done = true
                    )
                    _generationState.value = GenerationState.Error(e.message ?: "Error")
                }
                .collect { chunk ->
                    responseBuilder.append(chunk)
                    val current = responseBuilder.toString()
                    updateStreamingMessage(current, done = false)
                    _generationState.value = GenerationState.Streaming(current)
                }

            // Mark done
            _generationState.value = GenerationState.Done(responseBuilder.toString())
            updateStreamingMessage(responseBuilder.toString(), done = true)
        }
    }

    private fun updateStreamingMessage(text: String, done: Boolean) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == streamingMessageId) {
                msg.copy(text = text, isLoading = !done)
            } else msg
        }
    }

    /**
     * Set an image to send with the next message.
     */
    fun setSelectedImage(uri: Uri) {
        viewModelScope.launch {
            val bitmap = engine.decodeBitmap(uri)
            if (bitmap != null) {
                _selectedImage.value = Pair(uri, bitmap)
            }
        }
    }

    fun clearSelectedImage() {
        _selectedImage.value = null
    }

    /**
     * Clear conversation history.
     */
    fun clearConversation() {
        engine.clearConversation()
        addWelcomeMessage()
        _generationState.value = GenerationState.Idle
    }

    /**
     * Update AI persona and reload session.
     */
    fun updatePersona(persona: AiPersona) {
        currentPersona = persona
        prefsManager.savePersona(persona)
        viewModelScope.launch {
            engine.updatePersona(persona)
        }
    }

    fun getCurrentPersona() = currentPersona

    fun getModelPath() = engine.getModelPath()

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
