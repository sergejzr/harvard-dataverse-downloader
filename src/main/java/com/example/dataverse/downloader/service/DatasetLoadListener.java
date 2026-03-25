package com.example.dataverse.downloader.service;

public interface DatasetLoadListener {
    void onStatus(String message);

    void onProgress(int percent);

    default void onFileCountDiscovered(int totalFiles) {
    }

    default void onFilesLoaded(int loadedFiles, int totalFiles) {
    }
}