package com.gemmaai.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.gemmaai.app.model.AiPersona
import com.google.gson.Gson

/**
 * Manages persistent storage of user preferences and AI persona settings.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemma_ai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PERSONA = "ai_persona"
        private const val KEY_MODEL_PATH = "model_path"
        private const val KEY_THEME = "dark_theme"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
    }

    fun savePersona(persona: AiPersona) {
        prefs.edit().putString(KEY_PERSONA, gson.toJson(persona)).apply()
    }

    fun loadPersona(): AiPersona {
        val json = prefs.getString(KEY_PERSONA, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AiPersona::class.java)
            } catch (e: Exception) {
                AiPersona()
            }
        } else AiPersona()
    }

    fun saveModelPath(path: String) {
        prefs.edit().putString(KEY_MODEL_PATH, path).apply()
    }

    fun getModelPath(): String? = prefs.getString(KEY_MODEL_PATH, null)

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }

    fun isVoiceEnabled(): Boolean = prefs.getBoolean(KEY_VOICE_ENABLED, true)

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    fun isHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS, true)
}
