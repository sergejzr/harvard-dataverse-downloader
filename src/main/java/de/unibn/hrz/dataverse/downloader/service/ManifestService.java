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
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import de.unibn.hrz.dataverse.downloader.model.DownloadManifestEntry;

/**
 * Provides application service logic for interacting with the download
 * manifest used for resume support and UI state.
 *
 * <p>This class belongs to the service layer and is independent of the UI.</p>
 *
 * <p>Important implementation note: coordination is intentionally done with
 * JVM-local read/write locks instead of {@code FileChannel.lock()}.
 * Java file locks throw {@code OverlappingFileLockException} when the same JVM
 * acquires overlapping locks on the same file, which is exactly what happens
 * when background downloads and Swing refreshes touch the manifest in parallel.
 * Since this application is a single desktop process, in-memory locking is the
 * correct and more robust approach here.</p>
 *
 * @author Sergej Zerr
 */
public class ManifestService {
    private static final Logger LOGGER = Logger.getLogger(ManifestService.class.getName());

    private static final int IO_RETRIES = 8;
    private static final long BASE_RETRY_DELAY_MILLIS = 75L;

    /**
     * One read/write lock per normalized manifest path, shared across all
     * ManifestService instances in this JVM.
     */
    private static final ConcurrentHashMap<Path, ReentrantReadWriteLock> PATH_LOCKS =
            new ConcurrentHashMap<>();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Reads the manifest in a UI-friendly, lenient way.
     *
     * <p>If the manifest is temporarily unreadable because another thread is
     * replacing it, this method retries a few times and finally returns an
     * empty list instead of failing the caller. This is suitable for view
     * refresh logic.</p>
     *
     * @param manifestPath path to the manifest file
     * @return manifest entries, or an empty list when no stable manifest
     *         content could be obtained
     */
    public List<DownloadManifestEntry> read(Path manifestPath) {
        return readInternal(manifestPath, false);
    }

    /**
     * Reads the manifest strictly.
     *
     * <p>This method is intended for read-modify-write operations where
     * silently returning an empty list could destroy existing manifest state.
     * It retries transient failures and throws an exception if reading still
     * does not succeed.</p>
     *
     * @param manifestPath path to the manifest file
     * @return manifest entries
     */
    public List<DownloadManifestEntry> readStrict(Path manifestPath) {
        return readInternal(manifestPath, true);
    }

    /**
     * Writes the manifest atomically as far as the platform allows.
     *
     * <p>The implementation uses a unique temporary file for each write
     * attempt. This is much more robust than using a single fixed temp file
     * name, especially on Windows and shared folders.</p>
     *
     * @param manifestPath path to the manifest file
     * @param entries entries to persist
     */
    public void write(Path manifestPath, List<DownloadManifestEntry> entries) {
        if (manifestPath == null) {
            throw new IllegalArgumentException("manifestPath must not be null");
        }

        Path normalizedPath = normalize(manifestPath);
        ReentrantReadWriteLock lock = lockFor(normalizedPath);
        lock.writeLock().lock();

        try {
            Path parent = normalizedPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            IOException lastError = null;

            for (int attempt = 1; attempt <= IO_RETRIES; attempt++) {
                Path tempFile = tempPathOf(normalizedPath);
                Path backupFile = backupPathOf(normalizedPath);

                try {
                    writeJson(tempFile, entries);

                    if (Files.exists(normalizedPath)) {
                        try {
                            Files.copy(
                                    normalizedPath,
                                    backupFile,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException backupError) {
                            LOGGER.log(
                                    Level.FINE,
                                    "Could not refresh manifest backup: " + backupFile,
                                    backupError);
                        }
                    }

                    moveReplace(tempFile, normalizedPath);
                    return;
                } catch (IOException e) {
                    lastError = e;

                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignoredDelete) {
                        // Best effort cleanup only.
                    }

                    LOGGER.log(
                            Level.FINE,
                            "Manifest write attempt " + attempt + " failed for " + normalizedPath,
                            e);
                    sleepQuietly(retryDelayMillis(attempt));
                }
            }

            throw new IllegalStateException(
                    "Could not write manifest: " + normalizedPath,
                    lastError);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write manifest: " + normalizedPath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<DownloadManifestEntry> readInternal(Path manifestPath, boolean strict) {
        if (manifestPath == null) {
            return new ArrayList<>();
        }

        Path normalizedPath = normalize(manifestPath);
        if (!Files.exists(normalizedPath)) {
            return new ArrayList<>();
        }

        ReentrantReadWriteLock lock = lockFor(normalizedPath);
        Exception lastError = null;

        for (int attempt = 1; attempt <= IO_RETRIES; attempt++) {
            lock.readLock().lock();
            try {
                try (Reader reader = Files.newBufferedReader(normalizedPath, StandardCharsets.UTF_8)) {
                    DownloadManifestEntry[] entries =
                            gson.fromJson(reader, DownloadManifestEntry[].class);

                    return entries == null
                            ? new ArrayList<>()
                            : new ArrayList<>(Arrays.asList(entries));
                }
            } catch (JsonSyntaxException | AccessDeniedException e) {
                lastError = e;
                LOGGER.log(
                        Level.FINE,
                        "Manifest temporarily unreadable, retrying: " + normalizedPath,
                        e);
            } catch (IOException e) {
                lastError = e;
                LOGGER.log(
                        Level.FINE,
                        "Manifest read attempt " + attempt + " failed for " + normalizedPath,
                        e);
            } finally {
                lock.readLock().unlock();
            }

            sleepQuietly(retryDelayMillis(attempt));
        }

        if (strict) {
            throw new IllegalStateException("Could not read manifest: " + normalizedPath, lastError);
        }

        LOGGER.log(
                Level.FINE,
                "Giving up on lenient manifest read, returning empty list: " + normalizedPath,
                lastError);

        return new ArrayList<>();
    }

    private void writeJson(Path target, List<DownloadManifestEntry> entries) throws IOException {
        try (FileChannel channel = FileChannel.open(
                target,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
             Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {

            gson.toJson(entries, writer);
            writer.flush();
            channel.force(true);
        }
    }

    private void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path backupPathOf(Path manifestPath) {
        return manifestPath.resolveSibling(manifestPath.getFileName() + ".bak");
    }

    private Path tempPathOf(Path manifestPath) {
        String suffix = ".tmp."
                + System.nanoTime()
                + "."
                + ThreadLocalRandom.current().nextInt(1_000_000);
        return manifestPath.resolveSibling(manifestPath.getFileName() + suffix);
    }

    private ReentrantReadWriteLock lockFor(Path manifestPath) {
        return PATH_LOCKS.computeIfAbsent(manifestPath, key -> new ReentrantReadWriteLock(true));
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private long retryDelayMillis(int attempt) {
        return BASE_RETRY_DELAY_MILLIS * attempt;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}