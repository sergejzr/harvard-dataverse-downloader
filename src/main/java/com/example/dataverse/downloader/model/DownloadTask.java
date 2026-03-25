package com.example.dataverse.downloader.model;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadTask {
    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED, PAUSED }

    private final DatasetFileEntry entry;
    private final String datasetPid;
    private final String serverUrl;
    private final Path destination;
    private volatile Status status = Status.QUEUED;
    private volatile String message = "Queued";
    private final AtomicInteger progressPercent = new AtomicInteger();
    private final String apiKey;

    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);

    public DownloadTask(DatasetFileEntry entry, String datasetPid, String serverUrl, String apiKey, Path destination) {
        this.entry = entry;
        this.datasetPid = datasetPid;
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.destination = destination;
    }

    public String getApiKey() { return apiKey; }
    public DatasetFileEntry getEntry() { return entry; }
    public String getDatasetPid() { return datasetPid; }
    public String getServerUrl() { return serverUrl; }
    public Path getDestination() { return destination; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getProgressPercent() { return progressPercent.get(); }
    public void setProgressPercent(int progressPercent) { this.progressPercent.set(progressPercent); }

    public void requestPause() {
        pauseRequested.set(true);
    }

    public void clearPauseRequest() {
        pauseRequested.set(false);
    }

    public boolean isPauseRequested() {
        return pauseRequested.get();
    }

    public boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }
}