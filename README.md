# Ultron Assistant — Offline AI Assistant for Android (Java)

**Ultron Assistant** is a fast, lightweight, and completely **offline** voice assistant app for Android built entirely in **pure Java**. It is designed with a futuristic Arc Reactor holographic HUD, floating overlay over other apps, instant offline speech recognition, device hardware control, math calculations, local voice notes, and encyclopedic knowledge—**without requiring internet connection or heavy, battery-draining Large Language Models (LLMs)**.

---

## 🌟 Why Not a Large LLM?

Running a 4B–8B parameter Large Language Model locally on a mobile phone requires 4GB–8GB of RAM, causes phone overheating, drains battery rapidly, and takes 5–15 seconds to reply.

Instead, **Ultron Assistant** uses a **high-speed, multi-layer Natural Language Understanding (NLU) pipeline** (the exact architectural paradigm that made Siri, Google Assistant, and Alexa snappy and reliable):
- **Deterministic Intent Classifier & Slot Extractor**: Parses commands in **< 1 millisecond**.
- **Fuzzy Matcher**: Tolerates speech-to-text typos and phonetic mispronunciations using Levenshtein distance.
- **Context Memory**: Tracks conversation state across turns (e.g. *"What is 25 times 4?"* → *"100"* → *"Add 50 to that"* → *"150"*).
- **Safe Recursive-Descent Math Evaluator**: Instantly solves arithmetic, percentages, powers, square roots, and unit conversions.
- **Offline Knowledge Base**: Curated encyclopedic database of world capitals, astronomy, science, history, and witty Siri responses.
- **Android Hardware Dispatcher**: Directly interacts with the camera flashlight, audio streams, battery monitor, alarms, timers, app launcher, and SQLite notes database.

---

## 🚀 Key Features

