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
}
