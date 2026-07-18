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
            // CSV output: the default table format truncates the Image Name column to 25 chars,
            // which clips "VALORANT-Win64-Shipping.exe" (27) so a full-name match would wrongly fail.
            Process p = new ProcessBuilder(tasklist, "/nh", "/fo", "csv", "/fi", "Imagename eq " + VALORANT_PROCESS)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader input = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (isValorantTasklistRow(line)) return true;
                }
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to query the task list", e);
            return false;
        }
    }

    static boolean isValorantTasklistRow(String line) {
        return VALORANT_PROCESS.equalsIgnoreCase(firstCsvField(line));
    }

    static String firstCsvField(String line) {
        if (line == null || line.isEmpty()) return "";
        if (line.charAt(0) != '"') {
            int comma = line.indexOf(',');
            return comma < 0 ? line : line.substring(0, comma);
        }

        StringBuilder field = new StringBuilder();
        for (int i = 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    return field.toString();
                }
            } else {
                field.append(c);
            }
        }
        return field.toString();
    }

    /**
     * Launches Valorant through the Riot Client if it is not already running.
     * The Riot Client path is resolved from {@code RiotClientInstalls.json}, falling
     * back to the default install location.
     */
    /**
     * Resolves the RiotClientServices.exe path: {@code RiotClientInstalls.json}'s {@code rc_default}
     * is authoritative, but that file is maintained by Riot's own launcher and has been observed
     * transiently zero-filled (caught mid-write) - one retry after a short delay clears that race.
     * If the json is still unusable, falls back to probing common install locations across drive
     * letters (many users install off the {@code C:} default) before giving up.
     */
    private static String resolveRiotClientPath() {
        File installsJson = new File(System.getenv("ProgramData") + "\\Riot Games\\RiotClientInstalls.json");
        String fromJson = readRcDefault(installsJson);
        if (fromJson == null) {
            // Zero-byte/mid-write races self-heal within milliseconds - one short retry is enough.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            fromJson = readRcDefault(installsJson);
        }
        if (fromJson != null && new File(fromJson).isFile()) {
            return fromJson;
        }
        if (fromJson != null) {
            logger.warn("rc_default path '{}' from RiotClientInstalls.json does not exist; trying other sources.", fromJson);
        }

        // The Start Menu shortcut Riot's installer creates points at the exe wherever it actually
        // was installed - including non-default locations like "D:\Games\Riot Games\..." that a
        // drive-root guess below would never find. Far more reliable than guessing, so try it first.
        String fromShortcut = resolveViaStartMenuShortcut();
        if (fromShortcut != null) return fromShortcut;

        for (String drive : new String[]{"C:", "D:", "E:", "F:"}) {
            String candidate = drive + "\\Riot Games\\Riot Client\\RiotClientServices.exe";
            if (new File(candidate).isFile()) return candidate;
        }
        for (String envVar : new String[]{"ProgramFiles", "ProgramFiles(x86)"}) {
            String base = System.getenv(envVar);
            if (base == null) continue;
            String candidate = base + "\\Riot Games\\Riot Client\\RiotClientServices.exe";
            if (new File(candidate).isFile()) return candidate;
        }
        return fromJson != null ? fromJson : (System.getenv("SystemDrive") + "\\Riot Games\\Riot Client\\RiotClientServices.exe");
    }

    /**
     * Resolves RiotClientServices.exe by reading the target of the .lnk shortcut Riot's installer
     * places under the "Riot Games" Start Menu folder (checks both the all-users and current-user
     * Start Menu, since the installer can target either depending on install mode). Handles any
     * custom install path with no guessing involved.
     */
    private static String resolveViaStartMenuShortcut() {
        for (String rootEnv : new String[]{"ProgramData", "AppData"}) {
            String root = System.getenv(rootEnv);
            if (root == null) continue;
            File riotGamesFolder = new File(root, "Microsoft\\Windows\\Start Menu\\Programs\\Riot Games");
            File[] shortcuts = riotGamesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".lnk"));
            if (shortcuts == null) continue;

            for (File shortcut : shortcuts) {
                String target = resolveShortcutTarget(shortcut);
                if (target != null && target.toLowerCase().endsWith("riotclientservices.exe") && new File(target).isFile()) {
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * Resolves a Windows .lnk shortcut's target path via WScript.Shell (no Java library parses the
     * binary shortcut format, and shelling out to the same COM object Explorer itself uses is the
     * standard, robust approach). Returns null on any failure - a resolution attempt, not a
     * requirement.
     */
    private static String resolveShortcutTarget(File shortcut) {
        try {
            String escapedPath = shortcut.getAbsolutePath().replace("'", "''");
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(New-Object -ComObject WScript.Shell).CreateShortcut('" + escapedPath + "').TargetPath")
                    .redirectErrorStream(true).start();
            String target;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                target = reader.readLine();
            }
            p.waitFor();
            return (target == null || target.isBlank()) ? null : target.trim();
        } catch (Exception e) {
            logger.debug("Could not resolve shortcut target for {}: {}", shortcut, e.getMessage());
            return null;
        }
    }

    /**
     * Best-effort read of {@code rc_default} from RiotClientInstalls.json. Returns null on any
     * failure (missing file, corrupt/partial content, missing key) rather than throwing, so the
     * caller can retry or fall back to path probing.
     */
    private static String readRcDefault(File installsJson) {
        if (!installsJson.exists()) return null;
        try {
            String content = Files.readString(installsJson.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            return json.has("rc_default") ? json.get("rc_default").getAsString() : null;
        } catch (Exception e) {
            logger.debug("Could not read RiotClientInstalls.json: {}", e.getMessage());
            return null;
        }
    }

    public static void launchValorant() {
        if (isValorantRunning()) {
            logger.info("Valorant is already running; skipping launch.");
            return;
        }

        String riotClientPath = resolveRiotClientPath();
        if (riotClientPath == null || !new File(riotClientPath).isFile()) {
            logger.error("RiotClientServices.exe not found (tried '{}' and common install locations) - cannot "
                    + "auto-launch Valorant. Launch Valorant manually; the app will connect once it is running.",
                    riotClientPath);
            return;
        }

        try {
            logger.info("Launching Valorant via {}", riotClientPath);
            new ProcessBuilder(riotClientPath, "--launch-product=valorant", "--launch-patchline=live").start();
            logger.info("Valorant launch command issued.");
            Thread.sleep(2000); // give the client a moment to spin up
        } catch (IOException e) {
            logger.error("Failed to launch Valorant via Riot Client", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final Pattern LAST_CONFIRMED_MODE = Pattern.compile("(?m)^LastConfirmedFullscreenMode=\\d+");
    private static final Pattern PREFERRED_MODE = Pattern.compile("(?m)^PreferredFullscreenMode=\\d+");
    private static final Pattern FULLSCREEN_MODE = Pattern.compile("(?m)^FullscreenMode=\\d+");
    private static final String SHOOTER_SETTINGS_SECTION = "[/Script/ShooterGame.ShooterGameUserSettings]";

    private static Path valorantConfigRoot() {
        return Paths.get(System.getenv("LOCALAPPDATA"), "VALORANT", "Saved", "Config");
    }

    /**
     * Forces Valorant to Windowed Fullscreen (Borderless), preferring the active
     * player's {@code GameUserSettings.ini}. Falls back to scanning every saved Valorant
     * config only when the active account cannot be identified or its local file is not
     * present. MUST be called while Valorant is closed - Valorant rewrites the file on
     * exit, so a change made while it is running is lost.
     *
     * @return the number of config files updated
     */
    public static int setBorderlessMode(String puuid, String deployment) {
        return setBorderlessMode(valorantConfigRoot(), puuid, deployment);
    }

    /**
     * Legacy fallback: patches every saved local Valorant account config.
     */
    public static int setBorderlessMode() {
        return setBorderlessMode(valorantConfigRoot(), null, null);
    }

    static int setBorderlessMode(Path configRoot, String puuid, String deployment) {
        if (!Files.isDirectory(configRoot)) {
            logger.warn("Valorant config directory not found: {}", configRoot);
            return 0;
        }

        Path activePlayerIni = gameUserSettingsPath(configRoot, puuid, deployment);
        if (activePlayerIni != null) {
            if (Files.isRegularFile(activePlayerIni)) {
                boolean patched = patchBorderless(activePlayerIni);
                logger.info("Set Valorant to borderless in active player config: {}", activePlayerIni);
                return patched ? 1 : 0;
            }
            logger.warn("Active player Valorant config not found at {}; falling back to all configs.", activePlayerIni);
        }

        return setBorderlessModeInAllConfigs(configRoot);
    }

    static Path gameUserSettingsPath(Path configRoot, String puuid, String deployment) {
        if (isBlank(puuid) || isBlank(deployment)) return null;
        return configRoot.resolve(puuid + "-" + deployment).resolve("Windows").resolve("GameUserSettings.ini");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int setBorderlessModeInAllConfigs(Path configRoot) {
        List<Path> inis;
        try (var paths = Files.walk(configRoot)) {
            inis = paths.filter(f -> f.getFileName().toString().equals("GameUserSettings.ini")).toList();
        } catch (IOException e) {
            logger.error("Failed to scan Valorant config", e);
            return 0;
        }
        int patched = 0;
        for (Path ini : inis) {
            if (patchBorderless(ini)) patched++;
        }
        logger.info("Set Valorant to borderless in {} config file(s).", patched);
        return patched;
    }

    // Retries for the read-modify-write below: the ini can be transiently locked right after
    // Valorant exits (its own cleanup/telemetry subprocess, or an AV scan on the just-closed file),
    // which otherwise makes the borderless fix silently no-op for that session.
    private static final int PATCH_RETRIES = 4;
    private static final long PATCH_RETRY_DELAY_MS = 750;

    /**
     * Reads one ini, applies the borderless transform, and writes it back only if it changed.
     * Retries on IOException (transient file lock) before giving up.
     */
    private static boolean patchBorderless(Path ini) {
        IOException last = null;
        for (int attempt = 1; attempt <= PATCH_RETRIES; attempt++) {
            try {
                String content = Files.readString(ini);
                String patched = applyBorderless(content);
                if (patched.equals(content)) return false;
                Files.writeString(ini, patched);
                logger.debug("Patched Valorant config to borderless: {}", ini);
                return true;
            } catch (IOException e) {
                last = e;
                if (attempt < PATCH_RETRIES) {
                    try {
                        Thread.sleep(PATCH_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        logger.warn("Could not patch '{}' after {} attempt(s)", ini, PATCH_RETRIES, last);
        return false;
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
