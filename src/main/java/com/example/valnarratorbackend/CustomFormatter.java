package com.example.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.DefaultLoggingEventBuilder;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

@SuppressWarnings("ALL")
public class CustomFormatter {
    public static Logger Logger = null;
    public final Logger logger;

    public CustomFormatter(Class<?> cls) {
        logger = LoggerFactory.getLogger(cls);
        Logger = LoggerFactory.getLogger(CustomFormatter.class);
    }

    public String getName() {
        return logger.getName();
    }

    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return new DefaultLoggingEventBuilder(logger, level);
    }

    public LoggingEventBuilder atLevel(Level level) {
        return this.isEnabledForLevel(level) ? this.makeLoggingEventBuilder(level) : NOPLoggingEventBuilder.singleton();
    }

    public boolean isEnabledForLevel(Level level) {
        int levelInt = level.toInt();
        return switch (levelInt) {
            case 0 -> this.isTraceEnabled();
            case 10 -> this.isDebugEnabled();
            case 20 -> this.isInfoEnabled();
            case 30 -> this.isWarnEnabled();
            case 40 -> this.isErrorEnabled();
            default -> throw new IllegalArgumentException("Level [" + level + "] not recognized.");
        };
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public void trace(String var1) {
        logger.trace(var1);
    }

    public void trace(String var1, Object var2) {
        logger.trace(var1, var2);
    }

    public void trace(String var1, Object var2, Object var3) {
        logger.trace(var1, var2, var3);
    }

    public void trace(String var1, Object... var2) {
        logger.trace(var1, var2);
    }

    public void trace(String var1, Throwable var2) {
        logger.trace(var1, var2);
    }

    public boolean isTraceEnabled(Marker var1) {
        return logger.isTraceEnabled(var1);
    }

    public LoggingEventBuilder atTrace() {
        return this.isTraceEnabled() ? this.makeLoggingEventBuilder(Level.TRACE) : NOPLoggingEventBuilder.singleton();
    }

    public void trace(Marker var1, String var2) {
        logger.trace(var1, var2);
    }

    public void trace(Marker var1, String var2, Object var3) {
        logger.trace(var1, var2, var3);
    }

    public void trace(Marker var1, String var2, Object var3, Object var4) {
        logger.trace(var1, var2, var3, var4);
    }

    public void trace(Marker var1, String var2, Object... var3) {
        logger.trace(var1, var2, var3);
    }

    public void trace(Marker var1, String var2, Throwable var3) {
        logger.trace(var1, var2, var3);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(String var1) {
        logger.debug(var1);
    }

    public void debug(String var1, Object var2) {
        logger.debug(var1, var2);
    }

    public void debug(String var1, Object var2, Object var3) {
        logger.debug(var1, var2, var3);
    }

    public void debug(String var1, Object... var2) {
        logger.debug(var1, var2);
    }

    public void debug(String var1, Throwable var2) {
        logger.debug(var1, var2);
    }

    public boolean isDebugEnabled(Marker var1) {
        return logger.isDebugEnabled(var1);
    }

    public void debug(Marker var1, String var2) {
        logger.debug(var1, var2);
    }

    public void debug(Marker var1, String var2, Object var3) {
        logger.debug(var1, var2, var3);
    }

    public void debug(Marker var1, String var2, Object var3, Object var4) {
        logger.debug(var1, var2, var3, var4);
    }

    public void debug(Marker var1, String var2, Object... var3) {
        logger.debug(var1, var2, var3);
    }

    public void debug(Marker var1, String var2, Throwable var3) {
        logger.debug(var1, var2, var3);
    }

    public LoggingEventBuilder atDebug() {
        return this.isDebugEnabled() ? this.makeLoggingEventBuilder(Level.DEBUG) : NOPLoggingEventBuilder.singleton();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public void info(String var1) {
        logger.info(var1);
    }

    public void info(String var1, Object var2) {
        logger.info(var1, var2);
    }

    public void info(String var1, Object var2, Object var3) {
        logger.info(var1, var2, var3);
    }

    public void info(String var1, Object... var2) {
        logger.info(var1, var2);
    }

    public void info(String var1, Throwable var2) {
        logger.info(var1, var2);
    }

    public boolean isInfoEnabled(Marker var1) {
        return logger.isInfoEnabled(var1);
    }

    public void info(Marker var1, String var2) {
        logger.info(var1, var2);
    }

    public void info(Marker var1, String var2, Object var3) {
        logger.info(var1, var2, var3);
    }

    public void info(Marker var1, String var2, Object var3, Object var4) {
        logger.info(var1, var2, var3, var4);
    }

    public void info(Marker var1, String var2, Object... var3) {
        logger.info(var1, var2, var3);
    }

    public void info(Marker var1, String var2, Throwable var3) {
        logger.info(var1, var2, var3);
    }

    public LoggingEventBuilder atInfo() {
        return this.isInfoEnabled() ? this.makeLoggingEventBuilder(Level.INFO) : NOPLoggingEventBuilder.singleton();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void warn(String var1) {
        logger.warn(var1);
    }

    public void warn(String var1, Object var2) {
        logger.warn(var1, var2);
    }

    public void warn(String var1, Object... var2) {
        logger.warn(var1, var2);
    }

    public void warn(String var1, Object var2, Object var3) {
        logger.warn(var1, var2, var3);
    }

    public void warn(String var1, Throwable var2) {
        logger.warn(var1, var2);
    }

    public boolean isWarnEnabled(Marker var1) {
        return logger.isWarnEnabled(var1);
    }

    public void warn(Marker var1, String var2) {
        logger.warn(var1, var2);
    }

    public void warn(Marker var1, String var2, Object var3) {
        logger.warn(var1, var2, var3);
    }

    public void warn(Marker var1, String var2, Object var3, Object var4) {
        logger.warn(var1, var2, var3, var4);
    }

    public void warn(Marker var1, String var2, Object... var3) {
        logger.warn(var1, var2, var3);
    }

    public void warn(Marker var1, String var2, Throwable var3) {
        logger.warn(var1, var2, var3);
    }

    public LoggingEventBuilder atWarn() {
        return this.isWarnEnabled() ? this.makeLoggingEventBuilder(Level.WARN) : NOPLoggingEventBuilder.singleton();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public void error(String var1) {
        logger.error(var1);
    }

    public void error(Exception e) {
        logger.error(String.valueOf(e));
    }

    public void error(String var1, Object var2) {
        logger.error(var1, var2);
    }

    public void error(String var1, Object var2, Object var3) {
        logger.error(var1, var2, var3);
    }

    public void error(String var1, Object... var2) {
        logger.error(var1, var2);
    }

    public void error(String var1, Throwable var2) {
        logger.error(var1, var2);
    }

    public boolean isErrorEnabled(Marker var1) {
        return logger.isErrorEnabled(var1);
    }

    public void error(Marker var1, String var2) {
        logger.error(var1, var2);
    }

    public void error(Marker var1, String var2, Object var3) {
        logger.error(var1, var2, var3);
    }

    public void error(Marker var1, String var2, Object var3, Object var4) {
        logger.error(var1, var2, var3, var4);
    }

    public void error(Marker var1, String var2, Object... var3) {
        logger.error(var1, var2, var3);
    }

    public void error(Marker var1, String var2, Throwable var3) {
        logger.error(var1, var2, var3);
    }

    public LoggingEventBuilder atError() {
        return this.isErrorEnabled() ? this.makeLoggingEventBuilder(Level.ERROR) : NOPLoggingEventBuilder.singleton();
    }
}
