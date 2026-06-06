package com.jprcoder.valnarratorbackend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackendPureLogicTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetFullForms() {
        ChatUtilityHandler.setFullForms(ChatUtilityHandler.getDefaultFullForms());
    }

    @Test
    void sourceFromStringAcceptsLowercaseAndUnderscores() {
        Set<Source> result = Source.fromString("self_party_team");

        assertEquals(EnumSet.of(Source.SELF, Source.PARTY, Source.TEAM), result);
    }

    @Test
    void sourceFromStringRejectsBlankAndInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> Source.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Source.fromString("   "));
        assertThrows(IllegalArgumentException.class, () -> Source.fromString("SELF+SPECTATOR"));
    }

    @Test
    void sourceToStringHonoursCanonicalOrdering() {
        String result = Source.toString(EnumSet.of(Source.ALL, Source.SELF, Source.TEAM, Source.PARTY));

        assertEquals("SELF+PARTY+TEAM+ALL", result);
    }

    @Test
    void voiceTypeFromStringSelectsTheExpectedCategory() {
        assertEquals(VoiceType.AGENT, VoiceType.fromString("Sage_VALORANT"));
        assertEquals(VoiceType.INBUILT, VoiceType.fromString("Matthew_INBUILT"));
        assertEquals(VoiceType.STANDARD, VoiceType.fromString("Matthew"));
    }

    @Test
    void messageNormalisesNullInputsAndFormatsToString() {
        Message message = new Message(MessageType.PARTY, null, null, true);

        assertEquals(MessageType.PARTY, message.getMessageType());
        assertEquals("", message.getUserId());
        assertEquals("", message.getContent());
        assertEquals("", message.getId());
        assertTrue(message.isOwnMessage());
        assertEquals("(PARTY): ", message.toString());
    }

    @Test
    void messagePreservesSenderAndContent() {
        Message message = new Message(MessageType.TEAM, "PlayerOne", "Nice shot", false);

        assertEquals("PlayerOne", message.getUserId());
        assertEquals("PlayerOne", message.getId());
        assertEquals("Nice shot", message.getContent());
        assertFalse(message.isOwnMessage());
        assertEquals("(TEAM)PlayerOne: Nice shot", message.toString());
    }

    @Test
    void chatStatsCountMessagesCharactersWordsAndAverageLength() {
        Chat chat = new Chat(10);

        chat.updateMessageStats(new Message(MessageType.TEAM, "A", "two words", false));
        chat.updateMessageStats(new Message(MessageType.TEAM, "B", "  spaced   words  here  ", false));

        assertEquals(2, chat.getMessagesSent());
        assertEquals(33, chat.getCharactersSent());
        assertEquals(5, chat.getWordsSent());
        assertEquals(16, chat.getAverageMessageLength());
    }

    @Test
    void ignoredPlayerLookupUsesStoredPlayerId() {
        Chat chat = new Chat(10);
        chat.getPlayerNameTable().put("Teammate", "player-id");

        chat.addIgnoredPlayer("Teammate");

        assertTrue(chat.isIgnoredPlayerID("player-id"));
        assertFalse(chat.isIgnoredPlayerID("Teammate"));
        chat.removeIgnoredPlayer("Teammate");
        assertFalse(chat.isIgnoredPlayerID("player-id"));
    }

    @Test
    void defaultFullFormsAreImmutableAndContainCommonAbbreviations() {
        Map<String, String> defaults = ChatUtilityHandler.getDefaultFullForms();
        Map<String, String> copy = new LinkedHashMap<>(defaults);
        copy.put("NEW", "Value");

        assertTrue(defaults.containsKey("GGWP"));
        assertTrue(defaults.containsKey("GLHF"));
        assertTrue(copy.containsKey("NEW"));
        assertFalse(defaults.containsKey("NEW"));
    }

    @Test
    void setFullFormsFiltersNullAndBlankKeysAndReturnsSnapshotCopies() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put(" gg ", "Good game");
        input.put("", "ignored");
        input.put(null, "ignored");
        input.put("wp", null);

        ChatUtilityHandler.setFullForms(input);

        Map<String, String> snapshot = ChatUtilityHandler.getFullForms();
        assertEquals(Map.of("gg", "Good game"), snapshot);

        snapshot.put("extra", "value");
        assertEquals(Map.of("gg", "Good game"), ChatUtilityHandler.getFullForms());
    }

    @Test
    void expandShortFormsExpandsWholeWordsCaseInsensitivelyAndHandlesHealthValues() {
        ChatUtilityHandler.setFullForms(Map.of(
                "GGWP", "Good game,,,well played!",
                "GG", "Good game!",
                "WP", "Well played!",
                "BRB", "Be right back"));

        String result = ChatUtilityHandler.expandShortForms("ggwp GG wp 100hp BRB ggez");

        assertEquals("Good game,,,well played! Good game! Well played! 100 health Be right back ggez", result);
    }

    @Test
    void expandShortFormsLeavesNullEmptyAndNonWholeWordsUntouched() {
        ChatUtilityHandler.setFullForms(Map.of("GG", "Good game!"));

        assertNull(ChatUtilityHandler.expandShortForms(null));
        assertEquals("", ChatUtilityHandler.expandShortForms(""));
        assertEquals("maggie ggz", ChatUtilityHandler.expandShortForms("maggie ggz"));
    }

    @Test
    void zlibRoundTripPreservesUtf8SettingsJson() throws Exception {
        String input = "{\"message\":\"hello नमस्ते こんにちは\",\"enabled\":true}";

        String encoded = ZlibCompression.deflateAndBase64Encode(input);

        assertEquals(input, ZlibCompression.decodeBase64AndInflate(encoded));
    }

    @Test
    void zlibInflateRejectsIncompleteStreamsWithoutSpinning() {
        assertThrows(java.util.zip.DataFormatException.class,
                () -> ZlibCompression.inflate(new byte[]{1, 2, 3}));
    }

    @Test
    void applyBorderlessRewritesExistingFullscreenKeys() {
        String input = """
                [/Script/ShooterGame.ShooterGameUserSettings]
                LastConfirmedFullscreenMode=0
                PreferredFullscreenMode=2
                FullscreenMode=3
                """;

        String output = RiotUtilityHandler.applyBorderless(input);

        assertEquals("""
                [/Script/ShooterGame.ShooterGameUserSettings]
                LastConfirmedFullscreenMode=1
                PreferredFullscreenMode=1
                FullscreenMode=1
                """, output);
    }

    @Test
    void applyBorderlessInsertsFullscreenModeWhenMissingAndKeepsWindowsNewlines() {
        String input = "[/Script/ShooterGame.ShooterGameUserSettings]\r\nLastConfirmedFullscreenMode=2\r\nPreferredFullscreenMode=2\r\n";

        String output = RiotUtilityHandler.applyBorderless(input);

        assertTrue(output.contains("\r\nFullscreenMode=1\r\n") || output.contains("\r\nFullscreenMode=1"));
        assertEquals("[/Script/ShooterGame.ShooterGameUserSettings]\r\nFullscreenMode=1\r\nLastConfirmedFullscreenMode=1\r\nPreferredFullscreenMode=1\r\n", output);
    }

    @Test
    void applyBorderlessLeavesUnrelatedContentUntouched() {
        String input = "[SomeOtherSection]\nResolution=1920x1080\n";

        assertEquals(input, RiotUtilityHandler.applyBorderless(input));
    }

    @Test
    void gameUserSettingsPathTargetsActivePlayerFolder() {
        Path expected = tempDir.resolve("current-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");

        assertEquals(expected, RiotUtilityHandler.gameUserSettingsPath(tempDir, "current-puuid", "ap"));
        assertNull(RiotUtilityHandler.gameUserSettingsPath(tempDir, "", "ap"));
        assertNull(RiotUtilityHandler.gameUserSettingsPath(tempDir, "current-puuid", null));
    }

    @Test
    void setBorderlessModeTargetsActivePlayerBeforeFallbackScan() throws IOException {
        String fullscreen = """
                [/Script/ShooterGame.ShooterGameUserSettings]
                FullscreenMode=0
                LastConfirmedFullscreenMode=0
                PreferredFullscreenMode=0
                """;
        Path active = tempDir.resolve("current-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");
        Path other = tempDir.resolve("other-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");
        Files.createDirectories(active.getParent());
        Files.createDirectories(other.getParent());
        Files.writeString(active, fullscreen);
        Files.writeString(other, fullscreen);

        int patched = RiotUtilityHandler.setBorderlessMode(tempDir, "current-puuid", "ap");

        assertEquals(1, patched);
        assertTrue(Files.readString(active).contains("FullscreenMode=1"));
        assertTrue(Files.readString(other).contains("FullscreenMode=0"));
    }

    @Test
    void setBorderlessModeFallsBackToAllConfigsWhenActivePlayerFileIsMissing() throws IOException {
        Path first = tempDir.resolve("first-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");
        Path second = tempDir.resolve("second-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, "[/Script/ShooterGame.ShooterGameUserSettings]\nFullscreenMode=0\n");
        Files.writeString(second, "[/Script/ShooterGame.ShooterGameUserSettings]\nFullscreenMode=0\n");

        int patched = RiotUtilityHandler.setBorderlessMode(tempDir, "missing-puuid", "ap");

        assertEquals(2, patched);
        assertTrue(Files.readString(first).contains("FullscreenMode=1"));
        assertTrue(Files.readString(second).contains("FullscreenMode=1"));
    }

    @Test
    void setBorderlessModeReturnsZeroWhenActivePlayerConfigAlreadyBorderless() throws IOException {
        Path active = tempDir.resolve("current-puuid-ap").resolve("Windows").resolve("GameUserSettings.ini");
        Files.createDirectories(active.getParent());
        Files.writeString(active, "[/Script/ShooterGame.ShooterGameUserSettings]\nFullscreenMode=1\n");

        assertEquals(0, RiotUtilityHandler.setBorderlessMode(tempDir, "current-puuid", "ap"));
    }
}

