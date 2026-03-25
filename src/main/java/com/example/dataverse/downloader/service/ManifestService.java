package com.example.dataverse.downloader.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.dataverse.downloader.model.DownloadManifestEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ManifestService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<DownloadManifestEntry> read(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(manifestPath)) {
            DownloadManifestEntry[] entries = gson.fromJson(reader, DownloadManifestEntry[].class);
            return entries == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(entries));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read manifest: " + manifestPath, e);
        }
    }

    public void write(Path manifestPath, List<DownloadManifestEntry> entries) {
        try {
            Files.createDirectories(manifestPath.getParent());
            try (Writer writer = Files.newBufferedWriter(manifestPath)) {
                gson.toJson(entries, writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write manifest: " + manifestPath, e);
        }
    }
}
