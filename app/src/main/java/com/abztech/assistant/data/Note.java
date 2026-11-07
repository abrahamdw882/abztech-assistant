package com.abztech.assistant.data;

public class Note {
    private final long id;
    private final String content;
    private final long timestamp;

    public Note(long id, String content, long timestamp) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
