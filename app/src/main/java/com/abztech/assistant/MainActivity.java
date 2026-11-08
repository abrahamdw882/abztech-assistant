package com.abztech.assistant;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.abztech.assistant.data.NotesDatabaseHelper;
import com.abztech.assistant.engine.ActionDispatcher;
import com.abztech.assistant.engine.AssistantBrain;
import com.abztech.assistant.engine.IntentType;
import com.abztech.assistant.service.JarvisOverlayService;
import com.abztech.assistant.speech.SpeechRecognitionManager;
import com.abztech.assistant.speech.TextToSpeechManager;
import com.abztech.assistant.ui.AssistantState;
import com.abztech.assistant.ui.JarvisVisualizerView;
import com.abztech.assistant.util.PermissionHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements
        SpeechRecognitionManager.Listener,
        TextToSpeechManager.SpeechCallback {

    private JarvisVisualizerView visualizerView;
    private TextView textSpeakerTag;
    private TextView textDialogue;

    private FloatingActionButton btnMic;
    private ImageButton btnSend;
    private ImageButton btnMute;
    private ImageButton btnOverlay;
    private EditText editInput;
    private ChipGroup chipGroupSuggestions;

    private AssistantBrain assistantBrain;
    private SpeechRecognitionManager speechManager;
    private TextToSpeechManager ttsManager;
    private NotesDatabaseHelper notesDb;
    private long ttsStartTime = 0;
    private String lastBotSpokenText = "";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on so J.A.R.V.I.S. can stay continuously responsive on desk
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        initCoreEngine();
        initSuggestions();
        setupListeners();

        // Request audio & hardware permissions
        PermissionHelper.requestNecessaryPermissions(this);

        // Initial warm human greeting directly under the Arc Reactor
        setDialogue("ONLINE", "Hey there! I'm ready whenever you are. What's on your mind?");

        // Automatically activate hands-free listening if microphone permission is ready
        if (PermissionHelper.hasRecordAudioPermission(this)) {
            mainHandler.postDelayed(() -> {
                if (speechManager != null) {
                    speechManager.startListening();
                    visualizerView.setState(AssistantState.LISTENING);
                }
            }, 600);
        }
    }

    private void initViews() {
        visualizerView = findViewById(R.id.visualizerView);
        textSpeakerTag = findViewById(R.id.textSpeakerTag);
        textDialogue = findViewById(R.id.textDialogue);
        btnMic = findViewById(R.id.btnMic);
        btnSend = findViewById(R.id.btnSend);
        btnMute = findViewById(R.id.btnMute);
        btnOverlay = findViewById(R.id.btnOverlay);
        editInput = findViewById(R.id.editInput);
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions);
    }

    private void initCoreEngine() {
        notesDb = new NotesDatabaseHelper(this);
        ActionDispatcher actionDispatcher = new ActionDispatcher(this, notesDb);
        assistantBrain = new AssistantBrain(actionDispatcher);

        ttsManager = new TextToSpeechManager(this, this);
        speechManager = new SpeechRecognitionManager(this, this);
        speechManager.setContinuousMode(true);
    }

    private void initSuggestions() {
        String[] suggestions = new String[]{
                "Display Over Screen",
                "How are you?",
                "What's up?",
                "Turn on Flashlight",
                "Turn off Flashlight",
                "Open YouTube",
                "Close YouTube",
                "Open WhatsApp",
                "What's my battery percent?",
                "Go to Home Screen",
                "Set 5m Timer",
                "15% of 240",
                "Tell a Joke"
        };

        chipGroupSuggestions.removeAllViews();
        for (String s : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setChipBackgroundColorResource(R.color.chip_bg);
            chip.setChipStrokeColorResource(R.color.chip_stroke);
            chip.setChipStrokeWidth(1.5f);
            chip.setTextColor(getColor(R.color.text_primary));
            chip.setOnClickListener(v -> {
                if ("Display Over Screen".equalsIgnoreCase(s)) {
                    launchOverlayMode();
                } else {
                    processUserQuery(s);
                }
            });
            chipGroupSuggestions.addView(chip);
        }
    }

    private void setupListeners() {
        // Tapping the Arc Reactor or Dialogue area directly interrupts & listens!
        visualizerView.setOnClickListener(v -> interruptAndListen());
        textDialogue.setOnClickListener(v -> interruptAndListen());

        // Tapping the Mic button interrupts & listens!
        btnMic.setOnClickListener(v -> interruptAndListen());

        btnSend.setOnClickListener(v -> submitTypedQuery());

        editInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitTypedQuery();
                return true;
            }
            return false;
        });

        btnMute.setOnClickListener(v -> {
            boolean muted = !ttsManager.isMuted();
            ttsManager.setMuted(muted);
            btnMute.setImageResource(muted ? R.drawable.ic_volume_off : R.drawable.ic_volume);
            Toast.makeText(this, muted ? "Audio output muted" : "Audio output online", Toast.LENGTH_SHORT).show();
        });

        // Launch floating overlay over other apps
        btnOverlay.setOnClickListener(v -> launchOverlayMode());
    }

    public void launchOverlayMode() {
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Please enable 'Display over other apps' to activate floating Arc Reactor.", Toast.LENGTH_LONG).show();
            PermissionHelper.requestOverlayPermission(this);
            return;
        }

        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            Toast.makeText(this, "Microphone permission required for voice overlay.", Toast.LENGTH_SHORT).show();
            PermissionHelper.requestNecessaryPermissions(this);
            return;
        }

        // Destroy speech recognizer in main activity so overlay service has 100% exclusive access to the microphone
        if (speechManager != null) {
            speechManager.destroy();
            speechManager = null;
        }
        if (ttsManager != null) {
            ttsManager.stop();
        }

        try {
            Intent serviceIntent = new Intent(this, JarvisOverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            // Postpone moveTaskToBack slightly so foreground service starts safely without Android 14 FGS background violation
            mainHandler.postDelayed(() -> {
                try {
                    moveTaskToBack(true);
                } catch (Exception ignored) {}
            }, 350);
        } catch (Exception e) {
            try {
                startService(new Intent(this, JarvisOverlayService.class));
            } catch (Exception e2) {
                Toast.makeText(this, "Could not start overlay: " + e2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionHelper.REQUEST_CODE_OVERLAY) {
            if (PermissionHelper.hasOverlayPermission(this)) {
                Toast.makeText(this, "Overlay permission granted. Launching floating mode.", Toast.LENGTH_SHORT).show();
                launchOverlayMode();
            } else {
                Toast.makeText(this, "Permission required to display floating Arc Reactor over other apps.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Unified interruption handler. Instantly stops TTS, breaks any lock,
     * and forces the speech recognizer to listen immediately.
     */
    private void interruptAndListen() {
        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            PermissionHelper.requestNecessaryPermissions(this);
            return;
        }

        // 1. Immediately cut off audio playback
        ttsManager.stop();

        // 2. Force speech recognition reset and listen
        speechManager.forceListenNow();

        // 3. Update Visualizer & HUD Dialogue
        visualizerView.setState(AssistantState.LISTENING);
        setDialogue("LISTENING", "Awaiting voice input...");
    }

    private void submitTypedQuery() {
        String text = editInput.getText().toString().trim();
        if (!text.isEmpty()) {
            editInput.setText("");
            processUserQuery(text);
        }
    }

    private void processUserQuery(String query) {
        // Cut off audio and pause microphone so Jarvis never listens to its own voice
        ttsManager.stop();
        speechManager.pauseListening();

        setDialogue("YOU", query);
        visualizerView.setState(AssistantState.THINKING);

        new Thread(() -> {
            AssistantBrain.Response response = assistantBrain.process(query);

            mainHandler.post(() -> {
                String tag = (response.actionTag != null && !response.actionTag.isEmpty())
                        ? response.actionTag
                        : "ULTRON";
                setDialogue(tag, response.displayText);

                lastBotSpokenText = response.spokenText != null ? response.spokenText : "";
                ttsStartTime = System.currentTimeMillis();
                ttsManager.speak(response.spokenText);

                if (response.intentType == IntentType.OVERLAY_MODE) {
                    mainHandler.postDelayed(this::launchOverlayMode, 1200);
                }
            });
        }).start();
    }

    private void setDialogue(String speaker, String text) {
        if (textSpeakerTag != null) {
            textSpeakerTag.setText(speaker != null ? speaker.toUpperCase() : "ULTRON");
        }
        if (textDialogue != null) {
            textDialogue.setText(text);
        }
    }

    // SpeechRecognitionManager Callbacks
    @Override
    public void onReadyForSpeech() {
        visualizerView.setState(AssistantState.LISTENING);
    }

    @Override
    public void onBeginningOfSpeech() {
        // If user begins speaking, cut off any remaining TTS audio
        if (ttsManager != null && ttsManager.isSpeaking() && (System.currentTimeMillis() - ttsStartTime > 250)) {
            ttsManager.stop();
        }
        visualizerView.setState(AssistantState.LISTENING);
        setDialogue("YOU", "Listening...");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        visualizerView.setAudioLevel(rmsdB);
    }

    @Override
    public void onResults(String recognizedText) {
        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
            if (isSelfEcho(recognizedText)) {
                // Detected speech is an acoustic echo of Jarvis's own voice; discard it and keep listening!
                speechManager.resumeListening();
                return;
            }
            processUserQuery(recognizedText);
        } else {
            speechManager.resumeListening();
            visualizerView.setState(AssistantState.LISTENING);
        }
    }

    private boolean isSelfEcho(String detected) {
        if (detected == null || lastBotSpokenText == null || lastBotSpokenText.isEmpty()) return false;
        String cleanDetected = detected.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String cleanBot = lastBotSpokenText.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (cleanDetected.length() < 3 || cleanBot.length() < 3) return false;
        return cleanBot.contains(cleanDetected) || cleanDetected.contains(cleanBot);
    }

    @Override
    public void onPartialResults(String partialText) {
        if (ttsManager != null && ttsManager.isSpeaking()) {
            ttsManager.stop();
        }
        if (partialText != null && !partialText.trim().isEmpty()) {
            setDialogue("YOU", partialText);
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        // Auto-recovery is handled by SpeechRecognitionManager
    }

    @Override
    public void onEndOfSpeech() {
        visualizerView.setState(AssistantState.THINKING);
    }

    // TextToSpeech Callbacks
    @Override
    public void onSpeechStarted() {
        mainHandler.post(() -> visualizerView.setState(AssistantState.SPEAKING));
    }

    @Override
    public void onSpeechCompleted() {
        mainHandler.post(() -> {
            visualizerView.setState(AssistantState.LISTENING);
            speechManager.resumeListening();
        });
    }

    @Override
    public void onSpeechError(String error) {
        mainHandler.post(() -> {
            visualizerView.setState(AssistantState.LISTENING);
            speechManager.resumeListening();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_PERMISSIONS) {
            boolean audioGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.RECORD_AUDIO) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    audioGranted = true;
                    break;
                }
            }
            if (audioGranted) {
                speechManager.startListening();
                visualizerView.setState(AssistantState.LISTENING);
            } else {
                Toast.makeText(this, "Microphone permission required for voice activation.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (JarvisOverlayService.isRunning) {
            stopService(new Intent(this, JarvisOverlayService.class));
        }
        if (speechManager == null) {
            speechManager = new SpeechRecognitionManager(this, this);
            speechManager.setContinuousMode(true);
        }
        if (PermissionHelper.hasRecordAudioPermission(this)) {
            speechManager.resumeListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechManager != null) {
            speechManager.pauseListening();
        }
        if (ttsManager != null) {
            ttsManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechManager != null) speechManager.destroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (notesDb != null) notesDb.close();
    }
}
