package com.abztech.assistant.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/**
 * High-reliability, self-healing Speech Recognition Manager.
 * Solves state-machine deadlocks during interruptions, auto-recovers from Android
 * SpeechRecognizer busy states, and maintains hands-free continuous listening.
 */
public class SpeechRecognitionManager implements RecognitionListener {

    public interface Listener {
        void onReadyForSpeech();
        void onBeginningOfSpeech();
        void onRmsChanged(float rmsdB);
        void onResults(String recognizedText);
        void onPartialResults(String partialText);
        void onError(int errorCode, String errorMessage);
        void onEndOfSpeech();
    }

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean continuousMode = true;
    private boolean isPaused = false;
    private long lastListeningStartTime = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable restartRunnable = new Runnable() {
        @Override
        public void run() {
            if (continuousMode && !isPaused && !isListening) {
                startListening();
            }
        }
    };

    // Watchdog to auto-recover if Android SpeechRecognizer gets stuck
    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            if (isListening && (System.currentTimeMillis() - lastListeningStartTime > 8000)) {
                // If stuck in listening state for > 8 seconds without any speech callbacks, reset!
                resetRecognizer();
                if (continuousMode && !isPaused) {
                    startListening();
                }
            }
        }
    };

    public SpeechRecognitionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initRecognizer();
    }

    private synchronized void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
        } catch (Exception ignored) {}

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            if (speechRecognizer != null) {
                speechRecognizer.setRecognitionListener(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void resetRecognizer() {
        isListening = false;
        mainHandler.removeCallbacks(watchdogRunnable);
        mainHandler.removeCallbacks(restartRunnable);
        try {
            if (speechRecognizer != null) {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
        } catch (Exception ignored) {}
        initRecognizer();
    }

    public synchronized void startListening() {
        // Explicit call to start listening clears any pause block!
        this.isPaused = false;
        mainHandler.removeCallbacks(restartRunnable);
        mainHandler.removeCallbacks(watchdogRunnable);

        if (speechRecognizer == null) {
            initRecognizer();
            if (speechRecognizer == null) {
                if (listener != null) listener.onError(-1, "Speech recognition unavailable.");
                return;
            }
        }

        // Cancel any pending lingering recognition session
        try {
            speechRecognizer.cancel();
        } catch (Exception ignored) {}

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        // Generous silence threshold so the assistant waits patiently for the user to speak
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
            lastListeningStartTime = System.currentTimeMillis();
            mainHandler.postDelayed(watchdogRunnable, 8500);
        } catch (Exception e) {
            isListening = false;
            resetRecognizer();
            if (continuousMode) {
                scheduleRestart(400);
            } else if (listener != null) {
                listener.onError(-2, "Could not start microphone.");
            }
        }
    }

    public synchronized void stopListening() {
        mainHandler.removeCallbacks(watchdogRunnable);
        mainHandler.removeCallbacks(restartRunnable);
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    public synchronized void cancel() {
        mainHandler.removeCallbacks(watchdogRunnable);
        mainHandler.removeCallbacks(restartRunnable);
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    /**
     * Pause listening while Nova/Jarvis speaks back aloud so it doesn't hear itself.
     */
    public synchronized void pauseListening() {
        this.isPaused = true;
        cancel();
    }

    /**
     * Resume listening after speech output finishes or user taps to interrupt.
     */
    public synchronized void resumeListening() {
        this.isPaused = false;
        if (continuousMode) {
            scheduleRestart(150);
        }
    }

    /**
     * Instantly interrupts any speech and forcefully re-arms speech recognition.
     */
    public synchronized void forceListenNow() {
        this.isPaused = false;
        resetRecognizer();
        startListening();
    }

    public void scheduleRestart(long delayMs) {
        if (!continuousMode || isPaused) return;
        mainHandler.removeCallbacks(restartRunnable);
        mainHandler.postDelayed(restartRunnable, Math.max(50, delayMs));
    }

    public boolean isListening() {
        return isListening;
    }

    public void setContinuousMode(boolean enabled) {
        this.continuousMode = enabled;
        if (!enabled) {
            mainHandler.removeCallbacks(restartRunnable);
            mainHandler.removeCallbacks(watchdogRunnable);
        } else if (!isListening && !isPaused) {
            startListening();
        }
    }

    public boolean isContinuousMode() {
        return continuousMode;
    }

    public void destroy() {
        mainHandler.removeCallbacks(restartRunnable);
        mainHandler.removeCallbacks(watchdogRunnable);
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    // RecognitionListener Callbacks
    @Override
    public void onReadyForSpeech(Bundle params) {
        isListening = true;
        mainHandler.removeCallbacks(watchdogRunnable);
        if (listener != null) listener.onReadyForSpeech();
    }

    @Override
    public void onBeginningOfSpeech() {
        isListening = true;
        mainHandler.removeCallbacks(watchdogRunnable);
        if (listener != null) listener.onBeginningOfSpeech();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        if (listener != null) listener.onRmsChanged(rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {
        isListening = false;
        mainHandler.removeCallbacks(watchdogRunnable);
        if (listener != null) listener.onEndOfSpeech();
    }

    @Override
    public void onError(int error) {
        isListening = false;
        mainHandler.removeCallbacks(watchdogRunnable);

        // Normal silence/timeout in room — seamlessly rearm without user penalty
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            if (continuousMode && !isPaused) {
                scheduleRestart(100);
                return;
            }
        }

        // For any other error state (busy, client, glitch), reset recognizer and restart so it never freezes
        if (continuousMode && !isPaused) {
            resetRecognizer();
            scheduleRestart(250);
            return;
        }

        if (listener != null) listener.onError(error, "Speech error " + error);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        mainHandler.removeCallbacks(watchdogRunnable);

        String detected = null;
        if (results != null) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                detected = matches.get(0);
            }
        }

        if (detected != null && !detected.trim().isEmpty()) {
            if (listener != null) listener.onResults(detected);
        } else {
            if (continuousMode && !isPaused) {
                scheduleRestart(200);
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (partialResults != null && listener != null) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                listener.onPartialResults(matches.get(0));
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}
}
