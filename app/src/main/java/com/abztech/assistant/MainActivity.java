package com.abztech.assistant;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.abztech.assistant.ui.SiriWaveVisualizerView;
import com.abztech.assistant.util.PermissionHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SpeechRecognitionManager.Listener,
        TextToSpeechManager.SpeechCallback {

    private SiriWaveVisualizerView visualizerView;
    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private FloatingActionButton btnMic;
    private ImageButton btnSend;
    private ImageButton btnMute;
    private TextView btnHandsFree;
    private EditText editInput;
    private TextView textStatus;
    private ChipGroup chipGroupSuggestions;

    private AssistantBrain assistantBrain;
    private SpeechRecognitionManager speechManager;
    private TextToSpeechManager ttsManager;
    private NotesDatabaseHelper notesDb;

    private boolean isHandsFreeActive = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen awake while in Hands-Free mode so Nova can hear "Hello Nova"
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        initCoreEngine();
        initSuggestions();
        setupListeners();

        // Request audio & hardware permissions
        PermissionHelper.requestNecessaryPermissions(this);

        // Add initial greeting
        addBotMessage("Hi! Hands-free mode is ACTIVE. Just say 'Hello Nova' or give any command without clicking!", "HANDS_FREE_ACTIVE");

        // If audio permission is already granted, start listening immediately
        if (PermissionHelper.hasRecordAudioPermission(this)) {
            mainHandler.postDelayed(() -> {
                if (speechManager != null && isHandsFreeActive) {
                    speechManager.startListening();
                    setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
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
        btnHandsFree = findViewById(R.id.btnHandsFree);
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
                "👋 Hello Nova",
                "🔦 Turn on flashlight",
                "🚀 Open YouTube",
                "⏰ Set timer for 5 minutes",
                "🧮 What is 15% of 240?",
                "🔋 Battery status",
                "😂 Tell me a joke",
                "📝 What are my notes?"
        };

        chipGroupSuggestions.removeAllViews();
        for (String s : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                String clean = s.replaceAll("^[^\u0000-\u007F]+\\s*", "");
                processUserQuery(clean);
            });
            chipGroupSuggestions.addView(chip);
        }
    }

    private void setupListeners() {
        // Hands-Free Toggle Pill
        btnHandsFree.setOnClickListener(v -> toggleHandsFreeMode());

        // Microphone Button
        btnMic.setOnClickListener(v -> {
            if (speechManager.isListening()) {
                speechManager.stopListening();
                setAssistantState(AssistantState.IDLE, "Tap mic to speak");
            } else {
                if (!PermissionHelper.hasRecordAudioPermission(this)) {
                    PermissionHelper.requestNecessaryPermissions(this);
                    return;
                }
                ttsManager.stop();
                speechManager.startListening();
            }
        });

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
            Toast.makeText(this, muted ? "Voice muted" : "Voice enabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleHandsFreeMode() {
        isHandsFreeActive = !isHandsFreeActive;
        speechManager.setContinuousMode(isHandsFreeActive);

        if (isHandsFreeActive) {
            btnHandsFree.setText("🎙️ HANDS-FREE: ON");
            btnHandsFree.setTextColor(ContextCompat.getColor(this, R.color.siri_cyan));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            speechManager.startListening();
            setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
            Toast.makeText(this, "Hands-Free Activated: Just say 'Hello Nova'!", Toast.LENGTH_SHORT).show();
        } else {
            btnHandsFree.setText("🎙️ TAP TO TALK");
            btnHandsFree.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            speechManager.stopListening();
            setAssistantState(AssistantState.IDLE, "Tap mic to speak");
            Toast.makeText(this, "Hands-Free Paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitTypedQuery() {
        String text = editInput.getText().toString().trim();
        if (!text.isEmpty()) {
            editInput.setText("");
            processUserQuery(text);
        }
    }

    private void processUserQuery(String query) {
        // Pause listening loop while assistant processes and speaks so it doesn't hear itself
        speechManager.pauseListening();

        addUserMessage(query);
        setAssistantState(AssistantState.THINKING, "Thinking...");

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
        if (isHandsFreeActive) {
            setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
        } else {
            setAssistantState(AssistantState.LISTENING, "Listening...");
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        setAssistantState(AssistantState.LISTENING, "Listening...");
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
            if (isHandsFreeActive) {
                speechManager.resumeListening();
                setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
            } else {
                setAssistantState(AssistantState.IDLE, "Tap mic to speak");
            }
        }
    }

    @Override
    public void onPartialResults(String partialText) {
        if (partialText != null && !partialText.isEmpty()) {
            textStatus.setText(partialText);
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        if (!isHandsFreeActive) {
            setAssistantState(AssistantState.IDLE, errorMessage);
        }
    }

    @Override
    public void onEndOfSpeech() {
        setAssistantState(AssistantState.THINKING, "Processing...");
    }

    // TextToSpeech Callbacks
    @Override
    public void onSpeechStarted() {
        mainHandler.post(() -> setAssistantState(AssistantState.SPEAKING, "Speaking..."));
    }

    @Override
    public void onSpeechCompleted() {
        mainHandler.post(() -> {
            if (isHandsFreeActive) {
                setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
                speechManager.resumeListening();
            } else {
                setAssistantState(AssistantState.IDLE, "Tap mic to speak");
            }
        });
    }

    @Override
    public void onSpeechError(String error) {
        mainHandler.post(() -> {
            if (isHandsFreeActive) {
                speechManager.resumeListening();
                setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
            } else {
                setAssistantState(AssistantState.IDLE, "Tap mic to speak");
            }
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
            if (audioGranted && isHandsFreeActive) {
                speechManager.startListening();
                setAssistantState(AssistantState.LISTENING, "🎙️ Listening for 'Hello Nova'...");
            } else if (!audioGranted) {
                Toast.makeText(this, "Microphone permission is required for voice activation.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isHandsFreeActive && PermissionHelper.hasRecordAudioPermission(this)) {
            speechManager.resumeListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechManager != null) {
            speechManager.pauseListening();
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
