/*
 * Dataverse Downloader
 *
 * Copyright (c) 2026 Service Center for Research Data Management,
 * University of Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Sergej Zerr
 * Organization: Service Center for Research Data Management, University of Bonn
 */
package de.unibn.hrz.dataverse.downloader.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unibn.hrz.dataverse.downloader.model.DownloadManifestEntry;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask.Status;

/**
 * Coordinates dataset file downloads, including resume support,
 * checksum verification, and manifest updates.
 *
 * <p>This service executes downloads asynchronously and updates download
 * task state for presentation in the user interface.</p>
 *
 * @author Sergej Zerr
 */
public class DownloadService {
    private static final Logger LOGGER = Logger.getLogger(DownloadService.class.getName());

    private static final long UI_UPDATE_INTERVAL_MILLIS = 200L;
    private static final long MANIFEST_UPDATE_INTERVAL_MILLIS = 2000L;

    private final DownloadManager downloadManager;
    private final ExecutorService executorService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ManifestService manifestService = new ManifestService();
    private final ChecksumVerifier checksumVerifier = new ChecksumVerifier();
    private final Object manifestLock = new Object();

    public DownloadService(DownloadManager downloadManager, int parallelDownloads) {
        this.downloadManager = downloadManager;
        int threads = Math.max(1, parallelDownloads);
        this.executorService = Executors.newFixedThreadPool(threads);
    }

    public void queueDownloads(List<DownloadTask> tasks) {
        for (DownloadTask task : tasks) {
            queueDownload(task);
        }
    }

    public void queueDownload(DownloadTask task) {
        if (downloadManager.hasActiveTaskFor(task)) {
            return;
        }

        task.clearPauseRequest();
        if (task.getStatus() != Status.PAUSED) {
            task.setStatus(Status.QUEUED);
            task.setMessage("Queued");
        } else {
            task.setStatus(Status.QUEUED);
            task.setMessage("Queued for resume");
        }

        if (!downloadManager.getTasks().contains(task)) {
            downloadManager.addTask(task);
        } else {
            downloadManager.notifyListeners(task);
        }

        executorService.submit(() -> download(task));
    }

    public void pauseDownload(DownloadTask task) {
        if (task.getStatus() == Status.RUNNING || task.getStatus() == Status.QUEUED) {
            task.requestPause();
            task.setMessage("Pausing...");
            downloadManager.notifyListeners(task);
        }
    }

    public void resumeDownload(DownloadTask task) {
        if (task.getStatus() == Status.PAUSED || task.getStatus() == Status.FAILED) {
            queueDownload(task);
        }
    }

    private void download(DownloadTask task) {
        Path finalFile = task.getDestination().resolve(task.getEntry().getRelativePath());
        Path partFile = finalFile.resolveSibling(finalFile.getFileName() + ".part");
        Path manifestPath = task.getDestination().resolve("download-manifest.json");

        try {
            Files.createDirectories(finalFile.getParent());

            long expectedSize = task.getEntry().getSize();

            if (isAlreadyComplete(finalFile, expectedSize)) {
                task.setProgressPercent(100);
                task.setStatus(Status.COMPLETED);
                task.setMessage("Already downloaded");
                safeUpdateManifest(manifestPath, task, expectedSize, "COMPLETED");
                downloadManager.notifyListeners(task);
                return;
            }

            long existingBytes = Files.exists(partFile) ? Files.size(partFile) : 0L;

            if (expectedSize > 0 && existingBytes > expectedSize) {
                Files.deleteIfExists(partFile);
                existingBytes = 0L;
            }

            if (expectedSize > 0 && existingBytes == expectedSize) {
                if (verifyFile(partFile, task)) {
                    Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                    task.setProgressPercent(100);
                    task.setStatus(Status.COMPLETED);
                    task.setMessage("Completed");
                    safeUpdateManifest(manifestPath, task, expectedSize, "COMPLETED");
                    downloadManager.notifyListeners(task);
                    return;
                } else {
                    Files.deleteIfExists(partFile);
                    existingBytes = 0L;
                }
            }

            task.setStatus(Status.RUNNING);
            task.setMessage(existingBytes > 0 ? "Resuming" : "Downloading");
            downloadManager.notifyListeners(task);
            safeUpdateManifest(manifestPath, task, existingBytes, "RUNNING");

            String url = task.getServerUrl().replaceAll("/$", "")
                    + "/api/access/datafile/" + task.getEntry().getFileId();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url)).GET();

            if (task.getApiKey() != null && !task.getApiKey().isBlank()) {
                requestBuilder.header("X-Dataverse-key", task.getApiKey());
            }

            boolean resumeRequested = existingBytes > 0 && (expectedSize == 0 || existingBytes < expectedSize);
            if (resumeRequested) {
                requestBuilder.header("Range", "bytes=" + existingBytes + "-");
            }

            HttpResponse<InputStream> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();

            if (statusCode == 416) {
                handle416(task, finalFile, partFile, manifestPath, expectedSize);
                return;
            }

            if (resumeRequested && statusCode != 206 && statusCode != 200) {
                throw new IOException("Resume failed. HTTP " + statusCode);
            }

            if (!resumeRequested && statusCode != 200) {
                throw new IOException("Download failed. HTTP " + statusCode);
            }

            long writeOffset = existingBytes;
            boolean append = true;

