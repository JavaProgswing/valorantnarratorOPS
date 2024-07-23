package com.jprcoder.valnarratorgui;

public enum MessageType {
    ERROR_MESSAGE(0),
    INFORMATION_MESSAGE(1),
    WARNING_MESSAGE(2),
    QUESTION_MESSAGE(3),
    PLAIN_MESSAGE(-1);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public static MessageType fromInt(int value) {
        for (MessageType type : MessageType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("No MessageType with value " + value);
    }

    public int getValue() {
        return value;
    }
}
