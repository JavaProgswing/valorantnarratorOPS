package com.jprcoder.valnarratorbackend;

import com.jprcoder.valnarratorgui.Main;
import com.jprcoder.valnarratorgui.ValNarratorApplication;
import com.jprcoder.valnarratorgui.ValNarratorController;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.expandShortForms;
import static com.jprcoder.valnarratorbackend.ChatUtilityHandler.getPlayerName;

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
            ValNarratorApplication.showPreStartupDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
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
            ValNarratorApplication.showPreStartupDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
            throw new RuntimeException(e);
        }
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
        }

        properties.updateMessageStats(message);
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().setMessagesSent(properties.getMessagesSent());
            ValNarratorController.getLatestInstance().setCharactersNarrated(properties.getCharactersSent());
        });
        final String finalBody = message.getContent().replace("/", "").replace("\\", "");
        CompletableFuture.runAsync(() -> {
            try {
                VoiceGenerator.getInstance().speakVoice(expandShortForms(finalBody));
            } catch (IOException e) {
                logger.warn(String.format("Failed to narrate message: %s", e.getMessage()));
            } catch (QuotaExhaustedException e) {
                logger.warn(String.format("Quota exhausted, %s", e.getMessage()));
                ValNarratorController.getLatestInstance().markQuotaExhausted();
            } catch (OutdatedVersioningException e) {
                ValNarratorApplication.showPreStartupDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
                throw new RuntimeException(e);
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
