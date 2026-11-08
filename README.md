# Amara Assistant — Offline AI Assistant for Android (Java)

**Amara Assistant** is a fast, lightweight, and completely **offline** AI voice assistant for Android built entirely in **pure Java**. Designed with a futuristic Arc Reactor holographic HUD, system-wide floating overlay mode, instant offline speech recognition, app launching & closing, device hardware controls, math calculations, local voice notes, and encyclopedic knowledge—**running 100% locally with zero internet connection and zero battery-draining Large Language Models (LLMs)**.

---

<p align="center">
  <a href="https://github.com/abrahamdw882/abztech-assistant/raw/refs/heads/main/amara.apk">
    <img src="https://img.shields.io/badge/DOWNLOAD-AMARA%20APK-success?style=for-the-badge&logo=android&logoColor=white">
  </a>
</p>

<br>
<p align="center">
  <img src="https://i.ibb.co/4nhKvz43/amara-apppic.jpg" alt="Amara App" width="300">
</p>

## Why Not a Heavy LLM?

Running a 4B–8B parameter Large Language Model locally on a mobile phone consumes 4GB–8GB of RAM, generates excessive device heat, drains battery rapidly, and introduces a 5–15 second response delay.

Instead, **Amara Assistant** uses a **high-speed, multi-layer Natural Language Understanding (NLU) pipeline** that operates deterministically and instantaneously:
- **Deterministic Intent Classifier & Slot Extractor**: Parses commands and extracts parameters in **< 1 millisecond**.
- **Fuzzy Matcher**: Tolerates speech-to-text transcription typos and phonetic variations using Levenshtein distance.
- **Context Memory**: Tracks conversational state across turns (e.g. *"What is 25 times 4?"* → *"100"* → *"Add 50 to that"* → *"150"*).
- **Safe Recursive-Descent Math Evaluator**: Solves arithmetic, percentages, powers, square roots, and unit conversions offline.
- **Offline Knowledge Base**: Curated encyclopedic database of world capitals, astronomy, science, history, and witty responses.
- **Hardware Action Dispatcher**: Directly manages flashlight, audio streams, dual-method battery monitoring, alarms, timers, app launching, app closing, and local SQLite notes.

---

## Key Features

### 1. Holographic Arc Reactor HUD & System Overlay
- **Holographic Arc Reactor (`JarvisVisualizerView`)**: Custom hardware-accelerated canvas rendering concentric rotating energy rings, segment ticks, and a pulsating core responding in real time to microphone audio input (`rmsDb`).
- **Floating System Overlay (`JarvisOverlayService`)**: Draggable glassmorphic card that floats over any application on your screen via `SYSTEM_ALERT_WINDOW`, enabling hands-free or one-tap voice interaction anywhere on the device.
- **Acoustic Self-Echo Cancellation**: Prevents the assistant from listening to its own voice by pausing microphone intake during speech synthesis and verifying response audio.

### 2. Supported Offline Voice Commands & Capabilities

| Category | Sample Voice Commands | Executed Action |
| :--- | :--- | :--- |
| **Wake Word & Persona** | *"Hey Amara"*, *"Amara"*, *"Who are you?"* | Wakes up assistant and announces Amara identity |
| **Battery & Power** | *"What's my battery percent?"*, *"Tell me the battery percentage"*, *"Is it charging?"* | Reads device capacity via `BatteryManager` and sticky broadcast |
| **App Launcher** | *"Open WhatsApp"*, *"Launch Camera"*, *"Open YouTube"*, *"Open Settings"* | Scans installed apps and launches package via `PackageManager` |
| **App Closer & Home** | *"Close YouTube"*, *"Close this app"*, *"Exit"*, *"Go to home screen"* | Kills app background processes and returns to home screen |
| **Floating Overlay** | *"Display over screen"*, *"Floating mode"*, *"Overlay mode"* | Launches floating Arc Reactor HUD over other apps |
| **Flashlight / Torch** | *"Turn on flashlight"*, *"Torch off"*, *"Toggle flashlight"* | Controls camera LED via `CameraManager` |
| **Volume Control** | *"Volume up"*, *"Volume down"*, *"Mute"*, *"Set volume to 80%"* | Adjusts system music stream via `AudioManager` |
| **Time & Date** | *"What time is it?"*, *"What's today's date?"*, *"What day is today?"* | Formats local clock and calendar data |
| **Timers & Alarms** | *"Set timer for 10 minutes"*, *"Set alarm for 7:30 AM"* | Dispatches Android `AlarmClock` intents |
| **Math & Percentages** | *"What is 25 * 4?"*, *"15% of 850"*, *"Square root of 144"*, *"Add 50 to that"* | Evaluates mathematical expressions offline |
| **Unit Conversion** | *"100 celsius in fahrenheit"*, *"10 km to miles"*, *"50 kg in lbs"* | Converts metric/imperial units |
| **Offline Voice Notes** | *"Take a note: Buy milk"*, *"Show my notes"*, *"Clear notes"* | Persists memos locally in SQLite database (`NotesDatabaseHelper`) |
| **Phone & Messaging** | *"Call John"*, *"Send SMS to Mom saying I will be home soon"* | Prepares phone dialer and SMS composer intents |
| **World Facts & Trivia** | *"What is the capital of France?"*, *"Speed of light"*, *"Who created Android?"* | Instant factual lookup from curated offline knowledge base |
| **Casual Conversation** | *"How are you?"*, *"What's up?"*, *"Tell me a joke"* | Conversational small talk and humor |

