package com.example.dataverse.downloader.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.dataverse.downloader.model.DownloadTask;
import com.example.dataverse.downloader.model.DownloadTask.Status;

public class DownloadManager {
    public interface Listener {
        void onTaskUpdated(DownloadTask task);
    }

    private final List<DownloadTask> tasks = new CopyOnWriteArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void addTask(DownloadTask task) {
        tasks.add(task);
        notifyListeners(task);
    }

    public List<DownloadTask> getTasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(DownloadTask task) {
        for (Listener listener : listeners) {
            listener.onTaskUpdated(task);
        }
    }

    public boolean hasActiveTaskFor(DownloadTask candidate) {
        Path candidatePath = candidate.getDestination().resolve(candidate.getEntry().getRelativePath());

        for (DownloadTask existing : tasks) {
            Path existingPath = existing.getDestination().resolve(existing.getEntry().getRelativePath());

            boolean sameFile = existing.getEntry().getFileId() == candidate.getEntry().getFileId()
                    || existingPath.equals(candidatePath);

            if (!sameFile) {
                continue;
            }

            Status status = existing.getStatus();
            if (status == Status.QUEUED || status == Status.RUNNING) {
                return true;
            }
        }

        return false;
    }
}