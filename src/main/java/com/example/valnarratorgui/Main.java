package com.example.valnarratorgui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.example.valnarratorbackend.CustomFormatter;
import com.example.valnarratorencryption.Encryption;
import com.example.valnarratorencryption.Signature;
import com.example.valnarratorencryption.SignatureValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

record VersionInfo(long timestamp, double version, String changes) {
}

record RegistrationInfo(boolean registered, String signature, String salt) {
}

public class Main {

    public static final double currentVersion = 2.5;
    public static final String serialNumber;
    public static final String CONFIG_DIR = System.getenv("APPDATA") + "\\ValorantNarrator";
    private static Properties properties;
    private static final String installerName = "ValNarrator-setup.exe";
    private static final String versionInfoUrl = "https://api.valnarrator.tech/version/latest/info";
    private static final String installerDownloadUrl = "https://api.valnarrator.tech/installer/version/latest";
    private static final String registrationCheckUrl = "https://api.valnarrator.tech/register";

    private static char[] secretKey, secretSalt;

    static {
        try {
            serialNumber = getSerialNumber();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void encrypt(String signature, String salt) {
        try {
            Encryption.encrypt(signature, CONFIG_DIR+"\\secretSign.bin");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            Encryption.encrypt(salt, CONFIG_DIR+"\\secretSalt.bin");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        try (InputStream inputStream = Objects.requireNonNull(Main.class.getResource("config.properties")).openStream()) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Files.createDirectories(Paths.get(CONFIG_DIR));
        RegistrationInfo ri = fetchRegistrationInfo();
        if (ri.registered()) {
            encrypt(ri.signature(), ri.salt());
            secretKey = ri.signature().toCharArray();
            secretSalt = ri.salt().toCharArray();
        } else {
            try {
                secretKey = Encryption.decrypt(CONFIG_DIR+"\\secretSign.bin").toCharArray();
                secretSalt = Encryption.decrypt(CONFIG_DIR+"\\secretSalt.bin").toCharArray();
            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                     NoSuchPaddingException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                CompletableFuture.runAsync(() -> JOptionPane.showMessageDialog(null, "You are not registered, please contact support!", "Not Registered", JOptionPane.WARNING_MESSAGE));
                return;
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
        CustomFormatter logger = new CustomFormatter(Main.class);

        if (Arrays.asList(args).contains("win-launch") || Arrays.asList(args).contains("win-startup")) {
            VersionInfo vi;
            try {
                vi = fetchVersionInfo();
            } catch (com.google.gson.JsonSyntaxException e) {
                CompletableFuture.runAsync(() -> JOptionPane.showMessageDialog(null, "Our service is unavailable, please try again later!", "API Down", JOptionPane.WARNING_MESSAGE));
                return;
            }
            if (vi.version() > currentVersion) {
                VersionInfo finalVi = vi;
                CompletableFuture.runAsync(() -> JOptionPane.showMessageDialog(null, "Changes:" + finalVi.changes() + "\nClick Ok to continue.", "NEW Update v" + finalVi.version(), JOptionPane.INFORMATION_MESSAGE));
                Toolkit.getDefaultToolkit().beep();
                final String installerLocation = System.getenv("Temp") + "\\ValorantNarrator";
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
                    fos = new FileOutputStream(installerLocation + "\\" + installerName);
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
                    final String installerLocation = System.getenv("Temp") + "\\ValorantNarrator";
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
                        fos = new FileOutputStream(installerLocation + "\\" + installerName);
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


    private static ArrayList<String> getMBoardSerial() throws IOException {
        ArrayList<String> MBoardSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "baseboard", "get", "serialnumber"});
        } catch (IOException ignored) {
            return MBoardSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                MBoardSerial.add(serial);
            }
        }
        return MBoardSerial;
    }


    private static ArrayList<String> getDiskSerial() throws IOException {
        ArrayList<String> diskSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "diskdrive", "get", "serialnumber"});
        } catch (IOException ignored) {
            return diskSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                diskSerial.add(serial);
            }
        }
        return diskSerial;
    }


    private static ArrayList<String> getGPUSerial() throws IOException {
        ArrayList<String> gpuSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "PATH", "Win32_VideoController", "GET", "PNPDeviceID"});
        } catch (IOException ignored) {
            return gpuSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                gpuSerial.add(serial.substring(serial.indexOf('\\') + 1));
            }
        }
        return gpuSerial;
    }


    private static String getCPUSerial() throws IOException {
        String cpuSerial = null;
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
        } catch (IOException ignored) {
            return null;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            while (sc.hasNext()) {
                String next = sc.next();
                if ("ProcessorId".equals(next)) {
                    cpuSerial = sc.next().trim();
                    break;
                }
            }
        }
        return cpuSerial;
    }

    public static String getSerialNumber() throws IOException {
        String cpuSerial = getCPUSerial();
        ArrayList<String> gpuSerial = getGPUSerial();
        ArrayList<String> hddSerial = getDiskSerial();
        ArrayList<String> mBoardSerial = getMBoardSerial();
        return String.valueOf(Objects.hash(cpuSerial, gpuSerial, hddSerial, mBoardSerial));
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
