package com.gemmaai.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gemmaai.app.R
import com.gemmaai.app.databinding.ActivitySplashBinding
import com.gemmaai.app.model.ModelLoadState
import kotlinx.coroutines.launch

/**
 * Splash/Loading screen. Checks for the Gemma model and shows loading progress.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // Start animations
        startAnimations()

        // Observe model loading state
        lifecycleScope.launch {
            viewModel.modelState.collect { state ->
                handleModelState(state)
            }
        }
    }

    private fun startAnimations() {
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        binding.logoContainer.startAnimation(pulseAnim)
        binding.appNameText.startAnimation(fadeInAnim)
        binding.taglineText.startAnimation(fadeInAnim)
    }

    private fun handleModelState(state: ModelLoadState) {
        when (state) {
            is ModelLoadState.Loading -> {
                binding.statusText.text = state.progress
                binding.progressBar.isIndeterminate = true
            }
            is ModelLoadState.Ready -> {
                binding.statusText.text = "Ready!"
                navigateToMain()
            }
            is ModelLoadState.Error -> {
                showModelError(state.message)
            }
            else -> {}
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun showModelError(error: String) {
        binding.progressBar.isIndeterminate = false
        binding.statusText.text = "⚠️ Model not found"
        binding.errorContainer.visibility = android.view.View.VISIBLE
        binding.errorText.text = buildString {
            append("Could not load Gemma model.\n\n")
            append("Please download gemma-2b-it-cpu-int4.bin from:\n")
            append("ai.google.dev/edge/mediapipe/solutions/genai\n\n")
            append("Then push to device:\n")
            append("adb push gemma.bin /data/local/tmp/llm/\n\n")
            append("Error: $error")
        }
        binding.retryButton.setOnClickListener {
            binding.errorContainer.visibility = android.view.View.GONE
            viewModel.loadModel()
        }
    }
}
