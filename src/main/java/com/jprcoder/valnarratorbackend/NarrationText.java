package com.jprcoder.valnarratorbackend;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pure helpers for the narration path: bounding how much text a single message may speak, and
 * suppressing repeats. Kept free of app/UI dependencies so the logic is unit-testable.
 * <p>
 * These guard the push-to-talk pipeline: a very long message would hold the team-voice key open
 * for many seconds and stall every queued narration behind it, and OCR re-reads / own-name jitter
 * can emit the same line twice in quick succession.
 */
public final class NarrationText {

    /**
     * Upper bound on narrated characters. Valorant caps chat at ~144 chars; short-form expansion
     * can grow that, so this leaves headroom while still capping a pathological (pasted / mangled)
     * line so it cannot monopolise the microphone.
     */
    public static final int MAX_NARRATION_CHARS = 180;

    private NarrationText() {
    }

    /**
     * Truncates over-long narration text at a word boundary (falls back to a hard cut when there
     * is no space near the limit). Returns the stripped input unchanged when already within
     * {@code maxChars}. Null in, null out.
     */
    public static String cap(String text, int maxChars) {
        if (text == null) return null;
        String t = text.strip();
        if (t.length() <= maxChars) return t;
        int cut = t.lastIndexOf(' ', maxChars);
        if (cut < maxChars / 2) cut = maxChars; // no nearby space -> hard cut rather than lose most of it
        return t.substring(0, cut).strip();
    }

    /**
     * Normalized key for near-duplicate suppression: lower-cased with whitespace collapsed, so
     * spacing/case jitter between OCR re-reads maps to the same key.
     */
    public static String key(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    /**
     * Time-windowed duplicate filter. {@link #allow(String, long)} returns {@code false} when the
     * same key first narrated within {@code windowMs} of the supplied timestamp - so a line the OCR
     * emits twice in quick succession (or the same line re-read under name jitter) narrates once,
     * while a genuine repeat sent after the window passes narrates again. The window is measured
     * from a key's first occurrence (not refreshed on each hit), so a line held on screen still
     * re-narrates at most once per window rather than being suppressed forever. Bounded to
     * {@code cap} recent keys.
     * <p>
     * Access-ordered so the most recently seen keys survive eviction; {@code allow} is
     * synchronized for use from the narration-producing thread.
     */
    public static final class RecentDuplicateFilter {
        private final long windowMs;
        private final int cap;
        private final LinkedHashMap<String, Long> seen;

        public RecentDuplicateFilter(long windowMs, int cap) {
            this.windowMs = windowMs;
            this.cap = Math.max(1, cap);
            this.seen = new LinkedHashMap<>(16, 0.75f, true);
        }

        public synchronized boolean allow(String key, long nowMs) {
            seen.values().removeIf(t -> nowMs - t > windowMs);
            if (seen.containsKey(key)) {
                return false; // within the window of its first occurrence -> suppress
            }
            seen.put(key, nowMs);
            if (seen.size() > cap) {
                Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
                it.next(); // eldest (least-recently accessed) first under access order
                it.remove();
            }
            return true;
        }
    }
}
