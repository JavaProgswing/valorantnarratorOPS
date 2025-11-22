package com.jprcoder.valnarratorbackend;

@FunctionalInterface
public interface ProgressCallback {
    void onProgress(double percent, long bytesDownloaded, long totalBytes);
}
