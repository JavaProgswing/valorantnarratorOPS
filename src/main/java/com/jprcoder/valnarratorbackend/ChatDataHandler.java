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
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.expandShortForms;

public class ChatDataHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatDataHandler.class);
    private static final ChatDataHandler singleton;

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
        CompletableFuture.runAsync(() -> {
            try {
                VoiceGenerator.getInstance().speakVoice(expandShortForms(finalBody));
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
