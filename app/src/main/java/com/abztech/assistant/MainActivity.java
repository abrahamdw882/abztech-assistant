package com.abztech.assistant;

import android.content.pm.PackageManager;
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
    private EditText editInput;
    private ChipGroup chipGroupSuggestions;

    private AssistantBrain assistantBrain;
    private SpeechRecognitionManager speechManager;
    private TextToSpeechManager ttsManager;
    private NotesDatabaseHelper notesDb;

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
                "How are you?",
                "What's up?",
                "Flashlight",
                "Open YouTube",
                "Open WhatsApp",
                "Battery Status",
                "Set 5m Timer",
                "15% of 240",
                "Tell a Joke",
                "Fun Fact"
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
            chip.setOnClickListener(v -> processUserQuery(s));
            chipGroupSuggestions.addView(chip);
        }
    }

    private void setupListeners() {
        // Tapping the Arc Reactor directly interrupts & listens!
        visualizerView.setOnClickListener(v -> interruptAndListen());

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
        setDialogue("LISTENING", "// Awaiting voice input...");
    }

    private void submitTypedQuery() {
        String text = editInput.getText().toString().trim();
        if (!text.isEmpty()) {
            editInput.setText("");
            processUserQuery(text);
        }
    }

    private void processUserQuery(String query) {
        // Stop audio playback and pause microphone so Jarvis doesn't hear itself
        ttsManager.stop();
        speechManager.pauseListening();

        setDialogue("YOU", query);
        visualizerView.setState(AssistantState.THINKING);

        new Thread(() -> {
            AssistantBrain.Response response = assistantBrain.process(query);

            mainHandler.post(() -> {
                String tag = (response.actionTag != null && !response.actionTag.isEmpty())
                        ? response.actionTag
                        : "J.A.R.V.I.S.";
                setDialogue(tag, response.displayText);
                ttsManager.speak(response.spokenText);
            });
        }).start();
    }

    private void setDialogue(String speaker, String text) {
        if (textSpeakerTag != null) {
            textSpeakerTag.setText(speaker != null ? speaker.toUpperCase() : "J.A.R.V.I.S.");
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
        visualizerView.setState(AssistantState.LISTENING);
        setDialogue("YOU", "// Listening...");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        visualizerView.setAudioLevel(rmsdB);
    }

    @Override
    public void onResults(String recognizedText) {
        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
            processUserQuery(recognizedText);
        } else {
            speechManager.resumeListening();
            visualizerView.setState(AssistantState.LISTENING);
        }
    }

    @Override
    public void onPartialResults(String partialText) {
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
