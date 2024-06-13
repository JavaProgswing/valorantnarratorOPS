package com.jprcoder.valnarratorbackend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jprcoder.valnarratorencryption.Signature;
import com.jprcoder.valnarratorencryption.SignatureValidator;
import com.jprcoder.valnarratorgui.ValNarratorApplication;
import com.jprcoder.valnarratorgui.ValNarratorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
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
import java.util.*;

import static com.jprcoder.valnarratorbackend.AWSSignatureV4Generator.hash;
import static com.jprcoder.valnarratorencryption.SignatureValidator.generateSignature;
import static com.jprcoder.valnarratorgui.Main.*;

record SettingsData(String data, String type) {
}

public class APIHandler {
    public static final Logger logger = LoggerFactory.getLogger(APIHandler.class);
    private static final String valAPIUrl = "https://api.valnarrator.tech";
    private final ConnectionHandler connectionHandler;
    private boolean isPremium = false;

    public APIHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public static VersionInfo fetchVersionInfo() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/version/latest/info")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        return new Gson().fromJson(responseBody, VersionInfo.class);
    }

    public static RegistrationInfo fetchRegistrationInfo() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        Signature sign = SignatureValidator.generateRegistrationSignature(serialNumber);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/register?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        if (response.statusCode() == 502) {
            throw new IOException("API Service is down, please try again");
        }
        if (response.statusCode() == 409) {
            return new RegistrationInfo(false, response.headers().firstValue("Signature").get(), response.headers().firstValue("Salt").get());
        }
        return new RegistrationInfo(true, response.headers().firstValue("Signature").get(), response.headers().firstValue("Salt").get());
    }

    public static void downloadLatestVersion() throws IOException {
        final String installerLocation = Paths.get(System.getenv("Temp"), "ValorantNarrator").toString();
        URL url;
        URLConnection con;
        DataInputStream dis;
        FileOutputStream fos;
        byte[] fileData;
        try {
            url = new URL(valAPIUrl + "/installer/version/latest");
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
    }

    public AbstractMap.Entry<HttpResponse<InputStream>, InputStream> speakVoice(String text, String currentVoice, VoiceEngineType engineType, String accessKeyID, String secretKey, String sessionToken) throws QuotaExhaustedException {
        String requestBody = String.format("{\"Engine\":\"%s\",\"OutputFormat\":\"mp3\",\"Text\":\"%s\",\"VoiceId\":\"%s\"}", engineType.toValue(), text, currentVoice);
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
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://polly.ap-south-1.amazonaws.com/v1/speech"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            for (Map.Entry<String, String> entry : preheaders.entrySet()) {
                if (!entry.getKey().equals("host")) {
                    reqBuilder.header(entry.getKey().toLowerCase(), entry.getValue());
                }
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                reqBuilder.header(entry.getKey(), entry.getValue());
            }
            HttpResponse<InputStream> response = connectionHandler.getClient().send(reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            logger.debug(String.valueOf(response));
            if (response.statusCode() == 403) {
                logger.warn(String.format("Last refresh occurred %d ms ago, token has expired. Refreshing!", (System.currentTimeMillis() - VoiceTokenHandler.getLAST_REFRESH_MS())));
                addRequestQuota();
                final String id1 = System.getProperty("aws.accessKeyId"), key1 = System.getProperty("aws.secretKey"), sessionToken1 = System.getProperty("aws.sessionToken");
                return speakVoice(text, currentVoice, engineType, id1, key1, sessionToken1);
            }
            logger.debug(String.valueOf(response.headers()));
            return new AbstractMap.SimpleEntry<>(response, response.body());
        } catch (NoSuchAlgorithmException | InterruptedException | InvalidKeyException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getQuotaLimit() throws IOException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/quotaLimit?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        final String responseBody;
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            responseBody = response.body();
            logger.debug(String.valueOf(responseBody));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Integer.parseInt(responseBody);
    }

    public MessageQuota getRequestQuota() throws IOException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/remainingQuota?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();

        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            final String responseBody = response.body();
            logger.debug(String.valueOf(responseBody));
            if (response.statusCode() == 301) {
                ValNarratorApplication.showAlertAndWait("Unsupported Version!", "Please update your app, you're on a unsupported version.");
                System.exit(-1);
            } else if (response.statusCode() == 300) {
                ValNarratorApplication.showInformation("Information", responseBody);
            }
            isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

            return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageQuota addRequestQuota() throws IOException, QuotaExhaustedException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
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
            }
            isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

            System.setProperty("aws.accessKeyId", response.headers().firstValue("Aws_access_key_id").get());
            System.setProperty("aws.secretKey", response.headers().firstValue("Aws_secret_access_key").get());
            System.setProperty("aws.sessionToken", response.headers().firstValue("Aws_session_token").get());
            final MessageQuota messageQuota = new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
            ChatDataHandler.getInstance().updateQuota(messageQuota.remainingQuota());
            return messageQuota;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public EntitlementsTokenResponse getEntitlement(LockFileHandler lockFileHandler) {
        String entitlementUrl = String.format("https://127.0.0.1:%d/entitlements/v1/token", lockFileHandler.getPort());
        String auth = String.format("Basic %s", new String(Base64.getEncoder().encode(String.format("riot:%s", lockFileHandler.getPassword()).getBytes())));
        HttpRequest entitlementReq = HttpRequest.newBuilder().uri(URI.create(entitlementUrl)).setHeader("Authorization", auth).build();
        final String responseBody;
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(entitlementReq, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            responseBody = response.body().replace("\"{", "{").replace("}\"", "}").replace("\\\"", "\"");
            logger.debug(responseBody);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new Gson().fromJson(responseBody, EntitlementsTokenResponse.class);
    }

    public ArrayList<PlayerAccount> getPlayerNames(final String accessToken, final RiotClientDetails riotClientDetails, final String entitlementToken, final ArrayList<String> playerIDs) {
        final Gson gson = new Gson();
        HttpRequest playerReq = HttpRequest.newBuilder().uri(URI.create(String.format("https://pd.%s.a.pvp.net/name-service/v2/players", riotClientDetails.subject_deployment()))).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-Entitlements-JWT", entitlementToken).header("X-Riot-ClientVersion", riotClientDetails.version()).PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(playerIDs))).build();
        try {

            HttpResponse<String> response = connectionHandler.getClient().send(playerReq, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            final String responseBody = response.body();
            logger.debug(String.valueOf(responseBody));
            Type playerListType = new TypeToken<List<PlayerAccount>>() {
            }.getType();

            return gson.fromJson(responseBody, playerListType);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public RiotClientDetails getRiotClientDetails(LockFileHandler lockFileHandler) {
        String riotClientSessionUrl = String.format("https://127.0.0.1:%d/product-session/v1/external-sessions", lockFileHandler.getPort());
        String auth = String.format("Basic %s", new String(Base64.getEncoder().encode(String.format("riot:%s", lockFileHandler.getPassword()).getBytes())));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(riotClientSessionUrl)).setHeader("Authorization", auth).build();
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
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

            return new RiotClientDetails(version, subject_id, subject_deployment);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public String getClientPlatform() {
        return "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";
    }

    public String getEncodedPlayerSettings(final String accessToken, final String version) {
        HttpRequest settingsReq = HttpRequest.newBuilder().uri(URI.create("https://playerpreferences.riotgames.com/playerPref/v3/getPreference/Ares.PlayerSettings")).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-ClientVersion", version).build();
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(settingsReq, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            final String responseBody = response.body();
            final String encodedPlayerSettings = String.valueOf(responseBody);
            logger.debug(encodedPlayerSettings);
            return new Gson().fromJson(encodedPlayerSettings, JsonObject.class).get("data").getAsString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEncodedPlayerSettings(final String accessToken, final String version, final String encodedSettings) {
        final SettingsData settingsData = new SettingsData(encodedSettings, "Ares.PlayerSettings");
        final String jsonString = new Gson().toJson(settingsData);
        HttpRequest settingsReq = HttpRequest.newBuilder().uri(URI.create("https://playerpreferences.riotgames.com/playerPref/v3/savePreference")).setHeader("Authorization", String.format("Bearer %s", accessToken)).header("X-Riot-ClientPlatform", getClientPlatform()).header("X-Riot-ClientVersion", version).PUT(HttpRequest.BodyPublishers.ofString(jsonString)).header("Content-Type", "application/json").build();
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(settingsReq, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            final String responseBody = response.body();
            logger.debug(String.valueOf(responseBody));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractMap.Entry<HttpResponse<String>, String> speakPremiumVoice(final String voice, final String text) throws IOException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/getPremiumVoice?hwid=" + serialNumber + "&version=" + currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).POST(HttpRequest.BodyPublishers.ofString(String.format("""
                {
                  "emotion": "Neutral",
                  "name": "%s",
                  "text": "%s",
                  "speed": 1
                }""", voice, text))).setHeader("content-type", "application/json").build();
        HttpResponse<String> response;
        try {
            response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug(String.valueOf(response));
            final String responseBody = response.body();
            logger.debug(String.valueOf(responseBody));
            if (response.statusCode() == 503) {
                ValNarratorApplication.showAlert("Server Error!", "Custom voices is currently down, please try again later.");
                ValNarratorController.getLatestInstance().revertVoiceSelection();
                return new AbstractMap.SimpleEntry<>(response, null);
            }
            return new AbstractMap.SimpleEntry<>(response, responseBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSubscriptionURL() throws IOException, InterruptedException {
        String jsonPayload = String.format("{\"plan_id\": \"%s\",\n\"quantity\": \"1\",\n\"custom_id\": \"%s\",\n\"application_context\": {\n\"shipping_preference\": \"NO_SHIPPING\",\n\"return_url\": \"%s/payment_return\",\n\"cancel_url\": \"%s/payment_cancel\"\n}\n}", getProperties().getProperty("paymentPlanID"), serialNumber, getProperties().getProperty("paymentLink"), getProperties().getProperty("paymentLink"));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(getProperties().getProperty("paymentLink"))).POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).setHeader("content-type", "application/json").build();

        HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug(String.valueOf(response));
        final String responseBody = response.body();
        logger.debug(String.valueOf(responseBody));
        if (response.statusCode() == 200) {
            Optional<String> subscriptionHeader = response.headers().firstValue("subscriptionURL");
            if (subscriptionHeader.isPresent()) {
                return subscriptionHeader.get();
            }
        }
        return null;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}
