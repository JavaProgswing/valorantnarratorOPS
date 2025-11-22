package com.jprcoder.valnarratorbackend;

public record VersionInfo(long timestamp, double version, double agent_version, String changes) {
}