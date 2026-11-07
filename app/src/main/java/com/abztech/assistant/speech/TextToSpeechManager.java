package com.abztech.assistant.speech;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

/**
 * Handles offline Text-to-Speech playback with realistic, human-like voice selection,
 * natural pitch/cadence tuning, and speech sanitization.
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
            Locale targetLocale = Locale.getDefault();
            int result = tts.setLanguage(targetLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                targetLocale = Locale.US;
                tts.setLanguage(targetLocale);
            }

            // Select the most natural, human-sounding offline voice available on the device
            selectBestHumanVoice(targetLocale);

            // Natural human pitch (0.98f) and relaxed conversational cadence (0.98f)
            // Prevents high-pitched squeakiness or robotic machine-gun delivery
            tts.setPitch(0.98f);
            tts.setSpeechRate(0.98f);

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
                    if (callback != null) callback.onSpeechError("TTS playback interrupted.");
                }
            });

            isInitialized = true;
        } else {
            if (callback != null) callback.onSpeechError("Failed to initialize offline TTS engine.");
        }
    }

    /**
     * Inspects installed voice packs and selects the highest fidelity offline natural voice.
     */
    private void selectBestHumanVoice(Locale locale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null || voices.isEmpty()) return;

            Voice bestVoice = null;
            int highestScore = -1;

            String lang = locale.getLanguage();

            for (Voice v : voices) {
                // Must match language
                if (v.getLocale() == null || !v.getLocale().getLanguage().equalsIgnoreCase(lang)) {
                    continue;
                }

                // Prioritize local, offline-capable voices (no network lag)
                if (v.isNetworkConnectionRequired()) {
                    continue;
                }

                int score = 0;

                // Quality evaluation
                int quality = v.getQuality();
                if (quality == Voice.QUALITY_VERY_HIGH) score += 500;
                else if (quality == Voice.QUALITY_HIGH) score += 300;
                else if (quality == Voice.QUALITY_NORMAL) score += 100;

                // Low latency
                if (v.getLatency() == Voice.LATENCY_VERY_LOW || v.getLatency() == Voice.LATENCY_LOW) {
                    score += 50;
                }

                String name = v.getName().toLowerCase(Locale.ROOT);
                // Prefer modern neural/natural voice models over legacy robotic synthesizers
                if (name.contains("natural") || name.contains("neural")) score += 200;
                if (name.contains("local") || name.contains("embedded")) score += 100;
                if (!name.contains("espeak") && !name.contains("pico")) score += 50;

                if (score > highestScore) {
                    highestScore = score;
                    bestVoice = v;
                }
            }

            if (bestVoice != null) {
                tts.setVoice(bestVoice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void speak(String rawText) {
        if (!isInitialized || isMuted || rawText == null || rawText.trim().isEmpty()) {
            if (callback != null) callback.onSpeechCompleted();
            return;
        }

        stop();

        // Sanitize text for natural speech pronunciation:
        // Strip out brackets, bullet symbols, asterisks, hashtags, emojis
        String speechClean = rawText
                .replaceAll("(?i)\\[[^\\]]*\\]", "")   // remove tags like [STATUS]
                .replaceAll("[*#_`~•/]", "")
                .replaceAll("[\\p{So}\\p{Cn}]", "")    // remove Unicode symbols/emojis
                .replaceAll("\\s+", " ")
                .trim();

        if (speechClean.isEmpty()) {
            if (callback != null) callback.onSpeechCompleted();
            return;
        }

        String utteranceId = "SPEECH_" + System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(speechClean, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(speechClean, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public void stop() {
        if (tts != null && isInitialized) {
            try {
                tts.stop();
            } catch (Exception ignored) {}
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
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
        }
    }
}
