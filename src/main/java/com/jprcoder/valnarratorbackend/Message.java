package com.jprcoder.valnarratorbackend;


public class Message {
    private final String content;
    private final String id;
    private final String userId;
    private final boolean isOwnMessage;
    private final MessageType messageType;

    /**
     * Construct from a screen-OCR chat event (in-game TEAM / ALL / PARTY chat).
     * <p>
     * OCR has no PUUID - the on-screen chat only exposes the player's display
     * name - so the display name doubles as both the user id and conversation id.
     *
     * @param messageType  the channel the line was read from
     * @param name         the sender's display name as rendered in the chat box
     * @param body         the message text
     * @param isOwnMessage whether the sender is the local player
     */
    public Message(MessageType messageType, String name, String body, boolean isOwnMessage) {
        this.content = body == null ? "" : body;
        this.userId = name == null ? "" : name;
        this.id = this.userId;
        this.messageType = messageType;
        this.isOwnMessage = isOwnMessage;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public boolean isOwnMessage() {
        return isOwnMessage;
    }

    public String getUserId() {
        return userId;
    }

    public String toString() {
        return String.format("(%s)%s: %s", messageType, id, content);
    }

}

