package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentVoiceSynthesizerTest {

    @Test
    void buildSpeakPayloadEscapesQuotesAndBackslashes() {
        String text = "he said \"gg\" \\ done\nline2";
        String payload = AgentVoiceSynthesizer.buildSpeakPayload("Jett", text);

        // The raw payload must not contain an unescaped inner quote that would break the JSON.
        assertTrue(payload.contains("\\\""), "inner quotes must be escaped");

        // And it must round-trip back to the exact original values.
        JsonObject parsed = JsonParser.parseString(payload).getAsJsonObject();
        assertEquals("Jett", parsed.get("agent").getAsString());
        assertEquals(text, parsed.get("text").getAsString());
    }

    @Test
    void buildSpeakPayloadProducesValidJsonForPlainText() {
        JsonObject parsed = JsonParser.parseString(
                AgentVoiceSynthesizer.buildSpeakPayload("Sage", "rotate to a")).getAsJsonObject();
        assertEquals("Sage", parsed.get("agent").getAsString());
        assertEquals("rotate to a", parsed.get("text").getAsString());
    }
}
