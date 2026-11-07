package com.abztech.assistant.engine;

import java.util.LinkedList;

/**
 * Tracks short-term conversational context and memory across voice interactions.
 */
public class ContextMemory {

    private Double lastMathResult = null;
    private String lastSpokenSubject = null;
    private IntentType lastIntent = null;
    private final LinkedList<String> queryHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 10;

    public void recordInteraction(String query, IntentType intent, Double mathResult, String subject) {
        if (query != null) {
            queryHistory.addFirst(query);
            if (queryHistory.size() > MAX_HISTORY) {
                queryHistory.removeLast();
            }
        }
        this.lastIntent = intent;
        if (mathResult != null) {
            this.lastMathResult = mathResult;
        }
        if (subject != null && !subject.isEmpty()) {
            this.lastSpokenSubject = subject;
        }
    }

    public Double getLastMathResult() {
        return lastMathResult;
    }

    public void setLastMathResult(Double lastMathResult) {
        this.lastMathResult = lastMathResult;
    }

    public String getLastSpokenSubject() {
        return lastSpokenSubject;
    }

    public IntentType getLastIntent() {
        return lastIntent;
    }

    public void clear() {
        lastMathResult = null;
        lastSpokenSubject = null;
        lastIntent = null;
        queryHistory.clear();
    }
}
