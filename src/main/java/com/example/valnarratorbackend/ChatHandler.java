package com.example.valnarratorbackend;

import com.example.valnarratorencryption.SignatureValidator;
import com.example.valnarratorgui.Main;
import com.example.valnarratorgui.ValNarratorController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record AccountJsonParser(UserInfo userInfo) {
}

record UserInfo(ValAccount acct) {

}

record ValAccount(String game_name, String tag_line, String locale) {
}

public class ChatHandler {
    public static final int quotaLimit;
    public static final CustomFormatter logger = new CustomFormatter(ChatHandler.class);
    private static final String valAPIUrl = "https://api.valnarrator.tech";
    private static final HttpClient client;
    public static long messagesSent;
    public static long charactersSent;
    public static boolean isPremium;
    private static boolean isDisabled = false;
    private static boolean selfState = true, privateState = false, partyState = false, teamState = false, allState = false;
    private static String uniqueName = null;
    private static String partyID = null;
    private static String selfID = null;

    static {
        try {
            client = getClient();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        initialize();

        try {
            quotaLimit = getQuotaLimit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Platform.runLater(() -> ValNarratorController.latest_instance.setUserIDLabel(Main.serialNumber));
        MessageQuota mq;
        try {
            mq = getRequestQuota();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        isPremium = mq.isPremium();
        if (mq.remainingQuota() <= 0) {
            Platform.runLater(() -> {
                ValNarratorController.latest_instance.quotaLabel.setText("Quota Exhausted!");
                ValNarratorController.latest_instance.quotaLabel.setTextFill(Color.RED);
                ValNarratorController.latest_instance.quotaBar.setProgress(0.0);
                ValNarratorController.latest_instance.setPremiumDateLabel("NO");
            });
        } else {
            if (isPremium) {
                Platform.runLater(() -> {
                    ValNarratorController.latest_instance.windowTitle.setVisible(false);
                    ValNarratorController.latest_instance.premiumWindowTitle.setVisible(true);
                    ValNarratorController.latest_instance.subscribeButton.setVisible(false);
                    ValNarratorController.latest_instance.setPremiumDateLabel(String.format("Valid till %s", mq.premiumTill()));
                });
            } else {
                Platform.runLater(() -> {
                    ValNarratorController.latest_instance.quotaLabel.setText(mq.remainingQuota() + "/" + quotaLimit);
                    ValNarratorController.latest_instance.quotaBar.setProgress((double) mq.remainingQuota() / quotaLimit);
                    ValNarratorController.latest_instance.setPremiumDateLabel("NO");
                });
            }
        }
    }

    public static boolean isSelfState() {
        return selfState;
    }

    public static boolean isPrivateState() {
        return privateState;
    }

    public static boolean isPartyState() {
        return partyState;
    }

    public static boolean isTeamState() {
        return teamState;
    }

    public static boolean isAllState() {
        return allState;
    }

    public static void setSelfEnabled() {
        selfState = true;
    }

    public static void setPartyEnabled() {
        partyState = true;
    }

    public static void setPrivateEnabled() {
        privateState = true;
    }

    public static void setTeamEnabled() {
        teamState = true;
    }

    public static void setAllEnabled() {
        allState = true;
    }

    public static void setSelfDisabled() {
        selfState = false;
    }

    public static void setPartyDisabled() {
        partyState = false;
    }

    public static void setPrivateDisabled() {
        privateState = false;
    }

    public static void setTeamDisabled() {
        teamState = false;
    }

    public static void setAllDisabled() {
        allState = false;
    }

    public static void initialize() {
        String workingDirectory = System.getenv("LocalAppData");

        File f = new File(workingDirectory + "\\Riot Games\\Riot Client\\Config\\lockfile");
        if (f.isFile() && f.canRead()) {
            StringBuilder sb = new StringBuilder();
            try {
                try (FileInputStream in = new FileInputStream(f)) {
                    int ch;
                    while ((ch = in.read()) != -1) {
                        sb.append((char) ch);
                    }
                }
            } catch (IOException ex) {
                logger.warn("Error reading lockfile, exiting application.");
                System.exit(0);
            }
            String[] data = sb.toString().split(":");//name:pid:port:password:protocol
            String accountUrl = "https://127.0.0.1:" + data[2] + "/rso-auth/v1/authorization/userinfo";
            String pass = "riot:" + data[3];

            String auth = "Basic " + new String(Base64.getEncoder().encode(pass.getBytes()));
            HttpRequest accRequest = HttpRequest.newBuilder().uri(URI.create(accountUrl)).setHeader("Authorization", auth).build();
            String resp;

            try {
                HttpResponse<String> response = client.send(accRequest, HttpResponse.BodyHandlers.ofString());
                resp = response.body();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            resp = resp.replace("\"{", "{").replace("}\"", "}").replace("\\\"", "\"");
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();

            try {
                Gson gson = builder.create();
                AccountJsonParser jp = gson.fromJson(resp, AccountJsonParser.class);
                ValAccount acc = jp.userInfo().acct();
                logger.info("Welcome " + acc.game_name() + "#" + acc.tag_line());
                uniqueName = acc.game_name() + "#" + acc.tag_line();
                Platform.runLater(() -> ValNarratorController.latest_instance.setAccountLabel(uniqueName));
            } catch (Exception ignored) {
            }
        }
    }

    public static void message(Message message) {
        if (ChatHandler.getState()) {
            logger.info("Valorant Narrator disabled, ignoring message!");
            return;
        }
        if (message.getMessageType() == MessageType.PARTY && !ChatHandler.isPartyState()) {
            if (!(message.isOwnMessage() && ChatHandler.isSelfState())) {
                logger.info("Party/self messages disabled, ignoring message!");
                return;
            }
        } else if (message.getMessageType() == MessageType.TEAM && !ChatHandler.isTeamState()) {
            logger.info("Team messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.WHISPER && !ChatHandler.isPrivateState()) {
            logger.info("Private messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.ALL && !ChatHandler.isAllState()) {
            logger.info("All messages disabled, ignoring message!");
            return;
        }


        ChatHandler.messagesSent += 1;
        ChatHandler.charactersSent += message.getContent().length();
        Platform.runLater(() -> {
            ValNarratorController.latest_instance.setMessagesSent(ChatHandler.messagesSent);
            ValNarratorController.latest_instance.setCharactersNarrated(ChatHandler.charactersSent);
        });
        final String finalBody = message.getContent();
        try {
            MessageQuota mq = null;
            boolean keyNotPresent = false;
            if (System.getProperty("aws.accessKeyId") == null || System.getProperty("aws.secretKey") == null || System.getProperty("aws.sessionToken") == null) {
                mq = ChatHandler.addRequestQuota();
                keyNotPresent = true;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    VoiceGenerator.getInstance().speakVoice(ChatHandler.expandShortForms(finalBody), System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey"), System.getProperty("aws.sessionToken"));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            if (!keyNotPresent) mq = ChatHandler.addRequestQuota();
            if (mq.isPremium()) {
                Platform.runLater(() -> {
                    ValNarratorController.latest_instance.windowTitle.setVisible(false);
                    ValNarratorController.latest_instance.premiumWindowTitle.setVisible(true);
                    ValNarratorController.latest_instance.subscribeButton.setVisible(false);
                });
            } else {
                if (mq.remainingQuota() == 0) {
                    Platform.runLater(() -> {
                        ValNarratorController.latest_instance.quotaLabel.setText("Quota Exhausted!");
                        ValNarratorController.latest_instance.quotaLabel.setTextFill(Color.RED);
                        ValNarratorController.latest_instance.quotaBar.setProgress(0.0);
                    });
                } else {
                    int quota = mq.remainingQuota();
                    Platform.runLater(() -> {
                        ValNarratorController.latest_instance.quotaLabel.setText(quota + "/" + ChatHandler.quotaLimit);
                        ValNarratorController.latest_instance.quotaBar.setProgress((double) quota / ChatHandler.quotaLimit);
                    });
                }
            }
        } catch (java.lang.NullPointerException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean toggleState() {
        isDisabled = !isDisabled;
        return isDisabled;
    }

    public static boolean getState() {
        return isDisabled;
    }

    public static String expandShortForms(String message) {
        message = message.replaceAll("(?i)\\bGGWP\\b", "Good game,,,well played!");
        message = message.replaceAll("(?i)\\bGG\\b", "Good game!");
        message = message.replaceAll("(?i)\\bWP\\b", "Well played!");
        message = message.replaceAll("(?i)\\bMB\\b", "My bad!");
        message = message.replaceAll("(?i)\\bEZ\\b", "Easy!");
        message = message.replaceAll("(?i)\\bNT\\b", "Nice Try!");
        message = message.replaceAll("(?i)\\bBTW\\b", "By the way");
        message = message.replaceAll("(?i)\\bBRB\\b", "Be right back");
        message = message.replaceAll("(?i)\\bNS\\b", "Nice Shot!");
        message = message.replaceAll("(?i)\\bFR\\b", "For real");
        message = message.replaceAll("(?i)\\bIC\\b", "I see");
        message = message.replaceAll("(?i)\\bNC\\b", "Nice");
        message = message.replaceAll("(?i)\\bIKR\\b", "I know right");
        message = message.replaceAll("(?i)\\bLOL\\b", "Laughing out loud");
        message = message.replaceAll("(?i)\\bASF\\b", "As fuck");
        message = message.replaceAll("(?i)\\bIG\\b", "I guess");
        message = message.replaceAll("(?i)\\bTPED\\b", "Teleported");
        message = message.replaceAll("(?i)\\bGH\\b", "Good Half");
        message = message.replaceAll("(?i)\\bHP\\b", "Health");
        message = message.replaceAll("(?i)\\bNVM\\b", "Nevermind");
        message = message.replaceAll("(?i)\\bDM\\b", "Deathmatch");
        message = message.replaceAll("(?i)\\bUNR\\b", "Unrated");
        message = message.replaceAll("(?i)\\bCOMP\\b", "Competitive");
        message = message.replaceAll("(?i)\\bGL\\b", "Good luck");
        message = message.replaceAll("(?i)\\bGJ\\b", "Good Job");
        message = message.replaceAll("(?i)\\bNJ\\b", "Nice Job");
        message = message.replaceAll("(?i)\\bGLHF\\b", "Good luck, have fun");
        message = message.replaceAll("(?i)\\bWDYM\\b", "What do you mean?");
        message = message.replaceAll("(?i)\\bNP\\b", "No problem");
        message = message.replaceAll("(?i)\\bSMH\\b", "Shake my head");
        message = message.replaceAll("(?i)\\bTY\\b", "Thank you!");
        message = message.replaceAll("(?i)\\bSRY\\b", "Sorry!");
        message = message.replaceAll("(?i)\\bPLS\\b", "Please");
        String pattern = "(\\d+)hp";
        String replacement = "$1 health";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(message);
        message = matcher.replaceAll(replacement);
        return message;
    }

    public static HttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCertificates = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, null);
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    private static int getQuotaLimit() throws IOException {
        com.example.valnarratorencryption.Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/quotaLimit?hwid=" + Main.serialNumber + "&version=" + com.example.valnarratorgui.Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        String resp;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            resp = response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Integer.parseInt(resp);
    }

    public static MessageQuota getRequestQuota() throws IOException {
        com.example.valnarratorencryption.Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/remainingQuota?hwid=" + Main.serialNumber + "&version=" + com.example.valnarratorgui.Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 301) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Unsupported Version!");
                alert.setContentText("Please update your app, you're on a unsupported version.");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.showAndWait();
                System.exit(0);
            } else if (response.statusCode() == 300) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Information");
                alert.setContentText(response.body());
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.showAndWait();
                System.exit(0);
            }
            return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static MessageQuota addRequestQuota() throws IOException {
        com.example.valnarratorencryption.Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "?hwid=" + Main.serialNumber + "&version=" + com.example.valnarratorgui.Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 301) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Unsupported Version!");
                alert.setContentText("Please update your app, you're on a unsupported version.");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.showAndWait();
                System.exit(0);
            } else if (response.statusCode() == 300) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Information");
                alert.setContentText(response.body());
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.showAndWait();
                System.exit(0);
            }
            System.setProperty("aws.accessKeyId", response.headers().firstValue("Aws_access_key_id").get());
            System.setProperty("aws.secretKey", response.headers().firstValue("Aws_secret_access_key").get());
            System.setProperty("aws.sessionToken", response.headers().firstValue("Aws_session_token").get());
            return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPartyID() {
        return partyID;
    }

    public static void setPartyID(String partyID) {
        ChatHandler.partyID = partyID;
    }

    public static String getSelfID() {
        return selfID;
    }

    public static void setSelfID(String selfID) {
        ChatHandler.selfID = selfID;
    }
}
