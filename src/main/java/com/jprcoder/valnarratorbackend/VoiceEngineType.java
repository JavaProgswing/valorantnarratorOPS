package com.jprcoder.valnarratorbackend;

public enum VoiceEngineType {
    STANDARD("standard"), NEURAL("neural");

    public final String value;

    VoiceEngineType(String value) {
        this.value = value;
    }

    public String toValue() {
        return value;
    }

}
