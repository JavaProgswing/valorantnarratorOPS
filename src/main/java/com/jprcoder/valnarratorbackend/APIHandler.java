package com.jprcoder.valnarratorbackend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jprcoder.valnarratorencryption.Signature;
import com.jprcoder.valnarratorencryption.SignatureValidator;
import com.jprcoder.valnarratorgui.ValNarratorApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.jprcoder.valnarratorbackend.AWSSignatureV4Generator.hash;
import static com.jprcoder.valnarratorencryption.SignatureValidator.generateSignature;
import static com.jprcoder.valnarratorgui.Main.*;

record SettingsData(String data, String type) {
}

public class APIHandler {
    private static final Logger logger = LoggerFactory.getLogger(APIHandler.class);
    private static final String valAPIUrl = "https://api-valnarrator.vercel.app";
    private final ConnectionHandler connectionHandler;
    private boolean isPremium = false;

    public APIHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public static <T> HttpResponse<T> retryUntilSuccess(HttpClient client, HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        while (true) {
            try {
                var resp = client.send(request, handler);
                if (resp.statusCode() == 401 && request.uri().toString().startsWith(valAPIUrl)) {
                    reEncryptSignup();
                }
                return resp;
            } catch (IOException | InterruptedException e) {
                logger.warn("{} failed, retrying... {}", request, e.getMessage());

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static VersionInfo fetchVersionInfo() throws InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/version/latest/info")).build();
        HttpResponse<String> response = retryUntilSuccess(client, request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        return new Gson().fromJson(responseBody, VersionInfo.class);
    }

    public static RegistrationInfo fetchRegistrationInfo(int currentCount, int retryCount) throws IOException, InterruptedException, OutdatedVersioningException {
        if (currentCount > retryCount) {
            throw new IOException("Authorization failed!");
        }
        HttpClient client = HttpClient.newHttpClient();
        Signature sign = SignatureValidator.generateRegistrationSignature(serialNumber);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/register?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        HttpResponse<String> response = retryUntilSuccess(client, request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        if (response.statusCode() == 502) {
            throw new IOException("API Service is down, please try again");
        }
        if (response.statusCode() == 409) {
            return new RegistrationInfo(false, response.headers().firstValue("Signature").get(), response.headers().firstValue("Salt").get(), Boolean.parseBoolean(response.headers().firstValue("Referred").get()));
        }
        if (response.statusCode() == 426) {
            throw new OutdatedVersioningException();
        }
        if (response.statusCode() == 401) {
            logger.debug("Unauthorized request, retrying... x{}", currentCount);
            return fetchRegistrationInfo(currentCount + 1, retryCount);
        }
        return new RegistrationInfo(true, response.headers().firstValue("Signature").get(), response.headers().firstValue("Salt").get(), Boolean.parseBoolean(response.headers().firstValue("Referred").get()));
    }

    public static void downloadAgentVoice(ProgressCallback callback) throws IOException, InterruptedException {
        String url = valAPIUrl + "/agentvoice/download";

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        long totalBytes = resp.headers().firstValueAsLong("Content-Length").orElse(-1);

        logger.debug("Total bytes: {}", totalBytes);

        InputStream input = resp.body();

        File targetDir = new File(System.getenv("LOCALAPPDATA").replace("\\", "/") + "/ValorantNarrator");
        targetDir.mkdirs();

        File temp = File.createTempFile("agentvoice", ".exe");

        try (BufferedInputStream bis = new BufferedInputStream(input); FileOutputStream fos = new FileOutputStream(temp)) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int read;

            while ((read = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                downloaded += read;

                if (callback != null) {
                    double percent = totalBytes > 0 ? (100.0 * downloaded / totalBytes) : -1;
                    callback.onProgress(percent, downloaded, totalBytes);
                }
            }
        }

        // Move to final location
        File outFile = new File(targetDir, "valorantNarrator-agentVoices.exe");
        Files.copy(temp.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void downloadLatestVersion() throws IOException {
        final String installerLocation = Paths.get(System.getenv("Temp"), "ValorantNarrator").toString();
        URL url;
        URLConnection con = null;
        DataInputStream dis;
        FileOutputStream fos;
        byte[] fileData;
        try {
            url = new URL(valAPIUrl + "/installer/version/latest");
            while (con == null) {
                try {
                    con = url.openConnection();
                } catch (IOException e) {
                    logger.warn("/downloadLatestVersion failed, retrying... {}", e.getMessage());
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
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

        logger.debug("Starting installer from {}", installerLocation);
        Runtime.getRuntime().exec(String.format("cmd.exe /K \"cd %s && %s /silent\"", installerLocation, installerName));
    }

    public static boolean verifyReferralUser(String userId) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/user/" + userId)).build();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        HttpResponse<String> response = retryUntilSuccess(client, request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        return response.statusCode() == 200;
    }

    public static ReferralResponse submitReferral(String referrerId) {
        HttpClient client = HttpClient.newHttpClient();
        Signature sign = SignatureValidator.generateRegistrationSignature(serialNumber);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/refer?hwid=" + serialNumber + "&referrer=" + referrerId)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        HttpResponse<String> response = retryUntilSuccess(client, request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        if (response.statusCode() == 201) {
            return new Gson().fromJson(responseBody, ReferralResponse.class);
        }
        throw new IllegalStateException("Referral submission failed with status code: " + response.statusCode());
    }

    public AbstractMap.Entry<HttpResponse<InputStream>, InputStream> speakVoice(String text, short rate, String currentVoice, VoiceEngineType engineType, String accessKeyID, String secretKey, String sessionToken) throws QuotaExhaustedException {
        String requestBody = String.format("{\"Engine\":\"%s\",\"OutputFormat\":\"mp3\",\"Text\":\"<speak><prosody rate='%d%%'>%s</prosody></speak>\",\"VoiceId\":\"%s\",\"TextType\":\"ssml\"}", engineType.toValue(), rate, text, currentVoice);
        TreeMap<String, String> preheaders = new TreeMap<>();
        try {
            preheaders.put("x-amz-content-sha256", hash(requestBody));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        preheaders.put("x-amz-security-token", sessionToken);
        preheaders.put("host", "polly.ap-south-1.amazonaws.com");
        preheaders.put("content-type", "application/json");
        AWSSignatureV4Generator gen = new AWSSignatureV4Generator.Builder(accessKeyID, secretKey).payload(requestBody).serviceName("polly").httpMethodName("POST").regionName("ap-south-1").canonicalURI("/v1/speech").queryParameters(new TreeMap<>()).awsHeaders((TreeMap<String, String>) preheaders.clone()).build();
        try {
            Map<String, String> headers = gen.getHeaders();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create("https://polly.ap-south-1.amazonaws.com/v1/speech")).POST(HttpRequest.BodyPublishers.ofString(requestBody));

            for (Map.Entry<String, String> entry : preheaders.entrySet()) {
                if (!entry.getKey().equals("host")) {
                    reqBuilder.header(entry.getKey().toLowerCase(), entry.getValue());
                }
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                reqBuilder.header(entry.getKey(), entry.getValue());
            }
            HttpResponse<InputStream> response = retryUntilSuccess(connectionHandler.getClient(), reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            logger.debug(String.valueOf(response));
            if (response.statusCode() == 403) {
                logger.warn(String.format("Last refresh occurred %d ms ago, token has expired. Refreshing!", (System.currentTimeMillis() - VoiceTokenHandler.getLAST_REFRESH_MS())));
                addRequestQuota();
                final String id1 = System.getProperty("aws.accessKeyId"), key1 = System.getProperty("aws.secretKey"), sessionToken1 = System.getProperty("aws.sessionToken");
                return speakVoice(text, rate, currentVoice, engineType, id1, key1, sessionToken1);
            }
            logger.debug(String.valueOf(response.headers()));
            return new AbstractMap.SimpleEntry<>(response, response.body());
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            throw new RuntimeException(e);
        } catch (OutdatedVersioningException e) {
            ValNarratorApplication.showDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
            throw new RuntimeException(e);
        }
    }

    public MessageQuota getRequestQuota() throws IOException, OutdatedVersioningException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/remainingQuota?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();

        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        if (response.statusCode() == 301) {
            ValNarratorApplication.showAlertAndWait("Unsupported Version!", "Please update your app, you're on a unsupported version.");
            System.exit(-1);
        } else if (response.statusCode() == 300) {
            ValNarratorApplication.showInformation("Information", responseBody);
        } else if (response.statusCode() == 426) {
            throw new OutdatedVersioningException();
        }
        isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

        return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
    }

    public int getQuotaLimit() throws IOException, OutdatedVersioningException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/quotaLimit?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        final String responseBody;
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        responseBody = response.body();

        if (response.statusCode() == 426) {
            throw new OutdatedVersioningException();
        } else if (response.statusCode() == 401) {
            logger.debug("Unauthorized request, retrying... ");
            return getQuotaLimit();
        }
        logger.debug(String.valueOf(responseBody));
        return Integer.parseInt(responseBody);
    }

    public MessageQuota addRequestQuota() throws IOException, QuotaExhaustedException, OutdatedVersioningException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        logger.debug(String.valueOf(response.headers()));
        if (response.statusCode() == 301) {
            ValNarratorApplication.showAlertAndWait("Unsupported Version!", "Please update your app, you're on a unsupported version.");
            System.exit(-1);
        } else if (response.statusCode() == 300) {
            ValNarratorApplication.showInformation("Information", responseBody);
        } else if (response.statusCode() == 403) {
            throw new QuotaExhaustedException(Long.parseLong(response.headers().firstValue("refreshesIn").get()));
        } else if (response.statusCode() == 426) {
            throw new OutdatedVersioningException();
        }
        isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

        System.setProperty("aws.accessKeyId", response.headers().firstValue("aws-access-key-id").get());
        System.setProperty("aws.secretKey", response.headers().firstValue("aws-secret-access-key").get());
        System.setProperty("aws.sessionToken", response.headers().firstValue("aws-session-token").get());
        final MessageQuota messageQuota = new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));

        if (!messageQuota.isPremium()) ChatDataHandler.getInstance().updateQuota(messageQuota.remainingQuota());
        return messageQuota;
    }

    public EntitlementsTokenResponse getEntitlement(LockFileHandler lockFileHandler) {
        String entitlementUrl = String.format("https://127.0.0.1:%d/entitlements/v1/token", lockFileHandler.getPort());
        String auth = String.format("Basic %s", new String(Base64.getEncoder().encode(String.format("riot:%s", lockFileHandler.getPassword()).getBytes())));
        HttpRequest entitlementReq = HttpRequest.newBuilder().uri(URI.create(entitlementUrl)).setHeader("Authorization", auth).build();
        final String responseBody;
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), entitlementReq, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        responseBody = response.body().replace("\"{", "{").replace("}\"", "}").replace("\\\"", "\"");
        logger.debug(responseBody);
        return new Gson().fromJson(responseBody, EntitlementsTokenResponse.class);
    }

    public String getClientPlatform() {
        return "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";
    }

    public ArrayList<PlayerAccount> getPlayerNames(final String accessToken, final RiotClientDetails riotClientDetails, final String entitlementToken, final ArrayList<String> playerIDs) {
        final Gson gson = new Gson();
        HttpRequest playerReq = HttpRequest.newBuilder().uri(URI.create(String.format("https://pd.%s.a.pvp.net/name-service/v2/players", riotClientDetails.subject_deployment()))).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-Entitlements-JWT", entitlementToken).header("X-Riot-ClientVersion", riotClientDetails.version()).PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(playerIDs))).build();
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), playerReq, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        Type playerListType = new TypeToken<List<PlayerAccount>>() {
        }.getType();

        return gson.fromJson(responseBody, playerListType);
    }

    public RiotClientDetails getRiotClientDetails(LockFileHandler lockFileHandler) {
        String riotClientSessionUrl = String.format("https://127.0.0.1:%d/product-session/v1/external-sessions", lockFileHandler.getPort());
        String auth = String.format("Basic %s", new String(Base64.getEncoder().encode(String.format("riot:%s", lockFileHandler.getPassword()).getBytes())));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(riotClientSessionUrl)).setHeader("Authorization", auth).build();
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, JsonObject>>() {
        }.getType();
        Map<String, JsonObject> sessionsResponse = gson.fromJson(responseBody, type);
        String version = null, subject_id = null, subject_deployment = null;
        for (Map.Entry<String, JsonObject> entry : sessionsResponse.entrySet()) {
            if (entry.getKey().equals("host_app")) continue;
            JsonObject sessionData = entry.getValue();
            version = sessionData.get("version").getAsString();
            JsonObject launchConfig = sessionData.get("launchConfiguration").getAsJsonObject();
            JsonArray arguments = launchConfig.get("arguments").getAsJsonArray();
            for (var arg : arguments) {
                if (arg.getAsString().startsWith("-subject=")) {
                    subject_id = arg.getAsString().split("=")[1];
                }
                if (arg.getAsString().startsWith("-ares-deployment=")) {
                    subject_deployment = arg.getAsString().split("=")[1];
                }
            }
        }

        if (version == null || subject_id == null || subject_deployment == null) {
            logger.warn("Failed to retrieve Riot Client details, retrying...");
            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getRiotClientDetails(lockFileHandler);
        }

        return new RiotClientDetails(version, subject_id, subject_deployment);
    }

