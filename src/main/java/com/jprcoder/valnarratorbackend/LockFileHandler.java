package com.jprcoder.valnarratorbackend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LockFileHandler {

    private final String name, pid, protocol, password;

    private final int port;

    public LockFileHandler() throws IOException {
        Path path = Paths.get(System.getenv("LocalAppData"), "Riot Games", "Riot Client", "Config", "lockfile");
        String lockData = Files.readString(path, StandardCharsets.UTF_8);
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