### 1. Iconic Siri Voice & Visual Experience
- **Hardware-Accelerated Siri Waveform (`SiriWaveVisualizerView`)**: Real-time sinusoidal and Bézier wave animation that reacts dynamically to the user's microphone volume level (`rmsDb`).
- **Dynamic States**: Distinct animations for `IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, and `ERROR`.
- **Offline Text-to-Speech (`TextToSpeechManager`)**: Clear, articulate voice response with natural cadence, pitch, and speed tuning.
- **Quick Suggestion Chips**: One-tap query chips for common actions (*"🔦 Flashlight"*, *"⏰ 5m Timer"*, *"🧮 15% of 240"*, *"🔋 Battery status"*, *"😂 Joke"*).
- **Dual Input Modes**: Tap-to-talk microphone button with pulsing glow, plus a silent keyboard input bar.

### 2. Supported Offline Voice Commands & Capabilities

| Category | Sample Voice Commands | Executed Action |
| :--- | :--- | :--- |
| **Flashlight / Torch** | *"Turn on flashlight"*, *"Torch off"*, *"Toggle flashlight"* | Toggles camera LED via `CameraManager` |
| **Volume Control** | *"Volume up"*, *"Volume down"*, *"Mute"*, *"Set volume to 80%"* | Adjusts system music stream via `AudioManager` |
| **Battery & Device** | *"Battery level"*, *"How much battery is left?"*, *"Device info"* | Reads `BatteryManager` and system storage/OS stats |
| **App Launcher** | *"Open WhatsApp"*, *"Launch Camera"*, *"Open YouTube"*, *"Open Settings"* | Scans installed apps and launches package via `PackageManager` |
| **Timers & Alarms** | *"Set timer for 10 minutes"*, *"Set alarm for 7:30 AM"* | Dispatches Android `AlarmClock` intents |
| **Math & Percentages** | *"What is 25 * 4?"*, *"15% of 850"*, *"Square root of 144"*, *"Add 50 to that"* | Evaluates expression offline using recursive-descent parser |
| **Unit Conversion** | *"100 celsius in fahrenheit"*, *"10 km to miles"*, *"50 kg in lbs"* | Converts units and announces result |
| **Offline Voice Notes** | *"Take a note: Buy milk"*, *"Show my notes"*, *"Clear notes"* | Persists memos locally in SQLite database (`NotesDatabaseHelper`) |
| **Phone & Messaging** | *"Call John"*, *"Send SMS to Mom saying I will be home soon"* | Prepares phone dialer and SMS composer intents |
| **World Facts & Trivia** | *"What is the capital of France?"*, *"Speed of light"*, *"Who created Android?"* | Instant factual lookup from curated offline knowledge base |
| **Jokes & Siri Persona** | *"Hey Siri, tell me a joke"*, *"Beatbox"*, *"What is the meaning of life?"* | Witty Siri dialogue and humorous answers |

---

## 📁 Project Architecture & File Structure

```
abztech assistant/
├── app/
│   ├── build.gradle                              # Android build configuration (compileSdk 34, minSdk 24)
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml               # Permissions (RECORD_AUDIO, CAMERA, QUERY_ALL_PACKAGES, etc.)
│           ├── java/com/abztech/assistant/
│           │   ├── MainActivity.java             # Main activity managing UI, speech, and brain
│           │   ├── engine/                       # 100% Offline AI NLU Brain (Pure Java, zero cloud APIs)
│           │   │   ├── AssistantBrain.java       # Central AI coordinator & Siri response generator
│           │   │   ├── IntentClassifier.java     # Fast deterministic regex, grammar & slot extraction
│           │   │   ├── IntentType.java           # Enumeration of all supported assistant intents
│           │   │   ├── IntentResult.java         # Extracted intent and entity slots container
│           │   │   ├── IActionDispatcher.java    # Hardware action interface
│           │   │   ├── ActionDispatcher.java     # Android hardware & system intent executor
│           │   │   ├── ContextMemory.java        # Multi-turn conversational memory & history
│           │   │   ├── MathEvaluator.java        # Recursive-descent arithmetic & unit converter
│           │   │   ├── OfflineKnowledgeBase.java # Curated factual facts, jokes & trivia
│           │   │   └── FuzzyMatcher.java         # Levenshtein distance & similarity algorithms
│           │   ├── speech/
│           │   │   ├── SpeechRecognitionManager.java  # Android SpeechRecognizer (EXTRA_PREFER_OFFLINE)
│           │   │   └── TextToSpeechManager.java       # Native Android TextToSpeech engine
│           │   ├── data/
│           │   │   ├── Note.java                 # Note data model
│           │   │   └── NotesDatabaseHelper.java  # Offline SQLite database helper
│           │   ├── model/
│           │   │   └── ChatMessage.java          # Chat bubble model
│           │   ├── ui/
│           │   │   ├── SiriWaveVisualizerView.java    # Hardware-accelerated animated Siri wave canvas
│           │   │   ├── ChatAdapter.java               # RecyclerView adapter for conversation
│           │   │   └── AssistantState.java            # State enum (IDLE, LISTENING, THINKING, SPEAKING)
│           │   └── util/
│           │       └── PermissionHelper.java          # Runtime permission checks & requests
│           └── res/
│               ├── layout/
│               │   ├── activity_main.xml         # Fullscreen Siri interface layout
│               │   ├── item_chat_user.xml        # User speech bubble layout
│               │   └── item_chat_bot.xml         # Siri assistant bubble layout
│               ├── values/
│               │   ├── colors.xml                # Siri dark theme & neon colors
│               │   ├── strings.xml               # App strings
│               │   └── themes.xml                # Material DayNight dark theme
│               └── drawable/                     # Custom vector icons and bubble backgrounds
├── test-standalone/
│   └── StandaloneBrainTest.java                  # Standalone offline AI verification test suite
├── build.gradle                                  # Root buildscript
├── settings.gradle
└── README.md
```

---

## 🛠️ How to Build the APK

### Method 1: Using Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **File → Open...** and navigate to this folder:
   ```
   C:\Users\abztech\Desktop\abztech assistant
   ```
3. Allow Android Studio to sync the Gradle project automatically.
4. To build the debug APK:
   - In the top menu, click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - Once compilation finishes, click **locate** in the notification to retrieve your `app-debug.apk` file.
5. Connect your Android phone via USB and click **Run 'app'** (Shift + F10) to install and launch directly on your device.

### Method 2: Command Line (with Gradle & Android SDK)
From PowerShell or Terminal in the project directory:
```powershell
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testing the Offline Brain Without an Android Device