    public String getSubscriptionURL() {
        return String.format("https://valnarrator.vercel.app/?user-id=%s", serialNumber);
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public String getEncodedPlayerSettings(final String accessToken, final String version) {
        HttpRequest settingsReq = HttpRequest.newBuilder().uri(URI.create("https://player-preferences-usw2.pp.sgp.pvp.net/playerPref/v3/getPreference/Ares.PlayerSettings")).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-ClientVersion", version).build();
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), settingsReq, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        final String encodedPlayerSettings = String.valueOf(responseBody);
        logger.debug(encodedPlayerSettings);
        return new Gson().fromJson(encodedPlayerSettings, JsonObject.class).get("data").getAsString();
    }

    public void setEncodedPlayerSettings(final String accessToken, final String version, final String encodedSettings) {
        final SettingsData settingsData = new SettingsData(encodedSettings, "Ares.PlayerSettings");
        final String jsonString = new Gson().toJson(settingsData);
        HttpRequest settingsReq = HttpRequest.newBuilder().uri(URI.create("https://player-preferences-usw2.pp.sgp.pvp.net/playerPref/v3/savePreference")).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-ClientVersion", version).PUT(HttpRequest.BodyPublishers.ofString(jsonString)).header("Content-Type", "application/json").build();
        HttpResponse<String> response = retryUntilSuccess(connectionHandler.getClient(), settingsReq, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
    }
}
