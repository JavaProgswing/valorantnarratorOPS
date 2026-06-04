package com.jprcoder.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Small helper around {@link ProcessBuilder} that replaces the deprecated
 * {@code Runtime.exec(String)} calls used throughout the app.
 * <p>
 * Each argument is passed as a separate list element, so values containing
 * spaces (device names such as {@code "CABLE Input"}, paths, ...) are quoted
 * correctly by the platform instead of being naively split on whitespace.
 */
public final class ProcessUtil {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtil.class);

    private ProcessUtil() {
    }

    /**
     * Fire-and-forget: start a process and return immediately, logging failures.
     */
    public static void runDetached(String... command) {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            logger.warn("Failed to start '{}': {}", command.length > 0 ? command[0] : "", e.getMessage());
        }
    }

    /**
     * Start a process and return it for callers that need to read its output.
     */
    public static Process start(String... command) throws IOException {
        return new ProcessBuilder(command).start();
    }
}
