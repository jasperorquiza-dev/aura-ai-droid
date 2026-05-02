package com.gemmaai.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemmaai.app.R
import com.gemmaai.app.databinding.ActivityMainBinding
import com.gemmaai.app.model.GenerationState
import com.gemmaai.app.model.ModelLoadState
import com.gemmaai.app.ui.adapter.ChatAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main chat activity with glassmorphism UI.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter

    // Voice input launcher
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { spokenText ->
                binding.messageInput.setText(spokenText)
                binding.messageInput.setSelection(spokenText.length)
            }
        }
    }

    // Image picker launcher
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedImage(it)
        }
    }

    // File picker launcher
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleFileSelected(it) }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setupUI()
        observeViewModel()
        requestPermissions()
    }

    private fun setupUI() {
        // Recycler view
        chatAdapter = ChatAdapter()
        binding.recyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            itemAnimator = null // Disable for smooth streaming
        }

        // Toolbar
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }
        binding.clearButton.setOnClickListener {
            haptic()
            viewModel.clearConversation()
        }

        // Send button
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            if (text.isNotBlank() || viewModel.selectedImage.value != null) {
                haptic()
                viewModel.sendMessage(text)
                binding.messageInput.text?.clear()
            }
        }

        // Voice button
        binding.voiceButton.setOnClickListener {
            haptic()
            startVoiceInput()
        }

        // Attach image button
        binding.attachImageButton.setOnClickListener {
            haptic()
            showAttachmentMenu()
        }

        // Input text watcher - toggle send/voice button
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                updateSendVoiceButton(hasText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Remove selected image
        binding.removeImageButton.setOnClickListener {
            viewModel.clearSelectedImage()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                chatAdapter.submitList(messages.toMutableList()) {
                    // Scroll to bottom after update
                    if (messages.isNotEmpty()) {
                        binding.recyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.generationState.collect { state ->
                when (state) {
                    is GenerationState.Generating, is GenerationState.Streaming -> {
                        binding.sendButton.isEnabled = false
                        binding.typingIndicator.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.sendButton.isEnabled = true
                        binding.typingIndicator.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedImage.collect { pair ->
                if (pair != null) {
                    binding.imagePreviewContainer.visibility = View.VISIBLE
                    binding.previewImage.setImageBitmap(pair.second)
                    binding.previewImage.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.scale_in)
                    )
                } else {
                    binding.imagePreviewContainer.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.modelState.collect { state ->
                when (state) {
                    is ModelLoadState.Error -> {
                        Snackbar.make(binding.root, "Model error: ${state.message}", Snackbar.LENGTH_LONG)
                            .setAction("Retry") { viewModel.loadModel() }
                            .show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateSendVoiceButton(hasText: Boolean) {
        if (hasText || viewModel.selectedImage.value != null) {
            binding.voiceButton.visibility = View.GONE
            binding.sendButton.visibility = View.VISIBLE
        } else {
            binding.voiceButton.visibility = View.VISIBLE
            binding.sendButton.visibility = View.GONE
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Aura...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Voice input not available", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showAttachmentMenu() {
        // Show bottom sheet for attachment options
        val bottomSheet = AttachmentBottomSheet { option ->
            when (option) {
                AttachmentOption.IMAGE -> imagePicker.launch("image/*")
                AttachmentOption.FILE -> filePicker.launch(arrayOf("text/*", "application/pdf", "application/json"))
            }
        }
        bottomSheet.show(supportFragmentManager, "attachment")
    }

    private fun handleFileSelected(uri: Uri) {
        // Read text file and insert into message input
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().readText().take(2000) // Limit to 2000 chars
                val fileName = uri.lastPathSegment ?: "file"
                binding.messageInput.setText("Please analyze this file content:\n\n$text")
                binding.messageInput.setSelection(binding.messageInput.text?.length ?: 0)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Could not read file: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun haptic() {
        if (viewModel.prefsManager.isHapticsEnabled()) {
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check if persona was updated in Settings
        val updatedPersona = viewModel.prefsManager.loadPersona()
        if (updatedPersona != viewModel.getCurrentPersona()) {
            viewModel.updatePersona(updatedPersona)
        }
    }
}

enum class AttachmentOption { IMAGE, FILE }
