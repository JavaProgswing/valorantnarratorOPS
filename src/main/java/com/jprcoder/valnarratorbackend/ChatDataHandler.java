package com.jprcoder.valnarratorbackend;

import com.jprcoder.valnarratorgui.Main;
import com.jprcoder.valnarratorgui.ValNarratorController;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.expandShortForms;
import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.getPlayerName;

public class ChatDataHandler {
    public static final Logger logger = LoggerFactory.getLogger(ChatDataHandler.class);
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
        properties = new Chat(APIHandler.getQuotaLimit());
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

        MessageQuota mq = APIHandler.getRequestQuota();
        ValNarratorController.getLatestInstance().updateRequestQuota(mq);
        APIHandler.setPremium(mq.isPremium());
    }

    public void message(Message message) {
        if (properties.isDisabled()) {
            logger.info("Valorant Narrator disabled, ignoring message!");
            return;
        }
        if (properties.isIgnoredPlayerID(message.getUserId())) {
            logger.info(String.format("Ignoring message from %s!", properties.getPlayerIDTable().get(message.getUserId())));
            return;
        }

        if (message.getMessageType() == MessageType.WHISPER && !properties.isPrivateState()) {
            logger.info("Private messages disabled, ignoring message!");
            return;
        }

        if (message.isOwnMessage() && properties.isSelfState()) {
            logger.info("Self messages enabled, processing message!");
        } else if (message.getMessageType() == MessageType.PARTY && !properties.isPartyState()) {
            logger.info("Party messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.TEAM && !properties.isTeamState()) {
            logger.info("Team messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.ALL && !properties.isAllState()) {
            logger.info("All messages disabled, ignoring message!");
            return;
        }
        if (!properties.getPlayerIDTable().containsKey(message.getUserId())) {
            final String playerID = message.getUserId(), playerName = getPlayerName(playerID).trim();
            properties.getPlayerIDTable().put(playerID, playerName);
            properties.getPlayerNameTable().put(playerName, playerID);
            ValNarratorController.getLatestInstance().addIgnoredPlayer.getItems().addAll(playerName);
            logger.debug(String.format("Added %s: %s to HASH-TABLE!", playerID, playerName));
        }

        properties.updateMessageStats(message);
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().setMessagesSent(properties.getMessagesSent());
            ValNarratorController.getLatestInstance().setCharactersNarrated(properties.getCharactersSent());
        });
        final String finalBody = message.getContent();
        CompletableFuture.runAsync(() -> {
            try {
                VoiceGenerator.getInstance().speakVoice(expandShortForms(finalBody));
            } catch (IOException e) {
                logger.warn(String.format("Failed to narrate message: %s", e.getMessage()));
            } catch (QuotaExhaustedException e) {
                logger.warn(String.format("Quota exhausted, %s", e.getMessage()));
                Platform.runLater(() -> {
                    ValNarratorController.getLatestInstance().quotaLabel.setText(String.format("Quota Exhausted, %s!", e.getMessage()));
                    ValNarratorController.getLatestInstance().quotaLabel.setTextFill(Color.RED);
                    ValNarratorController.getLatestInstance().quotaBar.setProgress(0.0);
                });
            }
        });
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
