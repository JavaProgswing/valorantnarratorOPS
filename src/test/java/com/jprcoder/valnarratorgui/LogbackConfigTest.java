package com.jprcoder.valnarratorgui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogbackConfigTest {

    /**
     * The default (no {@code -debug}) config must write an always-on log file, so that with the
     * javaw launcher - which has no console - users can still retrieve logs. Drives the exact
     * SLF4J-bound {@link LoggerContext} + {@link JoranConfigurator} path {@code Main.main} uses.
     * {@code APPDATA} is redirected via a system property (logback resolves that ahead of the
     * environment variable) so the real user log is untouched.
     */
    @Test
    void defaultConfigWritesAlwaysOnLogFile(@TempDir Path tmp) throws Exception {
        System.setProperty("APPDATA", tmp.toString());
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            ctx.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(ctx);
            configurator.doConfigure(Objects.requireNonNull(
                    getClass().getClassLoader().getResource("logback-console.xml")));

            LoggerFactory.getLogger("logback-config-test").info("marker-line-abc123");

            // Flush the rolling file appender so its buffer reaches disk before we read it.
            Appender<?> file = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("FILE");
            if (file != null) file.stop();
        } finally {
            System.clearProperty("APPDATA");
        }

        Path logFile = tmp.resolve("valorantNarrator/valnarrator.log");
        assertTrue(Files.exists(logFile), "always-on log file must be created");
        assertTrue(Files.readString(logFile).contains("marker-line-abc123"),
                "log line must be written to the file");
    }
}
