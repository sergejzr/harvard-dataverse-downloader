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
package de.unibn.hrz.dataverse.downloader.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.unibn.hrz.dataverse.downloader.model.DownloadTask;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask.Status;

public class DownloadTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -4745156303341089967L;

    private final String[] columns = { "File", "Status", "Progress", "Message", "Action" };
    private final List<DownloadTask> tasks = new ArrayList<>();

    @Override
    public int getRowCount() {
        return tasks.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DownloadTask task = tasks.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> task.getEntry().getRelativePath();
            case 1 -> task.getStatus();
            case 2 -> task.getProgressPercent() + "%";
            case 3 -> task.getMessage();
            case 4 -> actionLabel(task);
            default -> "";
        };
    }

    public DownloadTask getTaskAt(int rowIndex) {
        return tasks.get(rowIndex);
    }

    public void upsert(DownloadTask task) {
        int existingIndex = tasks.indexOf(task);
        
        if (existingIndex < 0) {
            int insertIndex = findInsertIndex(task);
            tasks.add(insertIndex, task);
            fireTableRowsInserted(insertIndex, insertIndex);
            return;
        }

        /*
         * Important:
         * DownloadTask is mutable and the table keeps the same object reference.
         * So when status/progress changes, the object already reflects the new
         * state by the time we get here. Therefore we cannot compare "old group"
         * vs "new group" using the object currently stored in the list.
         *
         * Instead, remove only the updated item and insert it again at its
         * correct new position. This moves exactly one row, not the full table.
         */
        int newIndex = existingIndex;
        //task.getStatus()
        if(task.getProgressPercent()<=5&&task.getProgressPercent()>=98) {
        tasks.remove(existingIndex);
        newIndex = findInsertIndex(task);
        tasks.add(newIndex, task);
        }
        if (newIndex == existingIndex) {
        	
            fireTableRowsUpdated(newIndex, newIndex);
        } else {
            fireTableDataChanged();
        }
    }

    private int findInsertIndex(DownloadTask task) {
        int targetGroup = sortGroup(task);
        ///if (targetGroup==0) {return }
        for (int i = 0; i < tasks.size(); i++) {
            DownloadTask current = tasks.get(i);
            int currentGroup = sortGroup(current);

            if (targetGroup < currentGroup) {
                return i;
            }
        }

        return tasks.size();
    }

    private int sortGroup(DownloadTask task) {
        Status status = task.getStatus();
        int progress = task.getProgressPercent();

        if (status == Status.RUNNING) {
            return 0;
        }

        if (status == Status.PAUSED) {
            return 1;
        }

        if (status == Status.QUEUED && progress > 0) {
            return 1;
        }

        if (status == Status.QUEUED) {
            return 2;
        }

        if (status == Status.FAILED) {
            return 3;
        }

        if (status == Status.COMPLETED) {
            return 4;
        }

        return 5;
    }

    private String actionLabel(DownloadTask task) {
        return switch (task.getStatus()) {
            case RUNNING -> "⏸";
            case PAUSED, FAILED -> "▶";
            case QUEUED -> "○";
            case COMPLETED -> "\u2714";
        };
    }
}