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
 * Handles speech-to-text recognition configured for offline-preferred execution
 * with continuous hands-free listening loop support.
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable restartRunnable = new Runnable() {
        @Override
        public void run() {
            if (continuousMode && !isPaused && !isListening) {
                startListening();
            }
        }
    };

    public SpeechRecognitionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initRecognizer();
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }

        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(this);
        }
    }

    public synchronized void startListening() {
        if (isPaused) return;

        mainHandler.removeCallbacks(restartRunnable);

        if (speechRecognizer == null) {
            initRecognizer();
            if (speechRecognizer == null) {
                if (listener != null) listener.onError(-1, "Speech recognition is not available.");
                return;
            }
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            isListening = false;
            if (continuousMode && !isPaused) {
                scheduleRestart(500);
            } else if (listener != null) {
                listener.onError(-2, "Could not start listening.");
            }
        }
    }

    public synchronized void stopListening() {
        mainHandler.removeCallbacks(restartRunnable);
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    public synchronized void cancel() {
        mainHandler.removeCallbacks(restartRunnable);
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    public void pauseListening() {
        isPaused = true;
        cancel();
    }

    public void resumeListening() {
        isPaused = false;
        if (continuousMode) {
            scheduleRestart(200);
        }
    }

    public void scheduleRestart(long delayMs) {
        if (!continuousMode || isPaused) return;
        mainHandler.removeCallbacks(restartRunnable);
        mainHandler.postDelayed(restartRunnable, delayMs);
    }

    public boolean isListening() {
        return isListening;
    }

    public void setContinuousMode(boolean enabled) {
        this.continuousMode = enabled;
        if (!enabled) {
            mainHandler.removeCallbacks(restartRunnable);
        } else if (!isListening && !isPaused) {
            startListening();
        }
    }

    public boolean isContinuousMode() {
        return continuousMode;
    }

    public void destroy() {
        mainHandler.removeCallbacks(restartRunnable);
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
        if (listener != null) listener.onReadyForSpeech();
    }

    @Override
    public void onBeginningOfSpeech() {
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
        if (listener != null) listener.onEndOfSpeech();
    }

    @Override
    public void onError(int error) {
        isListening = false;

        // Normal pauses in room conversation — seamlessly rearm without showing error
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            if (continuousMode && !isPaused) {
                scheduleRestart(300);
                return;
            }
        }

        // Busy or client error — reset recognizer and restart
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            if (continuousMode && !isPaused) {
                initRecognizer();
                scheduleRestart(500);
                return;
            }
        }

        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error.";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Microphone permission required.";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout (Ensure offline voice pack is enabled).";
                break;
            default:
                message = "Listening interrupted.";
                break;
        }

        if (continuousMode && !isPaused) {
            scheduleRestart(1000);
        }

        if (listener != null) listener.onError(error, message);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
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
                scheduleRestart(250);
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
