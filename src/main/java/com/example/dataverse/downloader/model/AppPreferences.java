package com.example.dataverse.downloader.model;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPreferences {
    private String serverUrl = "https://dataverse.harvard.edu";
    private String apiKey = "";
    private Path outputFolder = Paths.get(System.getProperty("user.home"), "Downloads", "Dataverse");
    private int parallelDownloads = 3;
    private String overwritePolicy = "SKIP";
    private int datasetUrlHistorySize = 10;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Path getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    public int getParallelDownloads() {
        return parallelDownloads;
    }

    public void setParallelDownloads(int parallelDownloads) {
        this.parallelDownloads = parallelDownloads;
    }

    public String getOverwritePolicy() {
        return overwritePolicy;
    }

    public void setOverwritePolicy(String overwritePolicy) {
        this.overwritePolicy = overwritePolicy;
    }

    public int getDatasetUrlHistorySize() {
        return datasetUrlHistorySize;
    }

    public void setDatasetUrlHistorySize(int datasetUrlHistorySize) {
        this.datasetUrlHistorySize = datasetUrlHistorySize;
    }
}