---

## Project Architecture & File Structure

```
abztech assistant/
├── app/
│   ├── build.gradle                              # Android build configuration (compileSdk 34, minSdk 24)
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml               # Permissions (RECORD_AUDIO, CAMERA, SYSTEM_ALERT_WINDOW, etc.)
│           ├── java/com/abztech/assistant/
│           │   ├── MainActivity.java             # Main activity managing UI, speech, and brain
│           │   ├── engine/                       # 100% Offline AI NLU Brain (Pure Java, zero cloud APIs)
│           │   │   ├── AssistantBrain.java       # Central AI coordinator & response generator
│           │   │   ├── IntentClassifier.java     # Deterministic regex, grammar & slot extraction
│           │   │   ├── IntentType.java           # Enumeration of all supported assistant intents
│           │   │   ├── IntentResult.java         # Extracted intent and entity slots container
│           │   │   ├── IActionDispatcher.java    # Hardware action interface
│           │   │   ├── ActionDispatcher.java     # Android hardware & system intent executor
│           │   │   ├── ContextMemory.java        # Multi-turn conversational memory & history
│           │   │   ├── MathEvaluator.java        # Recursive-descent arithmetic & unit converter
│           │   │   ├── OfflineKnowledgeBase.java # Curated factual facts, jokes & trivia
│           │   │   └── FuzzyMatcher.java         # Levenshtein distance & similarity algorithms
│           │   ├── service/
│           │   │   └── JarvisOverlayService.java # Floating Arc Reactor system overlay foreground service
│           │   ├── speech/
│           │   │   ├── SpeechRecognitionManager.java  # Android SpeechRecognizer (continuous offline listening)
│           │   │   └── TextToSpeechManager.java       # Native Android TextToSpeech engine
│           │   ├── data/
│           │   │   ├── Note.java                 # Note data model
│           │   │   └── NotesDatabaseHelper.java  # Offline SQLite database helper
│           │   ├── model/
│           │   │   └── ChatMessage.java          # Message model
│           │   ├── ui/
│           │   │   ├── JarvisVisualizerView.java # Hardware-accelerated Arc Reactor HUD canvas
│           │   │   ├── SiriWaveVisualizerView.java # Legacy waveform canvas
│           │   │   ├── ChatAdapter.java          # Protocol adapter
│           │   │   └── AssistantState.java       # State enum (IDLE, LISTENING, THINKING, SPEAKING)
│           │   └── util/
│           │       └── PermissionHelper.java     # Runtime permission checks & overlay requests
│           └── res/
│               ├── layout/
│               │   ├── activity_main.xml         # Fullscreen Arc Reactor interface layout
│               │   └── layout_overlay_jarvis.xml # Floating Arc Reactor card overlay layout
│               ├── values/
│               │   ├── colors.xml                # Arc Reactor cyan, deep blue, dark theme
│               │   ├── strings.xml               # App strings (Amara)
│               │   └── themes.xml                # Material DayNight dark theme
│               └── drawable/                     # Custom vector icons and HUD backgrounds
├── test-standalone/
│   └── StandaloneBrainTest.java                  # Standalone offline AI verification test suite (33 tests)
├── .github/workflows/
│   └── build-apk.yml                             # Automated GitHub Actions APK build & CI testing
├── build.gradle                                  # Root buildscript
├── settings.gradle
└── README.md
```

---

## How to Build the APK

### Method 1: GitHub Actions CI (Fastest)
Every push to `main` automatically runs all 33 offline tests and builds a debug APK.
1. Go to the **Actions** tab on your GitHub repository: `abztech-assistant`.
2. Click the latest **Build Android APK** workflow run.
3. Download the **`Amara-Assistant-Debug-APK`** artifact zip containing `app-debug.apk`.

### Method 2: Using Android Studio
1. Open **Android Studio**.
2. Select **File → Open...** and choose the project root folder.
3. Allow Gradle to sync.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Retrieve your compiled APK at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Method 3: Command Line
```powershell
./gradlew assembleDebug
```

---

## Testing the Offline Brain Without an Android Device

The core reasoning engine (`AssistantBrain`, `IntentClassifier`, `MathEvaluator`, `OfflineKnowledgeBase`, `ContextMemory`) is decoupled from Android hardware dependencies. You can compile and run the full 33-test standalone suite locally on any computer using standard `javac` and `java`:

