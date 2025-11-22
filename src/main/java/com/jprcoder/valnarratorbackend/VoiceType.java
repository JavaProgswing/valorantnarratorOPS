package com.jprcoder.valnarratorbackend;

public enum VoiceType {
    AGENT, STANDARD, INBUILT;

    public static VoiceType fromString(String voiceType) {
        if (voiceType.endsWith("VALORANT")) {
            return AGENT;
        } else if (voiceType.endsWith("INBUILT")) {
            return INBUILT;
        } else {
            return STANDARD;
        }
    }
}
