package com.jprcoder.valnarratorbackend;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum Source {
    SELF, PARTY, TEAM, ALL;

    private static final List<Source> ORDER = List.of(
            Source.SELF, Source.PARTY, Source.TEAM, Source.ALL
    );

    /**
     * Converts string like:
     * "SELF", "PARTY+TEAM", "SELF+PARTY+TEAM+ALL"
     */
    public static EnumSet<Source> fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Source string cannot be null/empty");
        }

        String[] parts = raw
                .toUpperCase()
                .replace("_", "+")
                .split("\\+");

        EnumSet<Source> result = EnumSet.noneOf(Source.class);

        for (String part : parts) {
            try {
                result.add(Source.valueOf(part.trim()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid source type: " + part);
            }
        }

        return result;
    }

    public static String toString(EnumSet<Source> set) {
        return ORDER.stream()
                .filter(set::contains)
                .map(Enum::name)
                .collect(Collectors.joining("+"));
    }
}