            if (resumeRequested && statusCode == 200) {
                writeOffset = 0L;
                append = false;
            }

            try (InputStream in = response.body();
                 RandomAccessFile raf = new RandomAccessFile(partFile.toFile(), "rw")) {

                if (!append) {
                    raf.setLength(0L);
                }

                raf.seek(writeOffset);

                byte[] buffer = new byte[8192];
                int read;
                long downloaded = writeOffset;
                long lastUiUpdateAt = 0L;
                long lastManifestUpdateAt = 0L;

                while ((read = in.read(buffer)) != -1) {
                    if (task.isPauseRequested()) {
                        task.setStatus(Status.PAUSED);
                        task.setMessage("Paused");
                        safeUpdateManifest(manifestPath, task, downloaded, "PAUSED");
                        downloadManager.notifyListeners(task);
                        return;
                    }

                    raf.write(buffer, 0, read);
                    downloaded += read;

                    int progress = expectedSize <= 0
                            ? 0
                            : (int) Math.min(100L, (downloaded * 100L) / expectedSize);

                    task.setProgressPercent(progress);
                    task.setMessage("Downloading " + downloaded + " / " + expectedSize + " bytes");

                    long now = System.currentTimeMillis();

                    if (now - lastUiUpdateAt >= UI_UPDATE_INTERVAL_MILLIS) {
                        downloadManager.notifyListeners(task);
                        lastUiUpdateAt = now;
                    }

                    if (now - lastManifestUpdateAt >= MANIFEST_UPDATE_INTERVAL_MILLIS) {
                        safeUpdateManifest(manifestPath, task, downloaded, "RUNNING");
                        lastManifestUpdateAt = now;
                    }
                }

                safeUpdateManifest(manifestPath, task, downloaded, "RUNNING");
                downloadManager.notifyListeners(task);
            }

            if (!verifyFile(partFile, task)) {
                throw new IOException("Checksum verification failed for " + task.getEntry().getName());
            }

            Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            task.setProgressPercent(100);
            task.setStatus(Status.COMPLETED);
            task.setMessage("Completed");
            safeUpdateManifest(manifestPath, task, expectedSize, "COMPLETED");
            downloadManager.notifyListeners(task);

        } catch (Exception e) {
            if (task.getStatus() != Status.PAUSED) {
                task.setStatus(Status.FAILED);
                task.setMessage(e.getMessage());
                safeUpdateManifest(manifestPath, task, safeDownloadedBytes(partFile), "FAILED");
                downloadManager.notifyListeners(task);
            }
        } finally {
            task.clearPauseRequest();
        }
    }

    private void handle416(
            DownloadTask task,
            Path finalFile,
            Path partFile,
            Path manifestPath,
            long expectedSize) throws IOException {

        long currentPartSize = Files.exists(partFile) ? Files.size(partFile) : 0L;

        if (expectedSize > 0 && currentPartSize == expectedSize && verifyFile(partFile, task)) {
            Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            task.setProgressPercent(100);
            task.setStatus(Status.COMPLETED);
            task.setMessage("Completed");
            safeUpdateManifest(manifestPath, task, expectedSize, "COMPLETED");
            downloadManager.notifyListeners(task);
            return;
        }

        Files.deleteIfExists(partFile);
        safeUpdateManifest(manifestPath, task, 0L, "FAILED");
        throw new IOException("Server rejected resume request (HTTP 416). Partial file was reset; retry download.");
    }

    private boolean isAlreadyComplete(Path finalFile, long expectedSize) throws IOException {
        if (!Files.exists(finalFile)) {
            return false;
        }
        if (expectedSize <= 0) {
            return true;
        }
        return Files.size(finalFile) == expectedSize;
    }

    private boolean verifyFile(Path file, DownloadTask task) throws IOException {
        String algorithm = task.getEntry().getChecksumAlgorithm();
        String checksum = task.getEntry().getChecksumValue();

        if (algorithm == null || algorithm.isBlank() || checksum == null || checksum.isBlank()) {
            return true;
        }

        return checksumVerifier.verify(file, algorithm, checksum);
    }

    private long safeDownloadedBytes(Path file) {
        try {
            return Files.exists(file) ? Files.size(file) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    private void safeUpdateManifest(Path manifestPath, DownloadTask task, long bytesDownloaded, String status) {
        try {
            updateManifest(manifestPath, task, bytesDownloaded, status);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Manifest update failed for " + task.getEntry().getRelativePath(),
                    e);
        }
    }

    private void updateManifest(Path manifestPath, DownloadTask task, long bytesDownloaded, String status) {
        synchronized (manifestLock) {
            List<DownloadManifestEntry> entries = new ArrayList<>(manifestService.readStrict(manifestPath));
            entries.removeIf(entry -> entry.getFileId() == task.getEntry().getFileId());

            DownloadManifestEntry entry = new DownloadManifestEntry();
            entry.setFileId(task.getEntry().getFileId());
            entry.setRelativePath(task.getEntry().getRelativePath());
            entry.setBytesDownloaded(bytesDownloaded);
            entry.setChecksumAlgorithm(task.getEntry().getChecksumAlgorithm());
            entry.setChecksumValue(task.getEntry().getChecksumValue());
            entry.setStatus(status);

            entries.add(entry);
            manifestService.write(manifestPath, entries);
        }
    }
}