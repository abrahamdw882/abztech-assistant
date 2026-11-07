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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abztech.assistant.data.NotesDatabaseHelper;
import com.abztech.assistant.engine.ActionDispatcher;
import com.abztech.assistant.engine.AssistantBrain;
import com.abztech.assistant.model.ChatMessage;
import com.abztech.assistant.speech.SpeechRecognitionManager;
import com.abztech.assistant.speech.TextToSpeechManager;
import com.abztech.assistant.ui.AssistantState;
import com.abztech.assistant.ui.ChatAdapter;
import com.abztech.assistant.ui.JarvisVisualizerView;
import com.abztech.assistant.util.PermissionHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SpeechRecognitionManager.Listener,
        TextToSpeechManager.SpeechCallback {

    private JarvisVisualizerView visualizerView;
    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private FloatingActionButton btnMic;
    private ImageButton btnSend;
    private ImageButton btnMute;
    private EditText editInput;
    private TextView textStatus;
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

        // Initial Jarvis greeting
        addBotMessage("J.A.R.V.I.S. neural interface initialized. All systems online. Say 'Hello Nova' or 'Hey Jarvis' to give any command.", "SYSTEM: ONLINE");

        // Automatically activate hands-free listening if microphone permission is ready
        if (PermissionHelper.hasRecordAudioPermission(this)) {
            mainHandler.postDelayed(() -> {
                if (speechManager != null) {
                    speechManager.startListening();
                    setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
                }
            }, 600);
        }
    }

    private void initViews() {
        visualizerView = findViewById(R.id.visualizerView);
        recyclerChat = findViewById(R.id.recyclerChat);
        btnMic = findViewById(R.id.btnMic);
        btnSend = findViewById(R.id.btnSend);
        btnMute = findViewById(R.id.btnMute);
        editInput = findViewById(R.id.editInput);
        textStatus = findViewById(R.id.textStatus);
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);
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
                "⚡ Flashlight",
                "🚀 Open YouTube",
                "💬 Open WhatsApp",
                "🔋 Battery Status",
                "⏰ 5m Timer",
                "🧮 15% of 240",
                "💡 Fun Fact",
                "😂 Joke"
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
                String clean = s.replaceAll("^[^\u0000-\u007F]+\\s*", "");
                processUserQuery(clean);
            });
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

        // 3. Update HUD UI
        setAssistantState(AssistantState.LISTENING, "// LISTENING: AWAITING VOICE INPUT...");
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

        addUserMessage(query);
        setAssistantState(AssistantState.THINKING, "// NEURAL CORE: COMPUTING RESPONSE...");

        new Thread(() -> {
            AssistantBrain.Response response = assistantBrain.process(query);

            mainHandler.post(() -> {
                addBotMessage(response.displayText, response.actionTag);
                ttsManager.speak(response.spokenText);
            });
        }).start();
    }

    private void addUserMessage(String text) {
        chatMessages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerChat.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private void addBotMessage(String text, String actionTag) {
        chatMessages.add(new ChatMessage(text, false, actionTag));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerChat.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private void setAssistantState(AssistantState state, String statusText) {
        visualizerView.setState(state);
        textStatus.setText(statusText);
    }

    // SpeechRecognitionManager Callbacks
    @Override
    public void onReadyForSpeech() {
        setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
    }

    @Override
    public void onBeginningOfSpeech() {
        setAssistantState(AssistantState.LISTENING, "// VOICE DETECTED: RECORDING AUDIO STREAM...");
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
            setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
        }
    }

    @Override
    public void onPartialResults(String partialText) {
        if (partialText != null && !partialText.isEmpty()) {
            textStatus.setText("// " + partialText.toUpperCase());
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        // Auto-recovery is handled by SpeechRecognitionManager
    }

    @Override
    public void onEndOfSpeech() {
        setAssistantState(AssistantState.THINKING, "// DECODING TRANSMISSION...");
    }

    // TextToSpeech Callbacks
    @Override
    public void onSpeechStarted() {
        mainHandler.post(() -> setAssistantState(AssistantState.SPEAKING, "// VOCAL SYNTHESIS ACTIVE..."));
    }

    @Override
    public void onSpeechCompleted() {
        mainHandler.post(() -> {
            setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
            speechManager.resumeListening();
        });
    }

    @Override
    public void onSpeechError(String error) {
        mainHandler.post(() -> {
            setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
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
                setAssistantState(AssistantState.LISTENING, "// AWAITING INSTRUCTION: 'HELLO NOVA' OR 'JARVIS'");
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
