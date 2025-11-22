package com.jprcoder.valnarratorgui;

import javafx.application.Platform;

public class FXInit {
    public static synchronized void init() {
        try {
            Platform.startup(() -> {
            });
            Platform.setImplicitExit(false);
        } catch (IllegalStateException ignored) {
        }
    }
}
