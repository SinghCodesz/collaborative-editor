package com.editor.editorapp.model;

public class Operation {

    public enum OpType {
        INSERT,
        DELETE
    }

    private OpType type;
    private int position;
    private String character;  // The character to insert ("" for delete)
    private String userId;
    private long timestamp;
    private int sequenceNumber;

    // Default constructor (needed for JSON deserialization)
    public Operation() {}

    // Full constructor
    public Operation(OpType type, int position, String character,
                     String userId, long timestamp, int sequenceNumber) {
        this.type = type;
        this.position = position;
        this.character = character;
        this.userId = userId;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    // Getters and Setters
    public OpType getType() { return type; }
    public void setType(OpType type) { this.type = type; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    @Override
    public String toString() {
        return "Operation{" +
                "type=" + type +
                ", position=" + position +
                ", character='" + character + '\'' +
                ", userId='" + userId + '\'' +
                ", seq=" + sequenceNumber +
                '}';
    }
}