package com.jprcoder.valnarratorgui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jprcoder.valnarratorencryption.Encryption;
import com.jprcoder.valnarratorencryption.Signature;
import com.jprcoder.valnarratorencryption.SignatureValidator;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorbackend.SerialGenerator.getSerialNumber;

record VersionInfo(long timestamp, double version, String changes) {
}

record RegistrationInfo(boolean registered, String signature, String salt) {
}

public class Main {
    public static final String serialNumber;
    public static final String CONFIG_DIR = Paths.get(System.getenv("APPDATA"), "ValorantNarrator").toString();
    public static final String LOCK_FILE = Paths.get(CONFIG_DIR, "lockFile").toString();
    private static final String installerName = "ValNarrator-setup.exe";
    private static final String versionInfoUrl = "https://api.valnarrator.tech/version/latest/info";
    private static final String installerDownloadUrl = "https://api.valnarrator.tech/installer/version/latest";
    private static final String registrationCheckUrl = "https://api.valnarrator.tech/register";
    public static double currentVersion;
    private static Logger logger;
    private static Properties properties;
    private static char[] secretKey, secretSalt;

    static {
        try {
            serialNumber = getSerialNumber();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void encryptSignup(String signature, String salt) {
        try {
            Encryption.encrypt(signature, Paths.get(CONFIG_DIR, "secretSign.bin").toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not initialize app properly, try again with administrator!", "Not Registered", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            Encryption.encrypt(salt, Paths.get(CONFIG_DIR, "secretSalt.bin").toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not initialize app properly, try again with administrator!", "Not Registered", JOptionPane.WARNING_MESSAGE);
            System.exit(-1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean lockInstance(final String lockFile) {
        try {
            File file = new File(lockFile);
            file.createNewFile();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileLock lock = randomAccessFile.getChannel().tryLock();
            if (lock == null) {
                randomAccessFile.close();
                JOptionPane.showMessageDialog(null, "Another instance of this application is already running!", "App", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    randomAccessFile.close();
                    Files.delete(Paths.get(lockFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        lockInstance(LOCK_FILE);
        try (InputStream inputStream = Objects.requireNonNull(Main.class.getResource("config.properties")).openStream()) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fullVersioning = properties.getProperty("version");
        currentVersion = Double.parseDouble(fullVersioning.substring(0, fullVersioning.lastIndexOf('.')));
        Files.createDirectories(Paths.get(CONFIG_DIR));
        RegistrationInfo ri = fetchRegistrationInfo();
        if (ri.registered()) {
            encryptSignup(ri.signature(), ri.salt());
            secretKey = ri.signature().toCharArray();
            secretSalt = ri.salt().toCharArray();
        } else {
            try {
                secretKey = Encryption.decrypt(Paths.get(CONFIG_DIR, "secretSign.bin").toString()).toCharArray();
                secretSalt = Encryption.decrypt(Paths.get(CONFIG_DIR, "secretSalt.bin").toString()).toCharArray();
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(null, "You are not registered, please contact support!", "Not Registered", JOptionPane.WARNING_MESSAGE);
                System.exit(-1);
            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                     NoSuchPaddingException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }
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
        logger = LoggerFactory.getLogger(Main.class);
        logger.info(String.format("Starting Valorant-Narrator on v-%1$,.2f", currentVersion));

        if (Arrays.asList(args).contains("win-launch") || Arrays.asList(args).contains("win-startup")) {
            VersionInfo vi;
            try {
                vi = fetchVersionInfo();
            } catch (com.google.gson.JsonSyntaxException e) {
                CompletableFuture.runAsync(() -> JOptionPane.showMessageDialog(null, "Our service is unavailable, please try again later!", "API Down", JOptionPane.WARNING_MESSAGE));
                return;
            }
            if (vi.version() > currentVersion) {
                ValNarratorApplication.showInformation("New Update v" + vi.version(), "Changes:" + vi.changes() + "\nClick Ok to continue.");

                final String installerLocation = Paths.get(System.getenv("Temp"), "ValorantNarrator").toString();
                URL url;
                URLConnection con;
                DataInputStream dis;
                FileOutputStream fos;
                byte[] fileData;
                try {
                    url = new URL(installerDownloadUrl);
                    con = url.openConnection();
                    dis = new DataInputStream(con.getInputStream());
                    fileData = new byte[con.getContentLength()];
                    for (int q = 0; q < fileData.length; q++) {
                        fileData[q] = dis.readByte();
                    }
                    dis.close();
                    Files.createDirectories(Paths.get(installerLocation));
                    fos = new FileOutputStream(Paths.get(installerLocation, installerName).toString());
                    fos.write(fileData);
                    fos.close();
                } catch (Exception m) {
                    m.printStackTrace();
                }
                Runtime.getRuntime().exec(String.format("cmd.exe /K \"cd %s && %s /silent\"", installerLocation, installerName));
                return;
            }
            while (!isValorantRunning()) {
                vi = fetchVersionInfo();
                if (vi.version() > currentVersion) {
                    VersionInfo finalVi = vi;
                    CompletableFuture.runAsync(() -> JOptionPane.showMessageDialog(null, "Changes:" + finalVi.changes() + "\nClick Ok to continue.", "NEW Update v" + finalVi.version(), JOptionPane.INFORMATION_MESSAGE));
                    Toolkit.getDefaultToolkit().beep();
                    final String installerLocation = Paths.get(System.getenv("Temp"), "ValorantNarrator").toString();
                    URL url;
                    URLConnection con;
                    DataInputStream dis;
                    FileOutputStream fos;
                    byte[] fileData;
                    try {
                        url = new URL(installerDownloadUrl);
                        con = url.openConnection();
                        dis = new DataInputStream(con.getInputStream());
                        fileData = new byte[con.getContentLength()];
                        for (int q = 0; q < fileData.length; q++) {
                            fileData[q] = dis.readByte();
                        }
                        dis.close();
                        Files.createDirectories(Paths.get(installerLocation));
                        fos = new FileOutputStream(Paths.get(installerLocation, installerName).toString());
                        fos.write(fileData);
                        fos.close();
                    } catch (Exception m) {
                        m.printStackTrace();
                    }
                    Runtime.getRuntime().exec(String.format("cmd.exe /K \"cd %s && %s /silent\"", installerLocation, installerName));
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("Valorant started!");
            Application.launch(ValNarratorApplication.class, args);
            return;
        }
        Application.launch(ValNarratorApplication.class, args);
    }

    private static boolean isValorantRunning() {
        String findProcess = "VALORANT.exe";
        String filenameFilter = "/nh /fi \"Imagename eq " + findProcess + "\"";
        String tasksCmd = System.getenv("windir") + "/system32/tasklist.exe " + filenameFilter;
        Process p;
        ArrayList<String> procs;
        try {
            p = Runtime.getRuntime().exec(tasksCmd);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            procs = new ArrayList<>();
            String line;
            while ((line = input.readLine()) != null) procs.add(line);

            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return procs.stream().anyMatch(row -> row.contains(findProcess));
    }

    private static VersionInfo fetchVersionInfo() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(versionInfoUrl)).build();
        String resp;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            resp = response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();
        return gson.fromJson(resp, VersionInfo.class);
    }

    private static RegistrationInfo fetchRegistrationInfo() {
        HttpClient client = HttpClient.newHttpClient();
        Signature sign = SignatureValidator.generateRegistrationSignature(serialNumber);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(registrationCheckUrl + "?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 409) {
                return new RegistrationInfo(false, null, null);
            }
            return new RegistrationInfo(true, response.headers().firstValue("Signature").get(), response.headers().firstValue("Salt").get());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
