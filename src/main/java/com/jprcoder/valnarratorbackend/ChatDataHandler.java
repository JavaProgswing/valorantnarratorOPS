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

record AccountJsonParser(UserInfo userInfo) {
}

record UserInfo(ValAccount acct) {

}

public class ChatDataHandler {
    public static final Logger logger = LoggerFactory.getLogger(ChatDataHandler.class);
    private static final ChatDataHandler singleton;

    private final Chat properties;

    private final LockFileHandler lockFileHandler;

    private final APIHandler APIHandler;

    static {
        try {
            singleton = new ChatDataHandler();
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ChatDataHandler() throws NoSuchAlgorithmException, KeyManagementException, IOException {
        ConnectionHandler connectionHandler = new ConnectionHandler();
        APIHandler = new APIHandler(connectionHandler);
        properties = new Chat(APIHandler.getQuotaLimit());
        lockFileHandler = new LockFileHandler();
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
        ValAccount account = APIHandler.getLocalAccount(lockFileHandler);
        logger.info(String.format("Welcome %s#%s", account.game_name(), account.tag_line()));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().setAccountLabel(String.format("%s#%s", account.game_name(), account.tag_line())));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().setUserIDLabel(Main.serialNumber));

        MessageQuota mq = APIHandler.getRequestQuota();
        ValNarratorController.getLatestInstance().updateRequestQuota(mq);
        APIHandler.setPremium(mq.isPremium());
    }

    public void message(Message message) {
        if (properties.getState()) {
            logger.info("Valorant Narrator disabled, ignoring message!");
            return;
        }
        if (message.getMessageType() == MessageType.PARTY && !properties.isPartyState()) {
            if (!(message.isOwnMessage() && properties.isSelfState())) {
                logger.info("Party/self messages disabled, ignoring message!");
                return;
            }
        } else if (message.getMessageType() == MessageType.TEAM && !properties.isTeamState()) {
            logger.info("Team messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.WHISPER && !properties.isPrivateState()) {
            logger.info("Private messages disabled, ignoring message!");
            return;
        } else if (message.getMessageType() == MessageType.ALL && !properties.isAllState()) {
            logger.info("All messages disabled, ignoring message!");
            return;
        }

        properties.updateMessageStats(message);
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().setMessagesSent(properties.getMessagesSent());
            ValNarratorController.getLatestInstance().setCharactersNarrated(properties.getCharactersSent());
        });
        final String finalBody = message.getContent();
        try {
            MessageQuota mq = null;
            boolean keyNotPresent = false;
            if (System.getProperty("aws.accessKeyId") == null || System.getProperty("aws.secretKey") == null || System.getProperty("aws.sessionToken") == null) {
                mq = APIHandler.addRequestQuota();
                keyNotPresent = true;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    VoiceGenerator.getInstance().speakVoice(expandShortForms(finalBody), System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey"), System.getProperty("aws.sessionToken"));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            if (!keyNotPresent) mq = APIHandler.addRequestQuota();
            if (mq.isPremium()) {
                Platform.runLater(() -> {
                    ValNarratorController.getLatestInstance().windowTitle.setVisible(false);
                    ValNarratorController.getLatestInstance().premiumWindowTitle.setVisible(true);
                    ValNarratorController.getLatestInstance().subscribeButton.setVisible(false);
                });
            } else {
                if (mq.remainingQuota() == 0) {
                    Platform.runLater(() -> {
                        ValNarratorController.getLatestInstance().quotaLabel.setText("Quota Exhausted!");
                        ValNarratorController.getLatestInstance().quotaLabel.setTextFill(Color.RED);
                        ValNarratorController.getLatestInstance().quotaBar.setProgress(0.0);
                    });
                } else {
                    int quota = mq.remainingQuota();
                    Platform.runLater(() -> {
                        ValNarratorController.getLatestInstance().quotaLabel.setText(quota + "/" + properties.getQuotaLimit());
                        ValNarratorController.getLatestInstance().quotaBar.setProgress((double) quota / properties.getQuotaLimit());
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chat getProperties() {
        return properties;
    }

    public boolean isPremium() {
        return APIHandler.isPremium();
    }
}
