package com.jprcoder.valnarratorbackend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RiotUtilityHandler {
    public static boolean isValorantRunning() {
        String findProcess = "VALORANT.exe";
        String filenameFilter = "/nh /fi \"Imagename eq " + findProcess + "\"";
        String tasksCmd = System.getenv("windir") + "/system32/tasklist.exe " + filenameFilter;
        Process p;
        ArrayList<String> procs;
        try {
            p = Runtime.getRuntime().exec(tasksCmd);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            procs = new ArrayList<>();
            String line;
            while ((line = input.readLine()) != null) procs.add(line);

            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return procs.stream().anyMatch(row -> row.contains(findProcess));
    }
}
