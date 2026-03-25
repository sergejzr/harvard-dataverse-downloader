package com.example.dataverse.downloader.model;

import java.util.ArrayList;
import java.util.List;

public class DatasetInfo {
    private long datasetId;
    private String title;
    private String persistentId;
    private String serverUrl;
    private List<DatasetFileEntry> files = new ArrayList<>();

    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public List<DatasetFileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<DatasetFileEntry> files) {
        this.files = files;
    }
}