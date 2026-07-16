package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure-Java client for the Riot Client local API
 * (see
 * <a href="https://valapidocs.techchrism.me/">valapidocs.techchrism.me</a>).
 * <p>
 * Reads the lockfile for port + password, authenticates with HTTP Basic auth
 * ({@code riot:<password>}), and waits through several readiness stages to confirm the
 * Riot Client, local API, chat session and Valorant process are all up. It then exposes
 * the local player's identity ({@link #getSelfPuuid()} / {@link #getSelfName()}).
 * Chat messages themselves are delivered by the OCR sidecar, not this client.
 * <p>
 * The local API serves a self-signed certificate that is <em>not</em> issued
 * for
 * {@code 127.0.0.1}. A trust-all {@link TrustManager} accepts the certificate
 * chain,
 * but {@link HttpClient} still performs TLS hostname verification, which would
 * fail.
 * Hostname verification can only be turned off for {@code HttpClient} through
 * the
 * {@code jdk.internal.httpclient.disableHostnameVerification} system property
 * (its {@code SSLParameters} are ignored for this), set in the static
 * initializer below.
 */
public class RiotLocalApiClient {
    private static final Logger logger = LoggerFactory.getLogger(RiotLocalApiClient.class);
    // Stage timeouts (milliseconds).
    private static final long LOCKFILE_TIMEOUT_MS = 60_000;
    private static final long API_READY_TIMEOUT_MS = 300_000;
    private static final long CHAT_CONNECTED_TIMEOUT_MS = 240_000;
    private static final long VALORANT_DETECT_TIMEOUT_MS = 300_000;
    // Polling interval while we wait for Valorant to appear; short enough to keep startup
    // responsive without hammering task/session queries.
    private static final long VALORANT_DETECT_POLL_MS = 5_000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient httpClient;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private LockFileHandler lockFile;
    private String authHeader;
    private String baseUrl;
    private String selfPuuid;
    private String selfName;
    static {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }
    private ReadinessListener readinessListener;

    private String subjectDeployment;

    public RiotLocalApiClient() {
        this(buildLocalHttpClient());
    }

