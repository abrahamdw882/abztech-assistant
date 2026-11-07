package com.abztech.assistant.engine;

import java.util.HashMap;
import java.util.Map;

public class IntentResult {
    private final IntentType type;
    private final float confidence;
    private final String rawQuery;
    private final Map<String, String> slots;

    public IntentResult(IntentType type, float confidence, String rawQuery) {
        this.type = type;
        this.confidence = confidence;
        this.rawQuery = rawQuery;
        this.slots = new HashMap<>();
    }

    public IntentType getType() {
        return type;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public IntentResult putSlot(String key, String value) {
        this.slots.put(key, value);
        return this;
    }

    public String getSlot(String key) {
        return slots.get(key);
    }

    public String getSlotOrDefault(String key, String defaultValue) {
        String val = slots.get(key);
        return val != null ? val : defaultValue;
    }

    public boolean hasSlot(String key) {
        return slots.containsKey(key);
    }

    public Map<String, String> getSlots() {
        return slots;
    }

    @Override
    public String toString() {
        return "IntentResult{" +
                "type=" + type +
                ", confidence=" + confidence +
                ", slots=" + slots +
                ", rawQuery='" + rawQuery + '\'' +
                '}';
    }
}
