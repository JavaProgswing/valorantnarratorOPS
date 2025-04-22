package com.jprcoder.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {
    private static final Logger logger = LoggerFactory.getLogger(Message.class);
    private final String content;
    private final String id;
    private final String userId;
    private final boolean isOwnMessage;
    private final MessageType messageType;

    public Message(String xml) {
        logger.debug(xml);
        Pattern typePattern = Pattern.compile("type='(.*?)'");
        Pattern bodyPattern = Pattern.compile("<body>(.*?)</body>");
        Pattern jidPattern = Pattern.compile("jid='(.*?)'");
        Pattern fromPattern = Pattern.compile("from='(.*?)'");

        Matcher typeMatcher = typePattern.matcher(xml);
        Matcher bodyMatcher = bodyPattern.matcher(xml);
        Matcher jidMatcher = jidPattern.matcher(xml);
        Matcher fromMatcher = fromPattern.matcher(xml);

        id = jidMatcher.find() ? jidMatcher.group(1) : fromMatcher.find() ? fromMatcher.group(1) : "@";
        String type = typeMatcher.find() ? typeMatcher.group(1) : null;
        fromMatcher = fromPattern.matcher(xml);
        String from = fromMatcher.find() ? fromMatcher.group(1) : null;
        messageType = getMessageType(Objects.requireNonNull(from), type);
        content = bodyMatcher.find() ? HtmlEscape.unescapeHtml(bodyMatcher.group(1)) : null;
        userId = id.split("@")[0];

        isOwnMessage = ChatDataHandler.getInstance().getProperties().getSelfID().equals(userId);
    }

    private static MessageType getMessageType(String fromTag, String type) {
        String[] splitTag = fromTag.split("@");
        String serverType = splitTag[1].split("\\.")[0];
        String id = splitTag[0];
        switch (serverType) {
            case "ares-parties":
                return MessageType.PARTY;
            case "ares-pregame":
                return MessageType.TEAM;
            case "ares-coregame":
                if (id.endsWith("all")) return MessageType.ALL;
                else return MessageType.TEAM;
            default:
                if (type.equals("chat")) return MessageType.WHISPER;
                else return null;
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public boolean isOwnMessage() {
        return isOwnMessage;
    }

    public String getUserId() {
        return userId;
    }

    public String toString() {
        return String.format("(%s)%s: %s", messageType, id, content);
    }

}

