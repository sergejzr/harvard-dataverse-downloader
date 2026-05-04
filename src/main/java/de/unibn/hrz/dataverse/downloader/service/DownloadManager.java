package de.unibn.hrz.dataverse.downloader.service;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.unibn.hrz.dataverse.downloader.model.DownloadTask;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask.Status;

public class DownloadManager {
    public interface Listener {
        void onTaskUpdated(DownloadTask task);
    }

    public static final class DownloadStats {
        private final int total;
        private final int completed;
        private final int remaining;
        private final int failed;
        private final int percentCompleted;

        public DownloadStats(int total, int completed, int remaining, int failed, int percentCompleted) {
            this.total = total;
            this.completed = completed;
            this.remaining = remaining;
            this.failed = failed;
            this.percentCompleted = percentCompleted;
        }

        public int getTotal() {
            return total;
        }

        public int getCompleted() {
            return completed;
        }

        public int getRemaining() {
            return remaining;
        }

        public int getFailed() {
            return failed;
        }

        public int getPercentCompleted() {
            return percentCompleted;
        }

        public boolean isEmpty() {
            return total <= 0;
        }
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

    public DownloadStats getDownloadStats() {
        int total = tasks.size();
        int completed = 0;
        int failed = 0;

        for (DownloadTask task : tasks) {
            if (task.getStatus() == Status.COMPLETED) {
                completed++;
            } else if (task.getStatus() == Status.FAILED) {
                failed++;
            }
        }

        int remaining = Math.max(0, total - completed);
        int percentCompleted = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);

        return new DownloadStats(total, completed, remaining, failed, percentCompleted);
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