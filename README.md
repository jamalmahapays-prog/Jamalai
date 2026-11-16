# MyAI Pro — Android (Kotlin & Jetpack Compose)

**MyAI Pro** is a Dark Premium, bilingual (**Arabic RTL** / **English LTR**) AI productivity platform built natively for Android with **Jetpack Compose**, **Material 3**, **Firebase AI Logic**, **Cloud Firestore**, and **Google Sign-In (Credential Manager)**.

## Features

- **Dark Premium Adaptive UI**: Deep Obsidian & Electric Violet/Cyan palette, custom bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `Cairo`, `JetBrains Mono`), responsive `NavigationBar` (phones) + `NavigationRail` (tablets/foldables), and instant RTL/LTR language switching.
- **Dashboard (Home)**: Welcome header, live connectivity & AI provider health badges, real-time usage metrics from Firestore, tool shortcuts, and recent conversations.
- **AI Chat**: Multi-conversation persistent threads in Firestore, streaming Gemini responses (`gemini-3.8-flash`, `gemini-3.5-flash-lite`, `gemini-4-argon`), optional OpenAI GPT models (`gpt-4o-mini`, `gpt-4o`), multimodal Photo Picker images & SAF document attachments (PDF, DOCX, TXT), edit, regenerate, copy, and share.
- **Smart Email & Gmail Assistant**: Structured AI email analysis (priority classification, summary, task extraction, and suggested reply) + AI email composer with explicit user confirmation before opening the email/Gmail app (`ACTION_SENDTO`).
- **AI Tools Suite**:
  - **AI Writer** (tone-controlled article & copy generation)
  - **AI Translator** (multi-language instant translation & swap)
  - **AI Summarizer** (bullet points, executive summary, key takeaways + file import)
  - **AI Coding** (Kotlin, Python, TypeScript, Java, SQL, HTML/CSS code generation, debugging, and refactoring)
- **AI Image Studio**: Text-to-Image and Image-to-Image editing via Firebase AI Logic (`gemini-nano-banana-2.1`, `gemini-3-pro-image`) with aspect ratio options, Photo Picker input, gallery save, and share.
- **AI Document Assistant**: Validates files up to 20 MB, extracts `.docx`, `.txt`, `.md`, `.csv`, `.pdf` (multimodal), and images, generates summaries, and supports document Q&A.
- **Web Search**: Queries live Wikipedia & DuckDuckGo sources via OkHttp and synthesizes cited AI summaries.
- **Voice Assistant**: Real Android `SpeechRecognizer` (with runtime `RECORD_AUDIO` permission) and `TextToSpeech` playback in Arabic and English.

## Configuration & Security

- **Google Gemini & Image Studio**: Authenticated via **Firebase AI Logic** and **Firebase App Check** (`google-services.json` + `firebase_applet_config.xml`) — no client-side `GEMINI_API_KEY` required.
- **Optional OpenAI Provider**: Configure `OPENAI_API_KEY` securely in the **Secrets panel in AI Studio** (see `.env.example`). Never hardcode keys in source code or `.env.example`.
- **Database & Auth**: Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`) and user-isolated Firestore security rules (`firestore.rules`).

## Build & Test

- Build APK: `gradle assembleDebug`
- Run Firestore JS rules tests (with emulators running on `127.0.0.1:8085`):
  `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
- Run Kotlin Robolectric repository rule tests:
  `gradle :app:testDebugUnitTest --tests '*Rule*'`
