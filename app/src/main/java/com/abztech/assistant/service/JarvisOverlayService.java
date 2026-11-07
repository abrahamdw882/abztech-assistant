package com.abztech.assistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;

import com.abztech.assistant.MainActivity;
import com.abztech.assistant.R;
import com.abztech.assistant.data.NotesDatabaseHelper;
import com.abztech.assistant.engine.ActionDispatcher;
import com.abztech.assistant.engine.AssistantBrain;
import com.abztech.assistant.speech.SpeechRecognitionManager;
import com.abztech.assistant.speech.TextToSpeechManager;
import com.abztech.assistant.ui.AssistantState;
import com.abztech.assistant.ui.JarvisVisualizerView;
import com.abztech.assistant.util.PermissionHelper;

/**
 * System Overlay Service for J.A.R.V.I.S.
 * Displays the holographic Arc Reactor and dialogue HUD directly over other apps.
 */
public class JarvisOverlayService extends Service implements
        SpeechRecognitionManager.Listener,
        TextToSpeechManager.SpeechCallback {

    private static final String CHANNEL_ID = "jarvis_overlay_channel";
    private static final int NOTIFICATION_ID = 2001;

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams windowParams;

    private JarvisVisualizerView visualizerView;
    private TextView textSpeakerTag;
    private TextView textDialogue;

    private SpeechRecognitionManager speechManager;
    private TextToSpeechManager ttsManager;
    private AssistantBrain assistantBrain;
    private NotesDatabaseHelper notesDb;
    private long ttsStartTime = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        // Verify overlay permission before doing anything
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        try {
            startForegroundServiceNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initCoreEngine();
            initOverlayWindow();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return;
        }

        // Greet user on overlay launch
        setDialogue("ONLINE", "Hey there! I'm floating right here. How can I help?");

        // Start hands-free speech recognition if audio permission is granted
        if (PermissionHelper.hasRecordAudioPermission(this)) {
            mainHandler.postDelayed(() -> {
                try {
                    if (speechManager != null) {
                        speechManager.startListening();
                        if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 600);
        }
    }

    private void startForegroundServiceNotification() {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "J.A.R.V.I.S. Overlay Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps the floating Arc Reactor assistant responsive over other apps.");
                manager.createNotificationChannel(channel);
            }

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("J.A.R.V.I.S. Floating Assistant")
                    .setContentText("Displaying over screen. Tap to open full app.")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                if (PermissionHelper.hasRecordAudioPermission(this)) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            try {
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("J.A.R.V.I.S. Assistant")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();
                startForeground(NOTIFICATION_ID, notification);
            } catch (Exception ignored) {}
        }
    }

    private void initCoreEngine() {
        notesDb = new NotesDatabaseHelper(this);
        ActionDispatcher actionDispatcher = new ActionDispatcher(this, notesDb);
        assistantBrain = new AssistantBrain(actionDispatcher);

        ttsManager = new TextToSpeechManager(this, this);
        speechManager = new SpeechRecognitionManager(this, this);
        speechManager.setContinuousMode(true);
    }

    private void initOverlayWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.CENTER;

        // Wrap Service context with App Theme to prevent inflation crashes
        Context themedContext = new ContextThemeWrapper(this, R.style.Theme_NovaAssistant);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.layout_overlay_jarvis, null);

        visualizerView = overlayView.findViewById(R.id.overlayVisualizerView);
        textSpeakerTag = overlayView.findViewById(R.id.overlaySpeakerTag);
        textDialogue = overlayView.findViewById(R.id.overlayDialogue);
        LinearLayout overlayCard = overlayView.findViewById(R.id.overlayCard);
        ImageButton btnClose = overlayView.findViewById(R.id.btnOverlayClose);
        ImageButton btnExpand = overlayView.findViewById(R.id.btnOverlayExpand);

        // Tap Arc Reactor to interrupt & listen
        visualizerView.setOnClickListener(v -> interruptAndListen());

        // Close overlay button
        btnClose.setOnClickListener(v -> stopSelf());

        // Expand back to full activity
        btnExpand.setOnClickListener(v -> {
            try {
                Intent appIntent = new Intent(this, MainActivity.class);
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(appIntent);
            } catch (Exception ignored) {}
            stopSelf();
        });

        // Smooth dragging for the floating card across the screen
        overlayCard.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isMoved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = windowParams.x;
                        initialY = windowParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            isMoved = true;
                            windowParams.x = initialX + dx;
                            windowParams.y = initialY + dy;
                            if (windowManager != null && overlayView != null) {
                                try {
                                    windowManager.updateViewLayout(overlayView, windowParams);
                                } catch (Exception ignored) {}
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        return isMoved;
                }
                return false;
            }
        });

        windowManager.addView(overlayView, windowParams);
    }

    private void interruptAndListen() {
        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            return;
        }
        if (ttsManager != null) ttsManager.stop();
        if (speechManager != null) speechManager.forceListenNow();
        if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
        setDialogue("LISTENING", "Awaiting voice input...");
    }

    private void processQuery(String query) {
        if (ttsManager != null) ttsManager.stop();

        setDialogue("YOU", query);
        if (visualizerView != null) visualizerView.setState(AssistantState.THINKING);

        new Thread(() -> {
            AssistantBrain.Response response = assistantBrain.process(query);

            mainHandler.post(() -> {
                String tag = (response.actionTag != null && !response.actionTag.isEmpty())
                        ? response.actionTag
                        : "J.A.R.V.I.S.";
                setDialogue(tag, response.displayText);
                ttsStartTime = System.currentTimeMillis();
                if (ttsManager != null) ttsManager.speak(response.spokenText);

                if (response.intentType == com.abztech.assistant.engine.IntentType.WAKE_WORD) {
                    if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
                    if (speechManager != null) speechManager.resumeListening();
                } else {
                    if (speechManager != null) speechManager.resumeListening();
                }
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
        if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
    }

    @Override
    public void onBeginningOfSpeech() {
        // Voice Barge-in: if assistant is speaking and user speaks, cut off TTS immediately!
        if (ttsManager != null && ttsManager.isSpeaking() && (System.currentTimeMillis() - ttsStartTime > 250)) {
            ttsManager.stop();
        }
        if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
        setDialogue("YOU", "Listening...");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        if (visualizerView != null) visualizerView.setAudioLevel(rmsdB);
    }

    @Override
    public void onResults(String recognizedText) {
        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
            processQuery(recognizedText);
        } else {
            if (speechManager != null) speechManager.resumeListening();
            if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
        }
    }

    @Override
    public void onPartialResults(String partialText) {
        // Voice Barge-in: user spoke while TTS was active
        if (ttsManager != null && ttsManager.isSpeaking()) {
            ttsManager.stop();
        }
        if (partialText != null && !partialText.trim().isEmpty()) {
            setDialogue("YOU", partialText);
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        // SpeechManager self-heals
    }

    @Override
    public void onEndOfSpeech() {
        if (visualizerView != null) visualizerView.setState(AssistantState.THINKING);
    }

    // TextToSpeech Callbacks
    @Override
    public void onSpeechStarted() {
        mainHandler.post(() -> {
            if (visualizerView != null) visualizerView.setState(AssistantState.SPEAKING);
        });
    }

    @Override
    public void onSpeechCompleted() {
        mainHandler.post(() -> {
            if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
            if (speechManager != null) speechManager.resumeListening();
        });
    }

    @Override
    public void onSpeechError(String error) {
        mainHandler.post(() -> {
            if (visualizerView != null) visualizerView.setState(AssistantState.LISTENING);
            if (speechManager != null) speechManager.resumeListening();
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
            overlayView = null;
        }

        if (speechManager != null) {
            try {
                speechManager.destroy();
            } catch (Exception ignored) {}
            speechManager = null;
        }

        if (ttsManager != null) {
            try {
                ttsManager.shutdown();
            } catch (Exception ignored) {}
            ttsManager = null;
        }

        if (notesDb != null) {
            try {
                notesDb.close();
            } catch (Exception ignored) {}
            notesDb = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
