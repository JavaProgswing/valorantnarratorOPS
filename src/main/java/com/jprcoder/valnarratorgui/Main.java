package com.jprcoder.valnarratorgui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.gson.JsonSyntaxException;
import com.jprcoder.valnarratorbackend.*;
import com.jprcoder.valnarratorencryption.Encryption;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorbackend.APIHandler.*;
import static com.jprcoder.valnarratorbackend.RiotUtilityHandler.isValorantRunning;
import static com.jprcoder.valnarratorbackend.SerialGenerator.getSerialNumber;

public class Main {
    public static final String serialNumber;
    public static final String CONFIG_DIR = Paths.get(System.getenv("APPDATA"), "ValorantNarrator").toString();
    public static final String LOCK_FILE_NAME = "lockFile";
    public static final String LOCK_FILE = Paths.get(CONFIG_DIR, LOCK_FILE_NAME).toString();
    public static final String installerName = "ValNarrator-setup.exe";
    public static String API_URL;
    public static double currentVersion;
    private static Properties properties;
    private static char[] secretKey, secretSalt;

    static {
        try {
            serialNumber = getSerialNumber();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reEncryptSignup() throws IOException, InterruptedException {
        RegistrationInfo ri;
        try {
            ri = fetchRegistrationInfo(1, 1);
        } catch (OutdatedVersioningException e) {
            throw new IOException(e);
        }
        encryptSignup(ri.signature(), ri.salt());
        secretKey = ri.signature().toCharArray();
        secretSalt = ri.salt().toCharArray();
    }

    private static void encryptSignup(String signature, String salt) {
        try {
            Encryption.encrypt(signature, Paths.get(CONFIG_DIR, "secretSign.bin").toString());
        } catch (IOException e) {
            ValNarratorApplication.showDialog("Not Registered", "Could not initialize app properly, try again with administrator!", MessageType.fromInt(JOptionPane.ERROR_MESSAGE));
            System.exit(-1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            Encryption.encrypt(salt, Paths.get(CONFIG_DIR, "secretSalt.bin").toString());
        } catch (IOException e) {
            ValNarratorApplication.showDialog("Not Registered", "Could not initialize app properly, try again with administrator!", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
            System.exit(-1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean lockInstance() {
        try {
            Files.createDirectories(Path.of(CONFIG_DIR));
            File file = new File(Main.LOCK_FILE);
            file.createNewFile();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileLock lock = randomAccessFile.getChannel().tryLock();
            if (lock == null) {
                randomAccessFile.close();
                ValNarratorApplication.showDialog("App", "Another instance of this application is already running!", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
                System.exit(0);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    randomAccessFile.close();
                    Files.delete(Paths.get(Main.LOCK_FILE));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean debugEnabled = Arrays.asList(args).contains("-debug");
        String logbackConfigFile = "logback-console.xml";
        if (debugEnabled) {
            logbackConfigFile = "logback-debug.xml";
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(Objects.requireNonNull(Main.class.getClassLoader().getResource(logbackConfigFile)));
        } catch (JoranException e) {
            e.printStackTrace();
            StatusPrinter.print(loggerContext);
        }
        Logger logger = LoggerFactory.getLogger(Main.class);
        try (InputStream inputStream = Objects.requireNonNull(Main.class.getResource("config.properties")).openStream()) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("CRITICAL: Could not load app's config properties, exiting!");
            throw new RuntimeException(e);
        }
        String fullVersioning = properties.getProperty("version");
        currentVersion = Double.parseDouble(fullVersioning);
        API_URL = properties.getProperty("apiBaseUrl");
        boolean insMessage = Arrays.asList(args).contains("-showInstallationPrompt");
        if (insMessage && args.length == 1) {

            if (AgentVoiceSynthesizer.isNewerVersionAvailable()) {

                ValNarratorApplication.showNonBlockingDialog("Installation", "ValorantNarrator is updating agent voices...", MessageType.INFORMATION_MESSAGE).thenAccept(dialog -> CompletableFuture.runAsync(() -> {
                    try {
                        AgentVoiceSynthesizer.checkForUpdates((percent, done, total) -> Platform.runLater(() -> {
                            if (percent >= 0) {
                                if (done == total) dialog.setContentText("Finalizing update, please wait...");
                                else
                                    dialog.setContentText(String.format("Updating agent voices...\n%.2f%% (%.2f / %.2f MB)", percent, done / 10_00_000.0, total / 10_00_000.0));

                            } else dialog.setContentText("Updating agent voices...\n" + done + " bytes downloaded");
                        }));

                    } catch (IOException | InterruptedException e) {
                        logger.error("Download failed", e);
                        Platform.runLater(() -> {
                            dialog.setAlertType(Alert.AlertType.ERROR);
                            dialog.setContentText("Failed to download update!");
                        });
                        return;
                    }

                    Platform.runLater(() -> dialog.setContentText("Download complete! Restarting in 5 seconds..."));

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                    }

                    System.exit(0);
                }));
            } else {
                ValNarratorApplication.showDialog("Installation", String.format("ValorantNarrator v-%1$,.2f has been downloaded, restart the system to finish installation!", currentVersion), MessageType.fromInt(JOptionPane.INFORMATION_MESSAGE));
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
                System.exit(0);
            }
            return;
        }
        logger.info(String.format("Starting Valorant-Narrator on v-%1$,.2f", currentVersion));
        logger.info(String.format("Build date: %s", properties.getProperty("buildTimestamp")));

        Files.createDirectories(Paths.get(CONFIG_DIR));
        if (Arrays.asList(args).contains("win-launch") || Arrays.asList(args).contains("win-startup")) {
            logger.info("Updater: Checking for updates!");
            VersionInfo vi;
            while (true) {
                if (!isValorantRunning()) {
                    try {
                        vi = fetchVersionInfo();
                        if (vi.version() > currentVersion) {
                            logger.info(String.format("New Update v%f found, updating!", vi.version()));
                            ValNarratorApplication.showDialog("New Update v" + vi.version(), "Changes:" + vi.changes() + "\nClick Ok to continue.", MessageType.fromInt(JOptionPane.INFORMATION_MESSAGE));
                            long start = System.currentTimeMillis();
                            Toolkit.getDefaultToolkit().beep();
                            downloadLatestVersion();

                            logger.info(String.format("Updater finished in %d ms, exiting.", (System.currentTimeMillis() - start)));
                            return;
                        }
                    } catch (JsonSyntaxException | InterruptedException e) {
                        logger.error("CRITICAL: Could not find latest versioning info, trying again in a second!");
                    }
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Thread.sleep(30 * 1000);
                }
            }
        }
        lockInstance();
        logger.info("Detected normal start-up, launching application!");
        RegistrationInfo ri;
        try {
            ri = fetchRegistrationInfo(1, 3);
        } catch (com.google.gson.JsonSyntaxException | IOException e) {
            logger.error("CRITICAL: API is down!");
            return;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (OutdatedVersioningException e) {
            ValNarratorApplication.showDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
            throw new RuntimeException(e);
        }
        logger.info(String.format("New registration: %b, Serial id: %s", ri.registered(), serialNumber));
        if (ri.registered()) {
            encryptSignup(ri.signature(), ri.salt());
            secretKey = ri.signature().toCharArray();
            secretSalt = ri.salt().toCharArray();

            if (ValNarratorApplication.showConfirmationAlertAndWait("Support Your Friend!", "If someone referred you to ValNarrator, enter their User ID.\n" + "They’ll receive rewards, and you will unlock a bonus.")) {
                String referrerId = ValNarratorApplication.showInputDialogAndWait("Referral", "Enter the referrer's User ID:");

                if (referrerId == null || referrerId.trim().isEmpty()) {
                    logger.debug("Referral skipped: empty input.");
                    return;
                }

                referrerId = referrerId.trim();

                while (!verifyReferralUser(referrerId)) {
                    referrerId = ValNarratorApplication.showInputDialogAndWait("Invalid Referral", "Re-enter the referrer's User ID:");

                    if (referrerId == null || referrerId.trim().isEmpty()) {
                        logger.debug("Referral cancelled during correction.");
                        return;
                    }

                    referrerId = referrerId.trim();
                }
                logger.info("Referral user validated: " + referrerId);
                try {
                    ReferralResponse referralResponse = submitReferral(referrerId);
                    logger.info("Referral response: " + referralResponse);

                    if ("ok".equals(referralResponse.status())) {
                        long durationSeconds = referralResponse.duration();

                        long days = durationSeconds / 86400;
                        if (days > 0) {
                            ValNarratorApplication.showDialog("Referral Success", String.format("Congrats! You've been granted premium for %d days.", days), MessageType.INFORMATION_MESSAGE);
                            return;
                        }
                        long hours = durationSeconds / 3600;
                        long minutes = (durationSeconds % 3600) / 60;

                        ValNarratorApplication.showDialog("Referral Success", String.format("Congrats! You've been granted premium for %02d:%02d hours.", hours, minutes), MessageType.INFORMATION_MESSAGE);
                    } else {
                        ValNarratorApplication.showAlertAndWait("Referral Failed", "Referral couldn't be applied. Please try again later.");
                    }
                } catch (IllegalStateException e) {
                    logger.error("Error during referral submission", e);
                    ValNarratorApplication.showDialog("Referral Error", "An error occurred while processing your referral. Please try again later.", MessageType.ERROR_MESSAGE);
                }
            }


        } else {
            try {
                secretKey = Encryption.decrypt(Paths.get(CONFIG_DIR, "secretSign.bin").toString()).toCharArray();
                secretSalt = Encryption.decrypt(Paths.get(CONFIG_DIR, "secretSalt.bin").toString()).toCharArray();
            } catch (FileNotFoundException e) {
                logger.warn("CRITICAL: Could not find pre-existing files required for normal usage of app: secretSign.bin, secretSalt.bin");
                encryptSignup(ri.signature(), ri.salt());
                secretKey = ri.signature().toCharArray();
                secretSalt = ri.salt().toCharArray();
            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                     NoSuchPaddingException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            ReferralNotificationsResponse notif = fetchReferralNotifications();

            if (notif.notifications != null && !notif.notifications.isEmpty()) {

                StringBuilder builder = new StringBuilder();
                builder.append("🎉 You’ve received new referral bonuses!\n\n");

                for (ReferralNotification rn : notif.notifications) {
                    long hours = rn.bonus_duration / 3600;
                    long minutes = (rn.bonus_duration % 3600) / 60;

                    builder.append(String.format("• %s used your referral → +%02d:%02d premium\n", rn.referred_user_id, hours, minutes));
                }

                builder.append("\nThank you for sharing ValNarrator! 🚀");

                ValNarratorApplication.showDialog("Referral Rewards", builder.toString(), MessageType.INFORMATION_MESSAGE);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch referral notifications!", e);
        }

        Application.launch(ValNarratorApplication.class, args);
    }

    public static char[] getSecretKey() {
        return secretKey;
    }

    public static char[] getSecretSalt() {
        return secretSalt;
    }

    public static Properties getProperties() {
        return properties;
    }
}