```powershell
# 1. Compile the offline engine and test harness
javac -encoding UTF-8 -d "test-standalone/build" -cp "app/src/main/java" test-standalone/StandaloneBrainTest.java

# 2. Run the test suite
java -cp "test-standalone/build;app/src/main/java" com.abztech.assistant.StandaloneBrainTest
```

**Output**:
```
==================================================
    AMARA OFFLINE AI ASSISTANT - ENGINE TEST      
==================================================
✓ Test 1 Passed: 25 * 4 -> [RESULT: 100]
✓ Test 2 Passed: 15% of 240 -> [RESULT: 36]
✓ Test 3 Passed: sqrt(144) -> [RESULT: 12]
✓ Test 4 Passed: Context memory math (12 + 50) -> [RESULT: 62]
✓ Test 5 Passed: [RESULT: 100°C is 212°F]
✓ Test 6 Passed: [FLASHLIGHT: ON]
✓ Test 7 Passed: [VOLUME: 80%]
✓ Test 8 Passed: [BATTERY: 85%]
✓ Test 9 Passed: [LAUNCHING: WhatsApp]
✓ Test 10 Passed: [TIMER: 10 minutes]
✓ Test 11 Passed: [NOTE SAVED: pick up dry cleaning]
✓ Test 12 Passed: The capital of France is Paris.
✓ Test 13 Passed: Joke: Why did the scarecrow win an award? Because he was outstanding in his field!
✓ Test 14 Passed: [FLASHLIGHT: OFF]
✓ Test 15 Passed: The speed of light in a vacuum is approximately 299,792,458 meters per second, or about 300,000 km/s.
✓ Test 16 Passed: I'm Amara, your personal offline assistant, built by AbzTech. I live directly on your phone and run 100% offline.
✓ Test 17 Passed: 'Hello Nova' wake-word -> Yes? What's on your mind?
✓ Test 18 Passed: 'Hello Nova open YouTube' -> [LAUNCHING: YouTube]
✓ Test 19 Passed: 'Hello Nova turn on flashlight' -> [FLASHLIGHT: ON]
✓ Test 20 Passed: 'Hey Jarvis, open WhatsApp' -> [LAUNCHING: WhatsApp]
✓ Test 21 Passed: 'How are you' human reply -> I'm doing great, thanks for asking! How about yourself?
✓ Test 22 Passed: 'What's up' human reply -> Not a whole lot, keeping things running smoothly. Anything interesting happening?
✓ Test 23 Passed: 'Display over screen' -> [OVERLAY: ACTIVE]
✓ Test 24 Passed: 'what's today's date' -> Today is Sunday, November 8, 2026.
✓ Test 25 Passed: 'close YouTube' -> [CLOSED: YouTube]
✓ Test 26 Passed: 'go to home screen' -> [HOME]
✓ Test 27 Passed: 'Hey Amara' wake-word -> Go ahead, I'm listening.
✓ Test 28 Passed: 'whats my battery percent' -> [BATTERY: 85%]
✓ Test 29 Passed: 'tell me the battery percentage' -> [BATTERY: 85%]
✓ Test 30 Passed: Identity contains Amara -> I'm Amara, your personal offline assistant, built by AbzTech. I live directly on your phone and run 100% offline.
✓ Test 31 Passed: 'Hey Amara, what's my battery percent?' -> [BATTERY: 85%]
✓ Test 32 Passed: Suggestion Chip 'Turn on Flashlight' -> [FLASHLIGHT: ON]
✓ Test 33 Passed: Suggestion Chip 'Turn off Flashlight' -> [FLASHLIGHT: OFF]
==================================================
RESULTS: 33 / 33 TESTS PASSED (100% OFFLINE)
==================================================
```

---

## Enabling 100% Offline Speech Recognition on Android

Android's built-in `SpeechRecognizer` runs offline when the offline language model is downloaded:
1. Open your Android phone's **Settings**.
2. Go to **System → Languages & input → Speech → Voice typing** (or search *"Offline speech recognition"* in Settings).
3. Under the **Offline** tab, ensure your language (e.g., **English (US)**) is downloaded (~40–50 MB).
4. Once downloaded, transcription works entirely offline in airplane mode with zero latency.

---

## Permissions Used

- `RECORD_AUDIO`: Continuous offline voice command recognition.
- `SYSTEM_ALERT_WINDOW`: Displaying the floating Arc Reactor HUD over other apps.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Running the floating overlay background service with active microphone capture.
- `KILL_BACKGROUND_PROCESSES`: Closing apps upon voice command.
- `CAMERA`: Flashlight / torch control via `CameraManager`.
- `SET_ALARM`: Scheduling alarms and countdown timers.
- `VIBRATE`: Haptic feedback upon speech recognition.
- `CALL_PHONE` / `SEND_SMS`: Placing calls and drafting text messages.
- `QUERY_ALL_PACKAGES`: Finding, launching, and closing installed apps on the device.

---

## License
Created for **AbzTech**. Open for personal and commercial adaptation.