    /**
     * Test/advanced constructor allowing a custom {@link HttpClient}.
     */
    public RiotLocalApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private static HttpClient buildLocalHttpClient() {
        TrustManager[] trustAll = {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ctx).connectTimeout(Duration.ofSeconds(5)).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Failed to initialise local API TLS context", e);
        }
    }

    public void setReadinessListener(ReadinessListener listener) {
        this.readinessListener = listener;
    }

    // --- Connection lifecycle ------------------------------------------

    /**
     * Runs the full connection lifecycle on the calling thread (blocking).
     * Call from a background thread.
     */
    public void connectBlocking() throws IOException, InterruptedException {
        running.set(true);

        // Stage 1: lockfile.
        notifyStage("lockfile", "Waiting for Riot Client...");
        if (!waitForLockfile()) {
            throw new IOException("Timed out waiting for Riot Client lockfile (" + LOCKFILE_TIMEOUT_MS / 1000 + "s)");
        }
        readLockfile();
        notifyStage("lockfile", "Riot Client detected (port " + lockFile.getPort() + ").");

        // Stage 2: API reachable.
        notifyStage("api", "Connecting to local API...");
        if (!waitForApiReady()) {
            throw new IOException(
                    "Local API not reachable (" + API_READY_TIMEOUT_MS / 1000 + "s). Is the Riot Client running?");
        }
        notifyStage("api", "Local API ready.");

        // Stage 3: chat connected.
        notifyStage("chat", "Waiting for chat to connect...");
        JsonObject session = waitForChatConnected();
        if (session == null) {
            throw new IOException("Timed out waiting for chat to connect (" + CHAT_CONNECTED_TIMEOUT_MS / 1000 + "s)");
        }
        selfPuuid = session.has("puuid") ? session.get("puuid").getAsString() : null;
        selfName = session.has("game_name") ? session.get("game_name").getAsString() : null;
        logger.info("Chat connected (self name: {}).", selfName);
        notifyStage("chat", "Chat connected.");

        // Stage 4: Valorant process.
        notifyStage("valorant", "Waiting for Valorant...");
        if (!waitForValorant()) {
            throw new IOException(
                    "Timed out waiting for Valorant process (" + VALORANT_DETECT_TIMEOUT_MS / 1000 + "s)");
        }
        notifyStage("valorant", "Valorant detected.");

        notifyStage("ready", "Ready - listening for chat.");
    }

    /**
     * Stops the poll loop and ends the connection lifecycle.
     */
    public void disconnect() {
        running.set(false);
    }

    public String getSelfPuuid() {
        return selfPuuid;
    }

    /**
     * The local player's Riot display name (game name without tagline), as it
     * appears in the in-game chat box. Used by the OCR chat path for own-message
     * detection (OCR has no PUUID). May be {@code null} before chat connects.
     */
    public String getSelfName() {
        return selfName;
    }

    static String launchArgument(JsonObject session, String prefix) {
        if (!session.has("launchConfiguration") || !session.get("launchConfiguration").isJsonObject()) return null;
        JsonObject launchConfig = session.getAsJsonObject("launchConfiguration");
        if (!launchConfig.has("arguments") || !launchConfig.get("arguments").isJsonArray()) return null;

        for (JsonElement arg : launchConfig.getAsJsonArray("arguments")) {
            String value = arg.getAsString();
            if (value.startsWith(prefix)) return value.substring(prefix.length());
        }
        return null;
    }

    public LockFileHandler getLockFile() {
        return lockFile;
    }

    // --- Stage 1: lockfile ---------------------------------------------

    private boolean waitForLockfile() throws InterruptedException {
        long deadline = System.currentTimeMillis() + LOCKFILE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline && running.get()) {
            if (LockFileHandler.exists())
                return true;
            Thread.sleep(1000);
        }
        return LockFileHandler.exists();
    }

    private void readLockfile() throws IOException {
        this.lockFile = new LockFileHandler();
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(("riot:" + lockFile.getPassword()).getBytes(StandardCharsets.UTF_8));
        this.baseUrl = "https://127.0.0.1:" + lockFile.getPort();
        logger.info("Lockfile read (port {}).", lockFile.getPort());
    }

    // --- Stage 2: API reachable ----------------------------------------

    /**
     * Waits until the local API answers an HTTP request at all. Any HTTP response
     * (even 404) proves the TLS handshake + connection succeeded, which is what we
     * need here; specific readiness is checked in {@link #waitForChatConnected()}.
     */
    private boolean waitForApiReady() throws InterruptedException {
        long deadline = System.currentTimeMillis() + API_READY_TIMEOUT_MS;
        int attempt = 0;
        while (System.currentTimeMillis() < deadline && running.get()) {
            try {
                int status = doGet("/help").statusCode();
                logger.debug("Local API reachable (HTTP {}).", status);
                return true;
            } catch (Exception e) {
                if (attempt % 5 == 0)
                    logger.debug("Local API not reachable yet: {}", e.getMessage());
                try {
                    if (LockFileHandler.exists())
                        readLockfile();
                } catch (IOException ignored) {
                }
            }
            attempt++;
            Thread.sleep(2000);
        }
        return false;
    }

    // --- Stage 3: chat connected ---------------------------------------

    private JsonObject waitForChatConnected() throws InterruptedException {
        long deadline = System.currentTimeMillis() + CHAT_CONNECTED_TIMEOUT_MS;
        String lastState = "";
        while (System.currentTimeMillis() < deadline && running.get()) {
            try {
                HttpResponse<String> resp = doGet("/chat/v1/session");
                if (resp.statusCode() == 200) {
                    JsonObject session = JsonParser.parseString(resp.body()).getAsJsonObject();
                    String state = session.has("state") ? session.get("state").getAsString() : "unknown";
                    if (!state.equals(lastState)) {
                        logger.debug("Chat session state: {}", state);
                        notifyStage("chat", "Chat state: " + state);
                        lastState = state;
                    }
                    if ("connected".equals(state))
                        return session;
                }
            } catch (Exception e) {
                // Expected connection errors during startup; refresh the lockfile in case it
                // rotated.
                try {
                    if (LockFileHandler.exists())
                        readLockfile();
                } catch (IOException ignored) {
                }
            }
            Thread.sleep(2000);
        }
        return null;
    }

    // --- Stage 4: Valorant detection -----------------------------------

    /**
     * Valorant deployment/region from the current game launch args (for example
     * {@code ap}). Used with {@link #getSelfPuuid()} to target the active local
     * Valorant config folder ({@code <puuid>-<deployment>}).
     */
    public String getSubjectDeployment() {
        return subjectDeployment;
    }

    private boolean waitForValorant() throws InterruptedException {
        long deadline = System.currentTimeMillis() + VALORANT_DETECT_TIMEOUT_MS;
        boolean loggedWait = false;
        while (System.currentTimeMillis() < deadline && running.get()) {
            try {
                HttpResponse<String> resp = doGet("/product-session/v1/external-sessions");
                if (resp.statusCode() == 200) {
                    JsonObject sessions = JsonParser.parseString(resp.body()).getAsJsonObject();
                    for (String key : sessions.keySet()) {
                        JsonElement el = sessions.get(key);
                        if (el.isJsonObject()) {
                            JsonObject s = el.getAsJsonObject();
                            if (s.has("productId") && "valorant".equals(s.get("productId").getAsString())) {
                                String subject = launchArgument(s, "-subject=");
                                if (selfPuuid == null && subject != null) selfPuuid = subject;
                                subjectDeployment = launchArgument(s, "-ares-deployment=");
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (!loggedWait) {
                    logger.debug("Valorant session not found yet; waiting.");
                    loggedWait = true;
                }
            }
            Thread.sleep(VALORANT_DETECT_POLL_MS);
        }
        return false;
    }

    // --- REST helper ---------------------------------------------------

    private HttpResponse<String> doGet(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Authorization", authHeader)
                .timeout(REQUEST_TIMEOUT).GET().build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private void notifyStage(String stage, String detail) {
        logger.info("[{}] {}", stage, detail);
        if (readinessListener != null)
            readinessListener.onStageUpdate(stage, detail);
    }

    // --- Listener interfaces -------------------------------------------

    /**
     * Receives connection-stage updates for surfacing progress in the UI.
     */
    @FunctionalInterface
    public interface ReadinessListener {
        void onStageUpdate(String stage, String detail);
    }
}
