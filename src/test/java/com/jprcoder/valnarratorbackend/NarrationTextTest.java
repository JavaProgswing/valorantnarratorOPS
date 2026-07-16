package com.jprcoder.valnarratorbackend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NarrationTextTest {

    @Test
    void capLeavesShortTextUnchanged() {
        assertEquals("rotate a", NarrationText.cap("  rotate a  ", 180));
        assertNull(NarrationText.cap(null, 180));
    }

    @Test
    void capTruncatesLongTextAtWordBoundary() {
        String text = "one two three four five six seven eight";
        String capped = NarrationText.cap(text, 15);
        assertTrue(capped.length() <= 15, "must not exceed the limit");
        assertFalse(capped.endsWith(" "));
        assertTrue(text.startsWith(capped), "prefix preserved");
        assertFalse(capped.contains("  "));
        // cuts on a whole word rather than mid-word
        assertEquals("one two three", capped);
    }

    @Test
    void capHardCutsWhenNoNearbySpace() {
        String capped = NarrationText.cap("abcdefghijklmnopqrstuvwxyz", 10);
        assertEquals(10, capped.length());
    }

    @Test
    void keyNormalisesCaseAndWhitespace() {
        assertEquals("gg well played", NarrationText.key("  GG   Well  Played "));
        assertEquals(NarrationText.key("Rotate A"), NarrationText.key("rotate  a"));
    }

    @Test
    void duplicateFilterSuppressesRepeatWithinWindow() {
        NarrationText.RecentDuplicateFilter filter = new NarrationText.RecentDuplicateFilter(2500, 32);
        assertTrue(filter.allow("test", 1000));
        assertFalse(filter.allow("test", 1500), "repeat inside the window is suppressed");
        assertTrue(filter.allow("test", 4000), "same line later than the window narrates again");
    }

    @Test
    void duplicateFilterAllowsDistinctLines() {
        NarrationText.RecentDuplicateFilter filter = new NarrationText.RecentDuplicateFilter(2500, 32);
        assertTrue(filter.allow("hello", 1000));
        assertTrue(filter.allow("world", 1000));
    }

    @Test
    void duplicateFilterEvictsBeyondCapWithoutFalseSuppression() {
        NarrationText.RecentDuplicateFilter filter = new NarrationText.RecentDuplicateFilter(60000, 2);
        assertTrue(filter.allow("a", 0));
        assertTrue(filter.allow("b", 0));
        assertTrue(filter.allow("c", 0)); // evicts "a"
        assertTrue(filter.allow("a", 1), "evicted key is treated as new, not a duplicate");
    }
}
