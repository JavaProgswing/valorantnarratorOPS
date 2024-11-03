package com.jprcoder.valnarratorbackend;

import com.jprcoder.valnarratorgui.ValNarratorApplication;
import com.jprcoder.valnarratorgui.ValNarratorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class VoiceTokenHandler {
    private static final Logger logger = LoggerFactory.getLogger(VoiceTokenHandler.class);
    private static final int TOKEN_EXPIRATION_TIME = 895;
    private static long LAST_REFRESH_MS = 0;
    private final APIHandler apiHandler;

    public VoiceTokenHandler(APIHandler apiHandler) {
        this.apiHandler = apiHandler;
    }

    public static long getLAST_REFRESH_MS() {
        return LAST_REFRESH_MS;
    }

    public void startRefreshToken() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    apiHandler.addRequestQuota();
                } catch (IOException e) {
                    logger.warn(String.format("Failed to refresh token: %s", e.getMessage()));
                } catch (QuotaExhaustedException e) {
                    logger.warn(String.format("Quota exhausted, %s", e.getMessage()));
                    ValNarratorController.getLatestInstance().markQuotaExhausted();
                    try {
                        Thread.sleep(TOKEN_EXPIRATION_TIME * 1000);
                    } catch (InterruptedException e1) {
                        logger.warn(String.format("Failed to sleep: %s", e.getMessage()));
                    }
                    continue;
                } catch (OutdatedVersioningException e) {
                    ValNarratorApplication.showPreStartupDialog("Version Outdated", "Please update to the latest ValNarrator update to resume app functioning.", com.jprcoder.valnarratorgui.MessageType.fromInt(JOptionPane.WARNING_MESSAGE));
                    throw new RuntimeException(e);
                }
                LAST_REFRESH_MS = System.currentTimeMillis();
                logger.info("Token has been refreshed!");
                try {
                    Thread.sleep(TOKEN_EXPIRATION_TIME * 1000);
                } catch (InterruptedException e) {
                    logger.warn(String.format("Failed to sleep: %s", e.getMessage()));
                }
            }
        });
    }
}
