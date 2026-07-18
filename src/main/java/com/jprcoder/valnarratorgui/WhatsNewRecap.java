package com.jprcoder.valnarratorgui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jprcoder.valnarratorbackend.APIHandler;
import com.jprcoder.valnarratorbackend.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Shows a one-time "What's New" recap the first time the app launches on a newer version than it
 * last recapped. The last-recapped version is persisted in {@code CONFIG_DIR/whatsnew.json}, so the
 * dialog appears exactly once per update and never again until the next version bump.
 * <p>
 * On a brand-new install (no state file) the current version is recorded silently - a first-time
 * user gets no changelog, only genuine upgrades trigger the recap.
 */
public final class WhatsNewRecap {
    private static final Logger logger = LoggerFactory.getLogger(WhatsNewRecap.class);
    private static final String STATE_FILE = "whatsnew.json";
    private static final String KEY = "lastRecapVersion";

    private WhatsNewRecap() {
    }

    /**
     * If {@code currentVersion} is newer than the last version we recapped, show the release notes
     * and persist the new version. Safe to call off the FX thread; best-effort and never throws.
     */
    public static void maybeShow(double currentVersion) {
        try {
            File stateFile = new File(Main.CONFIG_DIR, STATE_FILE);

            if (!stateFile.exists()) {
                // First run on this machine - remember the version but don't show a changelog.
                writeVersion(stateFile, currentVersion);
                return;
            }

            double lastRecap = readVersion(stateFile);
            if (currentVersion <= lastRecap) {
                return; // already recapped (or downgrade) - nothing to show.
            }

            String changes = fetchChanges(currentVersion);
            String header = String.format("What's New in v-%1$,.2f", currentVersion);
            ValNarratorApplication.showDialog(header, changes, MessageType.INFORMATION_MESSAGE);

            // Persist only after showing, so a crash before the dialog doesn't swallow the recap.
            writeVersion(stateFile, currentVersion);
        } catch (Exception e) {
            logger.warn("Failed to show What's New recap: {}", e.getMessage());
        }
    }

    /**
     * Pulls the changelog for the current release from the backend. Falls back to a generic message
     * if the backend is unreachable or the notes are empty, so the recap still appears offline.
     */
    private static String fetchChanges(double currentVersion) {
        try {
            VersionInfo info = APIHandler.fetchVersionInfo();
            if (info != null && info.changes() != null && !info.changes().isBlank()) {
                return info.changes().trim();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.debug("Could not fetch release notes for recap: {}", e.getMessage());
        }
        return String.format("You've been updated to v-%1$,.2f. Enjoy the latest improvements and fixes!",
                currentVersion);
    }

    private static double readVersion(File stateFile) {
        try (FileReader reader = new FileReader(stateFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has(KEY) && json.get(KEY).isJsonPrimitive()) {
                return json.get(KEY).getAsDouble();
            }
        } catch (Exception e) {
            logger.debug("Could not read {}, treating as unseen: {}", STATE_FILE, e.getMessage());
        }
        return 0.0;
    }

    private static void writeVersion(File stateFile, double version) {
        try {
            Files.createDirectories(Paths.get(Main.CONFIG_DIR));
            JsonObject json = new JsonObject();
            json.addProperty(KEY, version);
            try (FileWriter writer = new FileWriter(stateFile, StandardCharsets.UTF_8)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            logger.warn("Could not persist {}: {}", STATE_FILE, e.getMessage());
        }
    }
}
