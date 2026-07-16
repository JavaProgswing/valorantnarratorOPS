package com.jprcoder.valnarratorbackend;

import com.jprcoder.valnarratorgui.Main;
import com.jprcoder.valnarratorgui.ValNarratorApplication;
import com.jprcoder.valnarratorgui.ValNarratorController;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.expandShortForms;

public class ChatDataHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatDataHandler.class);
    private static final ChatDataHandler singleton;

    /**
     * Narration runs on a single dedicated daemon thread (not the shared ForkJoin commonPool the
     * Riot API also uses) with a bounded queue, so a burst of chat cannot pile up blocked threads
     * or starve the rest of the app. {@link VoiceGenerator#speakVoice} already serializes playback;
     * this makes that ordering FIFO and stops the backlog from growing without bound. When the
     * queue is full the oldest not-yet-started narration is dropped (kept: the newest, most relevant
     * lines) rather than letting latency snowball.
     */
    private static final int NARRATION_QUEUE_CAPACITY = 8;
    /**
     * Repeat-suppression window: the same line re-emitted within this many ms narrates once.
     */
    private static final long DUPLICATE_WINDOW_MS = 2500;

    private final ThreadPoolExecutor narrationExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(NARRATION_QUEUE_CAPACITY),
            r -> {
                Thread t = new Thread(r, "narration-dispatch");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private final NarrationText.RecentDuplicateFilter recentNarrations =
            new NarrationText.RecentDuplicateFilter(DUPLICATE_WINDOW_MS, 32);

    static {
        try {
            singleton = new ChatDataHandler();
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Chat properties;
    private final APIHandler APIHandler;

    private ChatDataHandler() throws NoSuchAlgorithmException, KeyManagementException, IOException {
        ConnectionHandler connectionHandler = new ConnectionHandler();
        APIHandler = new APIHandler(connectionHandler);
        try {
            properties = new Chat(APIHandler.getQuotaLimit());
        } catch (OutdatedVersioningException e) {
            ValNarratorApplication.showDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.WARNING_MESSAGE);
            throw new RuntimeException(e);
        }
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateSingleton() {
    }

    public static ChatDataHandler getInstance() {
        return singleton;
    }

    public APIHandler getAPIHandler() {
        return APIHandler;
    }

    public void initialize() throws IOException {
        Platform.runLater(() -> ValNarratorController.getLatestInstance().setUserIDLabel(Main.serialNumber));

        MessageQuota mq;
        try {
            mq = APIHandler.getRequestQuota();
        } catch (OutdatedVersioningException e) {
            ValNarratorApplication.showDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.WARNING_MESSAGE);
            throw new RuntimeException(e);
        }
        ValNarratorController.getLatestInstance().updateRequestQuota(mq);
        APIHandler.setPremium(mq.isPremium());
    }

    /**
     * Applies the active narration filters - globally disabled, ignored players, the
     * per-source toggles (whisper / party / team / all) and the self/all rules - to an
     * incoming message. If it passes, the message is narrated asynchronously, message
     * stats are updated, and the sender's display name is learned for the dropdown.
     */
    public void message(Message message) {
        if (properties.isDisabled()) {
            logger.debug("Valorant Narrator disabled, ignoring message!");
            return;
        }
        if (properties.isIgnoredPlayerID(message.getUserId())) {
            logger.debug("Ignoring message from {}!", properties.getPlayerIDTable().get(message.getUserId()));
            return;
        }

        // The self/own-message toggle dictates: when it is off, never narrate the
        // local player's own messages, regardless of channel.
        if (message.isOwnMessage() && !properties.isSelfState()) {
            logger.debug("Self messages disabled, ignoring own message!");
            return;
        }

        if (message.getMessageType() == MessageType.WHISPER && !properties.isPrivateState()) {
            logger.debug("Private messages disabled, ignoring message!");
            return;
        }
        if (message.isOwnMessage() && properties.isSelfState()) {
            // Self messages enabled, skipping filtering checks.
        } else if (message.getMessageType() == MessageType.PARTY && !properties.isPartyState()) {
            logger.debug("Party messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.TEAM && !properties.isTeamState()) {
            logger.debug("Team messages disabled, ignoring message!");
            return;
        }

        if (message.getMessageType() == MessageType.ALL) {
            if (!properties.isAllState()) {
                logger.debug("All messages disabled, ignoring message!");
                return;
            } else if (message.isOwnMessage() && !properties.isSelfState()) {
                logger.debug("(ALL)Self messages disabled, ignoring message!");
                return;
            }
        }
        final String finalBody = message.getContent().replace("/", "").replace("\\", "");
        final String toSpeak = NarrationText.cap(expandShortForms(finalBody), NarrationText.MAX_NARRATION_CHARS);
        if (toSpeak == null || toSpeak.isBlank()) {
            return; // nothing left to narrate after sanitising
        }
        if (!recentNarrations.allow(NarrationText.key(toSpeak), System.currentTimeMillis())) {
            logger.debug("Suppressing repeated narration: '{}'", toSpeak);
            return;
        }
        enqueueNarration(toSpeak);
        properties.updateMessageStats(message);
        Platform.runLater(() -> {
            ValNarratorController controller = ValNarratorController.getLatestInstance();
            controller.setMessagesSent(properties.getMessagesSent());
            controller.setCharactersNarrated(properties.getCharactersSent());
            controller.setWordsNarrated(properties.getWordsSent());
            controller.setAverageLength(properties.getAverageMessageLength());
        });

        if (!properties.getPlayerIDTable().containsKey(message.getUserId())) {
            final String playerName = message.getUserId().trim();
            properties.getPlayerIDTable().put(playerName, playerName);
            properties.getPlayerNameTable().put(playerName, playerName);
            Platform.runLater(() -> ValNarratorController.getLatestInstance().playerDropdown.getItems().addAll(playerName));
        }
    }

    /**
     * Hands the (already sanitised, capped and de-duplicated) text to the serial narration thread.
     * The executor's {@code DiscardOldestPolicy} sheds the oldest queued line if narration has
     * fallen behind, so latency stays bounded instead of snowballing.
     */
    private void enqueueNarration(final String text) {
        try {
            narrationExecutor.execute(() -> {
                try {
                    VoiceGenerator.getInstance().speakVoice(text);
                } catch (IOException e) {
                    logger.warn("Failed to narrate message: {}", e.getMessage());
                } catch (QuotaExhaustedException e) {
                    logger.warn("Quota exhausted, {}", e.getMessage());
                    ValNarratorController.getLatestInstance().markQuotaExhausted();
                } catch (OutdatedVersioningException e) {
                    ValNarratorApplication.showDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.WARNING_MESSAGE);
                    throw new RuntimeException(e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.warn("Narration queue unavailable, dropping message: {}", e.getMessage());
        }
    }

    public void updateQuota(final int remainingQuota) {
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().quotaLabel.setText(remainingQuota + "/" + properties.getQuotaLimit());
            ValNarratorController.getLatestInstance().quotaBar.setProgress((double) remainingQuota / properties.getQuotaLimit());
        });
    }

    public Chat getProperties() {
        return properties;
    }

    public boolean isPremium() {
        return APIHandler.isPremium();
    }
}
