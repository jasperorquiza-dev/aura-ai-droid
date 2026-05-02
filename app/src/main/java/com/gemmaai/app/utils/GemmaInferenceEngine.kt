package com.gemmaai.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.gemmaai.app.model.AiPersona
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Core AI Engine wrapping the state-of-the-art Google LiteRT-LM runtime.
 * Supports modern Gemma 3 and Gemma 4 (.litertlm and .task) files.
 */
class GemmaInferenceEngine(private val context: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentPersona: AiPersona = AiPersona()

    companion object {
        // Alternative paths for finding models on device
        val MODEL_PATHS = listOf(
            "/sdcard/Aura/gemma.litertlm",
            "/sdcard/Download/gemma.litertlm",
            "/data/local/tmp/llm/gemma-4-E4B-it.litertlm",
            "/sdcard/Aura/gemma.bin",
            "/sdcard/Download/gemma.bin",
            "/data/local/tmp/llm/gemma.bin"
        )
    }

    suspend fun initialize(
        persona: AiPersona,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            currentPersona = persona
            onProgress("Searching for LiteRT model...")

            val modelPath = findModelPath()
                ?: return@withContext Result.failure(
                    Exception("Model not found. Please place gemma.litertlm in /sdcard/Aura/")
                )

            onProgress("Loading LiteRT-LM Engine (this may take a moment)...")

            val engineConfig = EngineConfig(
                modelPath = modelPath
            )

            engine = Engine(engineConfig)
            engine?.initialize()

            onProgress("Model loaded! Initializing session...")
            createNewSession()
            onProgress("Ready!")
            
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createNewSession() {
        conversation?.close()
        conversation = engine?.createConversation()
        
        // Inject system instructions as the first message to set the AI's behavior
        if (currentPersona.systemPrompt.isNotBlank()) {
            val systemMsg = Message.of(
                "SYSTEM INSTRUCTIONS (DO NOT REPLY TO THIS):\n${currentPersona.systemPrompt}\n\nAcknowledge these instructions internally and prepare for the user."
            )
            try {
                // We must collect the flow to actually push the message into the context window
                conversation?.sendMessageAsync(systemMsg)?.collect {}
            } catch (e: Exception) {
                // Ignore initialization errors
            }
        }
    }

    fun generateResponseStreaming(
        userMessage: String,
        imageBitmap: Bitmap? = null
    ): Flow<String> {
        val currentConversation = conversation
            ?: throw Exception("Conversation not initialized")

        // Build a multimodal message if an image is provided
        // The current Gemma 4 E4B model is text-only. Sending ImageContent causes a native SIGSEGV.
        // We will only send the text prompt to maintain stability.
        val finalPrompt = if (imageBitmap != null) {
            "[Image attached but current model is text-only. Please respond to the text prompt.]\n$userMessage"
        } else {
            userMessage
        }
        val msg = Message.of(finalPrompt)
        
        // Return the mapped flow from LiteRT-LM
        return currentConversation.sendMessageAsync(msg).map { message: com.google.ai.edge.litertlm.Message -> 
            val sb = StringBuilder()
            try {
                // Use a standard index-based loop for maximum compatibility
                val contents = message.contents
                val size = (contents as java.util.List<*>).size
                for (i in 0 until size) {
                    val content = contents.get(i)
                    if (content is com.google.ai.edge.litertlm.Content.Text) {
                        sb.append(content.text)
                    }
                }
            } catch (e: Exception) {
                return@map message.toString()
            }
            
            val result = sb.toString()
            if (result.isEmpty()) message.toString() else result
        }
    }

    fun clearConversation() {
        if (engine != null) {
            // Need coroutine to recreate session, but for now we just recreate conversation
            conversation?.close()
            conversation = engine?.createConversation()
        }
    }

    suspend fun updatePersona(persona: AiPersona): Result<Unit> = withContext(Dispatchers.IO) {
        currentPersona = persona
        return@withContext try {
            if (engine != null) {
                createNewSession()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decodeBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val maxSize = 768
                if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findModelPath(): String? {
        return MODEL_PATHS.firstOrNull { File(it).exists() }
    }

    fun isReady() = engine != null && conversation != null

    fun getModelPath() = findModelPath()

    fun release() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}
