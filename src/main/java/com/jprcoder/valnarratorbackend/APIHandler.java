package com.jprcoder.valnarratorbackend;

import com.google.gson.GsonBuilder;
import com.jprcoder.valnarratorencryption.Signature;
import com.jprcoder.valnarratorencryption.SignatureValidator;
import com.jprcoder.valnarratorgui.Main;
import com.jprcoder.valnarratorgui.ValNarratorApplication;
import com.jprcoder.valnarratorgui.ValNarratorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static com.jprcoder.valnarratorencryption.SignatureValidator.generateSignature;

record ValAccount(String game_name, String tag_line, String locale) {
}

public class APIHandler {
    public static final Logger logger = LoggerFactory.getLogger(APIHandler.class);
    private static final String valAPIUrl = "https://api.valnarrator.tech";
    private final ConnectionHandler connectionHandler;
    private boolean isPremium = false;

    public APIHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public int getQuotaLimit() throws IOException {
        Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/quotaLimit?hwid=" + Main.serialNumber + "&version=" + Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        String resp;
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            resp = response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Integer.parseInt(resp);
    }

    public MessageQuota getRequestQuota() throws IOException {
        Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "/remainingQuota?hwid=" + Main.serialNumber + "&version=" + Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();

        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 301) {
                ValNarratorApplication.showAlertAndWait("Unsupported Version!", "Please update your app, you're on a unsupported version.");
                System.exit(-1);
            } else if (response.statusCode() == 300) {
                ValNarratorApplication.showInformation("Information", response.body());
            }
            isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

            return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageQuota addRequestQuota() throws IOException {
        com.jprcoder.valnarratorencryption.Signature sign = SignatureValidator.generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(valAPIUrl + "?hwid=" + Main.serialNumber + "&version=" + Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).build();
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 301) {
                ValNarratorApplication.showAlertAndWait("Unsupported Version!", "Please update your app, you're on a unsupported version.");
                System.exit(-1);
            } else if (response.statusCode() == 300) {
                ValNarratorApplication.showInformation("Information", response.body());
            }
            isPremium = Boolean.parseBoolean(response.headers().firstValue("premium").get());

            System.setProperty("aws.accessKeyId", response.headers().firstValue("Aws_access_key_id").get());
            System.setProperty("aws.secretKey", response.headers().firstValue("Aws_secret_access_key").get());
            System.setProperty("aws.sessionToken", response.headers().firstValue("Aws_session_token").get());
            return new MessageQuota(Integer.parseInt(response.headers().firstValue("remainingQuota").get()), response.headers().firstValue("premiumTill").get(), Boolean.parseBoolean(response.headers().firstValue("premium").get()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ValAccount getLocalAccount(LockFileHandler lockFileHandler) {
        String accountUrl = String.format("https://127.0.0.1:%d/rso-auth/v1/authorization/userinfo", lockFileHandler.getPort());
        String auth = String.format("Basic %s", new String(Base64.getEncoder().encode(String.format("riot:%s", lockFileHandler.getPassword()).getBytes())));
        HttpRequest accRequest = HttpRequest.newBuilder().uri(URI.create(accountUrl)).setHeader("Authorization", auth).build();
        String resp;
        try {
            HttpResponse<String> response = connectionHandler.getClient().send(accRequest, HttpResponse.BodyHandlers.ofString());
            resp = response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        resp = resp.replace("\"{", "{").replace("}\"", "}").replace("\\\"", "\"");
        return new GsonBuilder().create().fromJson(resp, AccountJsonParser.class).userInfo().acct();
    }

    public String speakPremiumVoice(final String voice, final String text) throws IOException {
        Signature sign = generateSignature();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.valnarrator.tech/getPremiumVoice?hwid=" + Main.serialNumber + "&version=" + com.jprcoder.valnarratorgui.Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).POST(HttpRequest.BodyPublishers.ofString(String.format("""
                {
                  "emotion": "Neutral",
                  "name": "%s",
                  "text": "%s",
                  "speed": 1
                }""", voice, text))).setHeader("content-type", "application/json").build();
        HttpResponse<String> response;
        try {
            response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 503) {
                ValNarratorApplication.showAlert("Server Error!", "Custom voices is currently down, please try again later.");
                ValNarratorController.getLatestInstance().revertVoiceSelection();
                return null;
            }
            return response.body();
        } catch (InterruptedException e) {
            logger.warn("Exception while sending agent voice request!");
        }
        return null;
    }

    public String getSubscriptionURL() throws IOException, InterruptedException {
        String jsonPayload = String.format("{\n" + "  \"plan_id\": \"%s\",\n" + "  \"quantity\": \"1\",\n" + "  \"custom_id\": \"" + Main.serialNumber + "\",\n" + "   \"application_context\": {\n" + "     \"shipping_preference\": \"NO_SHIPPING\",\n" + "     \"return_url\": \"%s/payment_return\",\n" + "     \"cancel_url\": \"%s/payment_cancel\"\n" + "   }\n" + "}", Main.getProperties().getProperty("paymentPlanID"), Main.getProperties().getProperty("paymentLink"), Main.getProperties().getProperty("paymentLink"));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(Main.getProperties().getProperty("paymentLink"))).POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).setHeader("content-type", "application/json").build();

        HttpResponse<String> response = connectionHandler.getClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            java.util.Optional<String> subscriptionHeader = response.headers().firstValue("subscriptionURL");
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
