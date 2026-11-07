package com.abztech.assistant.model;

public class ChatMessage {
    private final String text;
    private final boolean isUser;
    private final long timestamp;
    private final String actionTag;

    public ChatMessage(String text, boolean isUser) {
        this(text, isUser, null);
    }

    public ChatMessage(String text, boolean isUser, String actionTag) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.actionTag = actionTag;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getActionTag() {
        return actionTag;
    }
}
