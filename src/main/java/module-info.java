module ValorantNarrator {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.google.gson;
    requires com.jfoenix;
    requires dev.mccue.jlayer;
    requires java.datatransfer;
    requires java.desktop;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires java.naming; // Missing logback dependency
    requires jdk.crypto.ec;

    exports com.jprcoder.valnarratorgui to javafx.graphics, javafx.fxml;
    opens com.jprcoder.valnarratorgui to javafx.fxml;
    opens com.jprcoder.valnarratorbackend to com.google.gson;
}