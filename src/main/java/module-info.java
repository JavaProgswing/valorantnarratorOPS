module ValorantNarrator {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires transitive com.google.gson;
    requires transitive com.jfoenix;
    requires dev.mccue.jlayer;
    requires java.datatransfer;
    requires java.desktop;
    requires transitive java.net.http;
    requires transitive javafx.base;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires java.naming; // Missing logback dependency
    requires jdk.crypto.ec;

    exports com.jprcoder.valnarratorgui to javafx.graphics, javafx.fxml;
    exports com.jprcoder.valnarratorbackend;

    opens com.jprcoder.valnarratorgui to javafx.fxml;
    opens com.jprcoder.valnarratorbackend to com.google.gson;
}