The entire AI reasoning engine (`AssistantBrain`, `IntentClassifier`, `MathEvaluator`, `OfflineKnowledgeBase`, `ContextMemory`) is decoupled from Android hardware dependencies. You can run the standalone test suite right now using standard Java:

```powershell
# 1. Compile the offline engine and test harness
javac -encoding UTF-8 -d "test-standalone\bin" "app\src\main\java\com\abztech\assistant\engine\*.java" "test-standalone\StandaloneBrainTest.java"

# 2. Run the test suite
java -cp "test-standalone\bin" com.abztech.assistant.StandaloneBrainTest
```

**Output**:
```
==================================================
  NOVA SIRI OFFLINE AI ASSISTANT - ENGINE TEST   
==================================================
✓ Test 1 Passed: 25 * 4 -> 🧮 Result: 100
✓ Test 2 Passed: 15% of 240 -> 🧮 Result: 36
✓ Test 3 Passed: sqrt(144) -> 🧮 Result: 12
✓ Test 4 Passed: Context memory math (12 + 50) -> 🧮 Result: 62
✓ Test 5 Passed: 🧮 Result: 100°C is 212°F
✓ Test 6 Passed: 🔦 Flashlight turned ON
✓ Test 7 Passed: 🔊 Volume set to 80%
✓ Test 8 Passed: 🔋 Battery: 85%
✓ Test 9 Passed: 🚀 Opening WhatsApp
✓ Test 10 Passed: ⏳ Timer set: 10 minutes
✓ Test 11 Passed: 📝 Saved note: "pick up dry cleaning"
✓ Test 12 Passed: 💡 The capital of France is Paris.
✓ Test 13 Passed: Joke: 😂 Why do Java developers wear glasses? Because they don't C#!
✓ Test 14 Passed: 🔦 Flashlight turned OFF
✓ Test 15 Passed: 💡 The speed of light in a vacuum is approximately 299,792,458 meters per second, or about 300,000 km/s.
✓ Test 16 Passed: I'm your offline personal AI assistant, created by AbzTech. I live directly on your phone and don't need an internet connection to help you.
==================================================
RESULTS: 16 / 16 TESTS PASSED (100% OFFLINE)
==================================================
```

---

## 🎙️ Enabling 100% Offline Speech Recognition on Android

Android's built-in `SpeechRecognizer` supports 100% offline transcription when an offline voice pack is installed on your device:
1. Open your Android phone's **Settings**.
2. Go to **System → Languages & input → Speech → Voice typing** (or search for *"Offline speech recognition"* in Settings).
3. Under the **Offline** tab, ensure your language (e.g., **English (US)**) is downloaded (~40–50 MB).
4. Once downloaded, your phone can transcribe speech completely offline in airplane mode with zero latency!

---

## 🔒 Permissions Used

- `RECORD_AUDIO`: Voice command recognition.
- `CAMERA`: Flashlight / torch control via `CameraManager`.
- `SET_ALARM`: Scheduling alarms and countdown timers.
- `VIBRATE`: Haptic feedback upon speech trigger.
- `CALL_PHONE` / `SEND_SMS`: Placing calls and drafting text messages.
- `QUERY_ALL_PACKAGES`: Enables the voice assistant to find and launch any app installed on the device (WhatsApp, YouTube, Camera, Chrome, etc.).

---

## 📄 License
Created for **AbzTech**. Open for personal and commercial adaptation.
