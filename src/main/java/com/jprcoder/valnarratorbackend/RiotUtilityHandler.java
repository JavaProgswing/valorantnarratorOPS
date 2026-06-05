package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RiotUtilityHandler {
    private static final Logger logger = LoggerFactory.getLogger(RiotUtilityHandler.class);
    private static final String VALORANT_PROCESS = "VALORANT-Win64-Shipping.exe";

    private RiotUtilityHandler() {
    }

    public static boolean isValorantRunning() {
        String tasklist = System.getenv("windir") + "\\system32\\tasklist.exe";
        try {
            Process p = new ProcessBuilder(tasklist, "/nh", "/fi", "Imagename eq " + VALORANT_PROCESS)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader input = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.contains(VALORANT_PROCESS)) return true;
                }
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to query the task list: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Launches Valorant through the Riot Client if it is not already running.
     * The Riot Client path is resolved from {@code RiotClientInstalls.json}, falling
     * back to the default install location.
     */
    public static void launchValorant() {
        if (isValorantRunning()) {
            logger.info("Valorant is already running; skipping launch.");
            return;
        }

        String riotClientPath = System.getenv("SystemDrive") + "\\Riot Games\\Riot Client\\RiotClientServices.exe";
        File installsJson = new File(System.getenv("ProgramData") + "\\Riot Games\\RiotClientInstalls.json");
        try {
            if (installsJson.exists()) {
                String content = Files.readString(installsJson.toPath());
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                if (json.has("rc_default")) {
                    riotClientPath = json.get("rc_default").getAsString();
                }
            } else {
                logger.warn("{} not found; using default Riot Client path.", installsJson);
            }
        } catch (Exception e) {
            logger.warn("Could not read RiotClientInstalls.json, using default path: {}", e.getMessage());
        }

        if (!new File(riotClientPath).isFile()) {
            logger.error("RiotClientServices.exe not found at '{}' - cannot auto-launch Valorant. "
                    + "Launch Valorant manually; the app will connect once it is running.", riotClientPath);
            return;
        }

        try {
            logger.info("Launching Valorant via {}", riotClientPath);
            new ProcessBuilder(riotClientPath, "--launch-product=valorant", "--launch-patchline=live").start();
            logger.info("Valorant launch command issued.");
            Thread.sleep(2000); // give the client a moment to spin up
        } catch (IOException e) {
            logger.error("Failed to launch Valorant via Riot Client: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final Pattern LAST_CONFIRMED_MODE = Pattern.compile("(?m)^LastConfirmedFullscreenMode=\\d+");
    private static final Pattern PREFERRED_MODE = Pattern.compile("(?m)^PreferredFullscreenMode=\\d+");
    private static final Pattern FULLSCREEN_MODE = Pattern.compile("(?m)^FullscreenMode=\\d+");
    private static final String SHOOTER_SETTINGS_SECTION = "[/Script/ShooterGame.ShooterGameUserSettings]";

    /**
     * Forces Valorant to Windowed Fullscreen (Borderless) by patching every account's
     * {@code GameUserSettings.ini} (EWindowMode 1 = borderless). MUST be called while
     * Valorant is closed - Valorant rewrites the file on exit, so a change made while it is
     * running is lost.
     *
     * @return the number of config files updated
     */
    public static int setBorderlessMode() {
        Path configRoot = Paths.get(System.getenv("LOCALAPPDATA"), "VALORANT", "Saved", "Config");
        if (!Files.isDirectory(configRoot)) {
            logger.warn("Valorant config directory not found: {}", configRoot);
            return 0;
        }
        List<Path> inis;
        try (var paths = Files.walk(configRoot)) {
            inis = paths.filter(f -> f.getFileName().toString().equals("GameUserSettings.ini")).toList();
        } catch (IOException e) {
            logger.error("Failed to scan Valorant config: {}", e.getMessage());
            return 0;
        }
        int patched = 0;
        for (Path ini : inis) {
            if (patchBorderless(ini)) patched++;
        }
        logger.info("Set Valorant to borderless in {} config file(s).", patched);
        return patched;
    }

    /** Reads one ini, applies the borderless transform, and writes it back only if it changed. */
    private static boolean patchBorderless(Path ini) {
        try {
            String content = Files.readString(ini);
            String patched = applyBorderless(content);
            if (patched.equals(content)) return false;
            Files.writeString(ini, patched);
            logger.debug("Patched Valorant config to borderless: {}", ini);
            return true;
        } catch (IOException e) {
            logger.warn("Could not patch '{}': {}", ini, e.getMessage());
            return false;
        }
    }

    /**
     * Pure transform: sets the fullscreen-mode keys to 1 (borderless) in a GameUserSettings.ini,
     * inserting {@code FullscreenMode} under the ShooterGame section if it is absent. Returns the
     * input unchanged when it has none of the mode keys. The inserted line uses the file's own
     * newline style, so a mixed-ending file is never produced. Package-private for unit testing.
     */
    static String applyBorderless(String content) {
        boolean hasModeKeys = FULLSCREEN_MODE.matcher(content).find()
                || PREFERRED_MODE.matcher(content).find()
                || LAST_CONFIRMED_MODE.matcher(content).find();
        if (!hasModeKeys) return content; // not a display config

        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        String out = LAST_CONFIRMED_MODE.matcher(content).replaceAll("LastConfirmedFullscreenMode=1");
        out = PREFERRED_MODE.matcher(out).replaceAll("PreferredFullscreenMode=1");
        if (FULLSCREEN_MODE.matcher(out).find()) {
            out = FULLSCREEN_MODE.matcher(out).replaceAll("FullscreenMode=1");
        } else if (out.contains(SHOOTER_SETTINGS_SECTION)) {
            out = out.replaceFirst(Pattern.quote(SHOOTER_SETTINGS_SECTION),
                    Matcher.quoteReplacement(SHOOTER_SETTINGS_SECTION + newline + "FullscreenMode=1"));
        }
        return out;
    }
}
