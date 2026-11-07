package com.abztech.assistant.speech;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;

/**
 * Handles offline Text-to-Speech playback with natural Siri-like vocal parameters.
 */
public class TextToSpeechManager implements TextToSpeech.OnInitListener {

    public interface SpeechCallback {
        void onSpeechStarted();
        void onSpeechCompleted();
        void onSpeechError(String error);
    }

    private final TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean isMuted = false;
    private SpeechCallback callback;

    public TextToSpeechManager(Context context, SpeechCallback callback) {
        this.callback = callback;
        this.tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }

            // Siri-like voice cadence: clean pitch and steady rate
            tts.setPitch(1.02f);
            tts.setSpeechRate(1.0f);

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (callback != null) callback.onSpeechStarted();
                }

                @Override
                public void onDone(String utteranceId) {
                    if (callback != null) callback.onSpeechCompleted();
                }

                @Override
                public void onError(String utteranceId) {
                    if (callback != null) callback.onSpeechError("TTS playback error.");
                }
            });

            isInitialized = true;
        } else {
            if (callback != null) callback.onSpeechError("Failed to initialize offline TTS engine.");
        }
    }

    public void speak(String text) {
        if (!isInitialized || isMuted || text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onSpeechCompleted();
            return;
        }

        stop();

        String utteranceId = "NOVA_SIRI_" + System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public void stop() {
        if (tts != null && isInitialized) {
            tts.stop();
        }
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (muted) stop();
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
