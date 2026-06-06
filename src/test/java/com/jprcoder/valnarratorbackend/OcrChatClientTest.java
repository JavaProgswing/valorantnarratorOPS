package com.jprcoder.valnarratorbackend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OcrChatClientTest {

    private static Message parseChannel(String channel) {
        Message message = OcrChatClient.parseChatLine(
                "{\"type\":\"chat\",\"channel\":\"" + channel + "\",\"name\":\"Teammate\",\"body\":\"hello\"}",
                () -> "Self");
        assertNotNull(message);
        return message;
    }

    @Test
    void parseChatLineMapsTeamMessageAndDetectsOwnNameCaseInsensitively() {
        Message message = OcrChatClient.parseChatLine(
                "{\"type\":\"chat\",\"channel\":\"TEAM\",\"name\":\"PlayerOne\",\"body\":\"rotate a\"}",
                () -> "playerone");

        assertNotNull(message);
        assertEquals(MessageType.TEAM, message.getMessageType());
        assertEquals("PlayerOne", message.getUserId());
        assertEquals("rotate a", message.getContent());
        assertTrue(message.isOwnMessage());
    }

    @Test
    void parseChatLineMapsKnownChannels() {
        assertEquals(MessageType.ALL, parseChannel("ALL").getMessageType());
        assertEquals(MessageType.PARTY, parseChannel("PARTY").getMessageType());
        assertEquals(MessageType.WHISPER, parseChannel("WHISPER").getMessageType());
    }

    @Test
    void parseChatLineDefaultsUnknownChannelToTeam() {
        Message message = parseChannel("SOMETHING_NEW");

        assertEquals(MessageType.TEAM, message.getMessageType());
    }

    @Test
    void parseChatLineDirectionOverridesNameBasedOwnDetection() {
        Message outbound = OcrChatClient.parseChatLine(
                "{\"type\":\"chat\",\"channel\":\"WHISPER\",\"name\":\"Other\",\"body\":\"yo\",\"direction\":\"TO\"}",
                () -> "Self");
        Message inbound = OcrChatClient.parseChatLine(
                "{\"type\":\"chat\",\"channel\":\"WHISPER\",\"name\":\"Self\",\"body\":\"yo\",\"direction\":\"FROM\"}",
                () -> "Self");

        assertNotNull(outbound);
        assertNotNull(inbound);
        assertTrue(outbound.isOwnMessage());
        assertFalse(inbound.isOwnMessage());
    }

    @Test
    void parseChatLineIgnoresNonChatAndIncompleteEvents() {
        assertNull(OcrChatClient.parseChatLine("not json", () -> "Self"));
        assertNull(OcrChatClient.parseChatLine("{\"type\":\"display\",\"mode\":\"ok\"}", () -> "Self"));
        assertNull(OcrChatClient.parseChatLine("{\"type\":\"chat\",\"name\":\"Self\"}", () -> "Self"));
        assertNull(OcrChatClient.parseChatLine("{\"type\":\"chat\",\"body\":\"hello\"}", () -> "Self"));
    }

    @Test
    void parseDisplayModeDiagnosticExtractsOnlyDisplayModeEvents() {
        assertEquals(OcrChatClient.DISPLAY_FULLSCREEN, OcrChatClient.parseDisplayModeDiagnostic(
                "{\"type\":\"display\",\"mode\":\"fullscreen\"}"));
        assertEquals(OcrChatClient.DISPLAY_OK, OcrChatClient.parseDisplayModeDiagnostic(
                "  {\"type\":\"display\",\"mode\":\"ok\"}  "));
        assertNull(OcrChatClient.parseDisplayModeDiagnostic("{\"type\":\"chat\",\"mode\":\"ok\"}"));
        assertNull(OcrChatClient.parseDisplayModeDiagnostic("plain stderr"));
    }
}
