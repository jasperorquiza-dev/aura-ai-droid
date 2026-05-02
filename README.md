# Aura AI — Offline Gemma Assistant for Android

An Android AI chat app powered by **Google Gemma 4B** running **100% offline** on-device using MediaPipe LLM Inference API. Features a premium **glassmorphism** UI, voice input, image analysis, file reading, and full AI persona customization.

---

## Features

| Feature | Description |
|---------|-------------|
| **Offline AI** | Gemma 4B runs entirely on-device — no internet, no API keys |
| **Chat** | Streaming responses with Markdown rendering |
| **Voice Input** | Speak your messages using the microphone |
| **Image Analysis** | Send photos from gallery for the AI to describe |
| **File Reading** | Load text/PDF files and ask the AI to summarize |
| **Glassmorphism UI** | Premium dark purple glass effect design |
| **AI Customization** | Name, system prompt, temperature, topK, topP, maxTokens |
| **Haptic Feedback** | Satisfying tactile responses on interactions |

---

## Quick Setup

### 1. Open in Android Studio
```
File → Open → select the GemmaAI folder
```

### 2. Download the Gemma Model

Go to: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android

Download: **`gemma2-4b-it-cpu-int4.bin`** (≈ 2.6 GB)

> You need to accept Google's terms on Kaggle: https://www.kaggle.com/models/google/gemma

### 3. Push Model to Device

```bash
# Create directory on device
adb shell mkdir -p /data/local/tmp/llm

# Push model (replace path with where you downloaded it)
adb push gemma2-4b-it-cpu-int4.bin /data/local/tmp/llm/gemma.bin

# Verify
adb shell ls -la /data/local/tmp/llm/
```

The app also checks `/sdcard/Download/gemma.bin` automatically.

### 4. Add Outfit Fonts (optional but recommended)

1. Download from: https://fonts.google.com/specimen/Outfit
2. Rename: `Outfit-Regular.ttf` → `outfit_regular.ttf`, `Outfit-Bold.ttf` → `outfit_bold.ttf`
3. Place in: `app/src/main/res/font/`

### 5. Build & Run

```
Run → Run 'app'
```

Minimum API: **26 (Android 8.0)**  
Recommended: Android 12+ with **6GB+ RAM**

---

## Screenshots Preview

### App Screens

| Splash | Chat | Settings |
|--------|------|----------|
| Animated logo + model loading | Glassmorphism chat bubbles | Sliders for AI tuning |

### UI Design Highlights
- **Deep purple → indigo** gradient background
- **Frosted glass** chat bubbles with border glow
- **Purple gradient** send button with haptic feedback
- **Animated typing dots** while AI thinks
- **Bottom sheet** for image/file attachment

---

## Project Structure

```
GemmaAI/
├── app/src/main/
│   ├── java/com/gemmaai/app/
│   │   ├── model/
│   │   │   └── ChatMessage.kt        # Data models + states
│   │   ├── utils/
│   │   │   ├── GemmaInferenceEngine.kt  # MediaPipe LLM wrapper
│   │   │   └── PreferencesManager.kt    # Settings persistence
│   │   └── ui/
│   │       ├── ChatViewModel.kt      # State management
│   │       ├── SplashActivity.kt     # Loading screen
│   │       ├── MainActivity.kt       # Main chat UI
│   │       ├── SettingsActivity.kt   # AI persona settings
│   │       ├── AttachmentBottomSheet.kt
│   │       └── adapter/
│   │           └── ChatAdapter.kt    # RecyclerView adapter
│   └── res/
│       ├── layout/                   # XML layouts
│       ├── drawable/                 # Glassmorphism shapes
│       ├── anim/                     # Animations
│       ├── font/                     # Outfit font (add manually)
│       └── values/                   # Colors, themes, strings
```

---

## AI Customization

In Settings (gear icon), you can adjust:

| Parameter | Description | Range |
|-----------|-------------|-------|
| **AI Name** | What the assistant calls itself | Text |
| **System Prompt** | Defines personality & behavior | Multiline text |
| **Temperature** | Creativity/randomness | 0.0 → 2.0 |
| **Max Tokens** | Response length limit | 128 → 2048 |
| **Top-K** | Token sampling diversity | 1 → 100 |
| **Top-P** | Nucleus sampling threshold | 0.0 → 1.0 |

---

## Performance Tips

- Use **int4 quantized** model (gemma2-4b-it-cpu-int4.bin) for fastest performance
- Keep **Max Tokens ≤ 512** for faster responses on older devices
- The first response after loading may be slower (JIT warmup)
- Recommended: Snapdragon 8 Gen 1+ or equivalent with 8GB RAM

---

## Troubleshooting

**"Model not found"**
→ Run: `adb push gemma.bin /data/local/tmp/llm/gemma.bin`

**Out of memory crash**
→ Reduce Max Tokens in Settings, use the int4 model variant

**Slow responses**
→ Normal for 4B model on CPU. Consider keeping responses short.

**Font shows as default**
→ Download Outfit font and place in `res/font/` (see setup step 4)

---

## Dependencies

- **MediaPipe Tasks GenAI** 0.10.14 — on-device LLM inference
- **Markwon** 4.6.2 — Markdown rendering in chat
- **Coil** 2.7.0 — Image loading
- **Material3** — UI components
- **Kotlin Coroutines** — Async streaming

---

## License

This project uses Google's Gemma model under the [Gemma Terms of Use](https://ai.google.dev/gemma/terms).
