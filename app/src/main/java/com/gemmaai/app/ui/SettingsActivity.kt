package com.gemmaai.app.ui

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.gemmaai.app.R
import com.gemmaai.app.databinding.ActivitySettingsBinding
import com.gemmaai.app.model.AiPersona
import com.google.android.material.snackbar.Snackbar

/**
 * Settings screen for customizing AI persona, response style, and app preferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setupToolbar()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
        }
    }

    private fun loadCurrentSettings() {
        val persona = viewModel.getCurrentPersona()

        binding.apply {
            nameInput.setText(persona.name)
            systemPromptInput.setText(persona.systemPrompt)

            // Temperature slider (0.0 - 2.0, displayed as 0-200)
            temperatureSlider.progress = (persona.temperature * 100).toInt()
            temperatureValue.text = "%.2f".format(persona.temperature)

            // Max tokens slider (128 - 2048)
            maxTokensSlider.progress = persona.maxTokens - 128
            maxTokensValue.text = "${persona.maxTokens}"

            // Top-K slider (1 - 100)
            topKSlider.progress = persona.topK
            topKValue.text = "${persona.topK}"

            // Top-P slider (0.0 - 1.0)
            topPSlider.progress = (persona.topP * 100).toInt()
            topPValue.text = "%.2f".format(persona.topP)

            // App preferences
            voiceSwitch.isChecked = viewModel.prefsManager.isVoiceEnabled()
            hapticsSwitch.isChecked = viewModel.prefsManager.isHapticsEnabled()

            // Model info
            val modelPath = viewModel.getModelPath()
            modelPathText.text = modelPath ?: "Model not found"
        }
    }

    private fun setupListeners() {
        // Temperature
        binding.temperatureSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                binding.temperatureValue.text = "%.2f".format(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Max tokens
        binding.maxTokensSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 128
                binding.maxTokensValue.text = "$value"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Top-K
        binding.topKSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.topKValue.text = "$progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Top-P
        binding.topPSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                binding.topPValue.text = "%.2f".format(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Voice toggle
        binding.voiceSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.prefsManager.setVoiceEnabled(checked)
        }

        // Haptics toggle
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.prefsManager.setHapticsEnabled(checked)
        }

        // Save button
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        // Reset to defaults
        binding.resetButton.setOnClickListener {
            val defaults = AiPersona()
            binding.nameInput.setText(defaults.name)
            binding.systemPromptInput.setText(defaults.systemPrompt)
            binding.temperatureSlider.progress = (defaults.temperature * 100).toInt()
            binding.maxTokensSlider.progress = defaults.maxTokens - 128
            binding.topKSlider.progress = defaults.topK
            binding.topPSlider.progress = (defaults.topP * 100).toInt()
            Snackbar.make(binding.root, "Reset to defaults", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        val name = binding.nameInput.text.toString().trim().ifBlank { "Aura" }
        val systemPrompt = binding.systemPromptInput.text.toString().trim()
            .ifBlank { AiPersona.DEFAULT_SYSTEM_PROMPT }
        val temperature = binding.temperatureSlider.progress / 100f
        val maxTokens = binding.maxTokensSlider.progress + 128
        val topK = binding.topKSlider.progress.coerceAtLeast(1)
        val topP = binding.topPSlider.progress / 100f

        val persona = AiPersona(
            name = name,
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxTokens = maxTokens,
            topK = topK,
            topP = topP
        )

        viewModel.updatePersona(persona)
        Snackbar.make(binding.root, "✅ Settings saved! Conversation reset.", Snackbar.LENGTH_SHORT).show()
    }
}
