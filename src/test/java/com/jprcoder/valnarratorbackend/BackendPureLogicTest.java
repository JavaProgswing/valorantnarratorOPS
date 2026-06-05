package com.jprcoder.valnarratorbackend;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackendPureLogicTest {

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
}

