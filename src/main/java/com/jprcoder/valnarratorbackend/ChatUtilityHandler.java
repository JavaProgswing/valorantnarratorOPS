package com.jprcoder.valnarratorbackend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat text utilities: expanding short-forms/abbreviations into spoken
 * full-forms
 * before narration, and resolving player display names.
 * <p>
 * The short-form table is user-configurable and persisted in
 * {@code config.json}
 * (see {@code VoiceGenerator}). Insertion order is significant - longer keys
 * (e.g.
 * {@code GGWP}) must precede their prefixes ({@code GG}) so the more specific
 * expansion wins.
 */
public class ChatUtilityHandler {

    /**
     * Built-in defaults, used as a seed for first run and as the "reset" baseline.
     */
    private static final Map<String, String> DEFAULT_FULL_FORMS = buildDefaultFullForms();
    private static final Pattern HP_NUMERIC = Pattern.compile("(\\d+)hp", Pattern.CASE_INSENSITIVE);
    /**
     * Live table; guarded by the class monitor. Defaults until config is loaded.
     */
    private static Map<String, String> fullForms = new LinkedHashMap<>(DEFAULT_FULL_FORMS);

    private ChatUtilityHandler() {
    }

    private static Map<String, String> buildDefaultFullForms() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("GGWP", "Good game,,,well played!");
        m.put("GG", "Good game!");
        m.put("WP", "Well played!");
        m.put("MB", "My bad!");
        m.put("EZ", "Easy!");
        m.put("NT", "Nice Try!");
        m.put("BTW", "By the way");
        m.put("BRB", "Be right back");
        m.put("NS", "Nice Shot!");
        m.put("FR", "For real");
        m.put("IC", "I see");
        m.put("NC", "Nice");
        m.put("IKR", "I know right");
        m.put("LOL", "Laughing out loud");
        m.put("ASF", "As fuck");
        m.put("IG", "I guess");
        m.put("TPED", "Teleported");
        m.put("GH", "Good Half");
        m.put("HP", "Health");
        m.put("NVM", "Nevermind");
        m.put("DM", "Deathmatch");
        m.put("UNR", "Unrated");
        m.put("COMP", "Competitive");
        m.put("GL", "Good luck");
        m.put("GJ", "Good Job");
        m.put("NJ", "Nice Job");
        m.put("GLHF", "Good luck, have fun");
        m.put("WDYM", "What do you mean?");
        m.put("NP", "No problem");
        m.put("SMH", "Shake my head");
        m.put("TY", "Thank you!");
        m.put("SRY", "Sorry!");
        m.put("PLS", "Please");
        return m;
    }

    /**
     * An immutable copy of the built-in defaults.
     */
    public static Map<String, String> getDefaultFullForms() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(DEFAULT_FULL_FORMS));
    }

    /**
     * A snapshot copy of the currently active short-form table.
     */
    public static synchronized Map<String, String> getFullForms() {
        return new LinkedHashMap<>(fullForms);
    }

    /**
     * Replace the active short-form table (e.g. after the user edits it).
     */
    public static synchronized void setFullForms(Map<String, String> newForms) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (newForms != null) {
            newForms.forEach((k, v) -> {
                if (k != null && !k.isBlank() && v != null)
                    copy.put(k.trim(), v);
            });
        }
        fullForms = copy;
    }

    /**
     * Expands every configured short-form found in {@code message} (whole-word,
     * case-insensitive) and converts inline health values such as {@code 100hp}
     * into {@code 100 health}.
     */
    public static synchronized String expandShortForms(String message) {
        if (message == null || message.isEmpty())
            return message;
        for (Map.Entry<String, String> entry : fullForms.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank())
                continue;
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(key) + "\\b");
            message = pattern.matcher(message).replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        message = HP_NUMERIC.matcher(message).replaceAll("$1 health");
        return message;
    }

}
