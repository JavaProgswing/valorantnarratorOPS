package com.jprcoder.valnarratorgui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.gson.JsonSyntaxException;
import com.jprcoder.valnarratorbackend.OutdatedVersioningException;
import com.jprcoder.valnarratorbackend.RegistrationInfo;
import com.jprcoder.valnarratorbackend.VersionInfo;
import com.jprcoder.valnarratorencryption.Encryption;
import javafx.application.Application;
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
            ValNarratorApplication.showPreStartupDialog("Not Registered", "Could not initialize app properly, try again with administrator!", MessageType.fromInt(JOptionPane.ERROR_MESSAGE));
            System.exit(-1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            Encryption.encrypt(salt, Paths.get(CONFIG_DIR, "secretSalt.bin").toString());
        } catch (IOException e) {
            ValNarratorApplication.showPreStartupDialog("Not Registered", "Could not initialize app properly, try again with administrator!", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
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
                ValNarratorApplication.showPreStartupDialog("App", "Another instance of this application is already running!", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
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
        boolean insMessage = Arrays.asList(args).contains("-showInstallationPrompt");
        if (insMessage && args.length == 1) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.exit(0);
            });
            ValNarratorApplication.showPreStartupDialog("Installation", String.format("ValorantNarrator v-%1$,.2f has been downloaded, restart the system for finishing the installation!", currentVersion), MessageType.fromInt(JOptionPane.INFORMATION_MESSAGE));
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
                            ValNarratorApplication.showPreStartupDialog("New Update v" + vi.version(), "Changes:" + vi.changes() + "\nClick Ok to continue.", MessageType.fromInt(JOptionPane.INFORMATION_MESSAGE));
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
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
            ValNarratorApplication.showPreStartupDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
            throw new RuntimeException(e);
        }
        logger.info(String.format("New registration: %b, Serial id: %s", ri.registered(), serialNumber));
        if (ri.registered()) {
            encryptSignup(ri.signature(), ri.salt());
            secretKey = ri.signature().toCharArray();
            secretSalt = ri.salt().toCharArray();
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
