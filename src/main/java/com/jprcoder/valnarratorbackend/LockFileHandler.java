package com.jprcoder.valnarratorbackend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LockFileHandler {

    private static final Path LOCKFILE_PATH = Paths.get(
            System.getenv("LocalAppData"), "Riot Games", "Riot Client", "Config", "lockfile");

    private String name, pid, protocol, password;

    private int port;

    public LockFileHandler() throws IOException {
        readLockfile();
    }

    /**
     * Check whether the lockfile exists on disk (Riot Client is running).
     */
    public static boolean exists() {
        return Files.exists(LOCKFILE_PATH);
    }

    /**
     * Best-effort staleness check: true if the lockfile is missing, unreadable, or its PID no
     * longer corresponds to a running process. Riot Client doesn't always clean up this file on a
     * crash/force-kill, and without this check a stale file looks identical to a live one - the
     * caller would poll a dead port for its full timeout instead of noticing nothing is listening.
     */
    public static boolean isStale() {
        if (!exists()) return true;
        try {
            String lockData = Files.readString(LOCKFILE_PATH, StandardCharsets.UTF_8);
            String[] data = lockData.split(":");
            if (data.length < 5) return true;
            return ProcessHandle.of(Long.parseLong(data[1])).isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Deletes the on-disk lockfile if {@link #isStale()}. A live lockfile is left untouched. Clears
     * the way for a freshly-launched Riot Client to write a valid one instead of callers trusting
     * dead connection info.
     */
    public static void deleteStale() {
        if (!isStale()) return;
        try {
            Files.deleteIfExists(LOCKFILE_PATH);
        } catch (IOException ignored) {
            // Best-effort; a subsequent read will just fail the same way it would have anyway.
        }
    }

    /**
     * Re-read the lockfile. Port and password rotate on every Riot Client restart.
     */
    public void refresh() throws IOException {
        readLockfile();
    }

    private void readLockfile() throws IOException {
        String lockData = Files.readString(LOCKFILE_PATH, StandardCharsets.UTF_8);
        String[] data = lockData.split(":");
        if (data.length < 5) throw new IOException("Invalid lockfile data!");

        name = data[0];
        pid = data[1];
        port = Integer.parseInt(data[2]);
        password = data[3];
        protocol = data[4];
    }

    public String getName() {
        return name;
    }

    public String getPid() {
        return pid;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPassword() {
        return password;
    }
}