package com.abztech.assistant.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Handles speech-to-text recognition configured for offline-preferred execution.
 * Streams audio amplitude (RMS dB) for Siri waveform visualization.
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

    public SpeechRecognitionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initRecognizer();
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            // Android 12+ on-device recognition
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }

        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(this);
        }
    }

    public void startListening() {
        if (speechRecognizer == null) {
            initRecognizer();
            if (speechRecognizer == null) {
                if (listener != null) listener.onError(-1, "Speech recognition is not available on this device.");
                return;
            }
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        // Tell the OS to prioritize the local offline language pack
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            if (listener != null) listener.onError(-2, "Could not start listening: " + e.getMessage());
            isListening = false;
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    public void cancel() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
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
        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error.";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error.";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Microphone permission required.";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network error. Make sure an offline voice pack is enabled in device settings.";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No speech detected. Please try again.";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Recognition service busy.";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input.";
                break;
            default:
                message = "Speech recognition error (" + error + ").";
                break;
        }
        if (listener != null) listener.onError(error, message);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        if (results != null && listener != null) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                listener.onResults(matches.get(0));
                return;
            }
        }
        if (listener != null) listener.onResults("");
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
