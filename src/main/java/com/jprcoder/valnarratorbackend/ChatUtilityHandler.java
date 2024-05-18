package com.jprcoder.valnarratorbackend;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtilityHandler {
    public static String expandShortForms(String message) {
        message = message.replaceAll("(?i)\\bGGWP\\b", "Good game,,,well played!");
        message = message.replaceAll("(?i)\\bGG\\b", "Good game!");
        message = message.replaceAll("(?i)\\bWP\\b", "Well played!");
        message = message.replaceAll("(?i)\\bMB\\b", "My bad!");
        message = message.replaceAll("(?i)\\bEZ\\b", "Easy!");
        message = message.replaceAll("(?i)\\bNT\\b", "Nice Try!");
        message = message.replaceAll("(?i)\\bBTW\\b", "By the way");
        message = message.replaceAll("(?i)\\bBRB\\b", "Be right back");
        message = message.replaceAll("(?i)\\bNS\\b", "Nice Shot!");
        message = message.replaceAll("(?i)\\bFR\\b", "For real");
        message = message.replaceAll("(?i)\\bIC\\b", "I see");
        message = message.replaceAll("(?i)\\bNC\\b", "Nice");
        message = message.replaceAll("(?i)\\bIKR\\b", "I know right");
        message = message.replaceAll("(?i)\\bLOL\\b", "Laughing out loud");
        message = message.replaceAll("(?i)\\bASF\\b", "As fuck");
        message = message.replaceAll("(?i)\\bIG\\b", "I guess");
        message = message.replaceAll("(?i)\\bTPED\\b", "Teleported");
        message = message.replaceAll("(?i)\\bGH\\b", "Good Half");
        message = message.replaceAll("(?i)\\bHP\\b", "Health");
        message = message.replaceAll("(?i)\\bNVM\\b", "Nevermind");
        message = message.replaceAll("(?i)\\bDM\\b", "Deathmatch");
        message = message.replaceAll("(?i)\\bUNR\\b", "Unrated");
        message = message.replaceAll("(?i)\\bCOMP\\b", "Competitive");
        message = message.replaceAll("(?i)\\bGL\\b", "Good luck");
        message = message.replaceAll("(?i)\\bGJ\\b", "Good Job");
        message = message.replaceAll("(?i)\\bNJ\\b", "Nice Job");
        message = message.replaceAll("(?i)\\bGLHF\\b", "Good luck, have fun");
        message = message.replaceAll("(?i)\\bWDYM\\b", "What do you mean?");
        message = message.replaceAll("(?i)\\bNP\\b", "No problem");
        message = message.replaceAll("(?i)\\bSMH\\b", "Shake my head");
        message = message.replaceAll("(?i)\\bTY\\b", "Thank you!");
        message = message.replaceAll("(?i)\\bSRY\\b", "Sorry!");
        message = message.replaceAll("(?i)\\bPLS\\b", "Please");
        String pattern = "(\\d+)hp";
        String replacement = "$1 health";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(message);
        message = matcher.replaceAll(replacement);
        return message;
    }

    public static String getPlayerName(final String playerID) {
        final ArrayList<String> playerIDs = new ArrayList<>();
        playerIDs.add(playerID);
        final ArrayList<PlayerAccount> playerNames = ChatDataHandler.getInstance().getAPIHandler().getPlayerNames(VoiceGenerator.getAccessToken(), VoiceGenerator.getRiotClientDetails(), VoiceGenerator.getEntitlement().token(), playerIDs);
        return String.format("%s#%s", playerNames.get(0).GameName(), playerNames.get(0).TagLine());
    }
}
