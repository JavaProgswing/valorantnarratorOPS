package com.jprcoder.valnarratorbackend;

import java.util.ArrayList;
import java.util.Hashtable;

public class Chat {
    private final int quotaLimit;
    private final Hashtable<String, String> playerIDs = new Hashtable<>();
    private final Hashtable<String, String> playerNames = new Hashtable<>();
    private final ArrayList<String> ignoredPlayerIDs = new ArrayList<>();
    private long messagesSent, charactersSent;
    private boolean selfState = true, privateState, partyState, teamState, allState, isDisabled;
    private boolean isQuotaExhausted;
    private String selfID;

    public Chat(int quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public void markQuotaExhausted() {
        isQuotaExhausted = true;
    }

    public boolean isQuotaExhausted() {
        return isQuotaExhausted;
    }

    public void addIgnoredPlayer(final String player) {
        ignoredPlayerIDs.add(playerNames.get(player));
    }

    public void removeIgnoredPlayer(final String player) {
        ignoredPlayerIDs.remove(playerNames.get(player));
    }

    public boolean isIgnoredPlayerID(final String playerID) {
        return ignoredPlayerIDs.contains(playerID);
    }

    public Hashtable<String, String> getPlayerIDTable() {
        return playerIDs;
    }

    public Hashtable<String, String> getPlayerNameTable() {
        return playerNames;
    }

    public int getQuotaLimit() {
        return quotaLimit;
    }

    public void updateMessageStats(Message message) {
        messagesSent++;
        charactersSent += message.getContent().length();
    }

    public long getMessagesSent() {
        return messagesSent;
    }

    public long getCharactersSent() {
        return charactersSent;
    }

    public String getSelfID() {
        return selfID;
    }

    public void setSelfID(String selfID) {
        this.selfID = selfID;
    }

    public boolean toggleState() {
        return isDisabled = !isDisabled;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public boolean isSelfState() {
        return selfState;
    }

    public boolean isPrivateState() {
        return privateState;
    }

    public boolean isPartyState() {
        return partyState;
    }

    public boolean isTeamState() {
        return teamState;
    }

    public boolean isAllState() {
        return allState;
    }

    public void setPrivateEnabled() {
        privateState = true;
    }

    public void setPrivateDisabled() {
        privateState = false;
    }

    public void setSelf(boolean state) {
        selfState = state;
    }

    public void setParty(boolean state) {
        partyState = state;
    }

    public void setTeam(boolean state) {
        teamState = state;
    }

    public void setAll(boolean state) {
        allState = state;
    }

}
