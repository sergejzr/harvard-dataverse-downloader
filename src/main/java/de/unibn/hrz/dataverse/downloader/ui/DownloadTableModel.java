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

        // New task
        if (existingIndex < 0) {
            int insertIndex = findInsertIndex(task.getStatus());
            tasks.add(insertIndex, task);
            fireTableRowsInserted(insertIndex, insertIndex);
            return;
        }

        DownloadTask existingTask = tasks.get(existingIndex);
        Status oldStatus = existingTask.getStatus();
        Status newStatus = task.getStatus();

        // Replace stored task data
        tasks.set(existingIndex, task);

        // Same cluster -> keep row where it is
        if (oldStatus == newStatus) {
            fireTableRowsUpdated(existingIndex, existingIndex);
            return;
        }

        // Different cluster -> move to end of new cluster
        tasks.remove(existingIndex);
        int newIndex = findInsertIndex(newStatus);
        tasks.add(newIndex, task);

        fireTableDataChanged();
    }

    /**
     * Returns the position where a task with the given status should be inserted.
     * It will be appended to the end of its status cluster.
     */
    private int findInsertIndex(Status status) {
        int newPriority = priority(status);

        int index = 0;
        while (index < tasks.size() && priority(tasks.get(index).getStatus()) <= newPriority) {
            index++;
        }
        return index;
    }

    private int priority(Status status) {
        return switch (status) {
            case RUNNING -> 0;
            case PAUSED -> 1;
            case QUEUED -> 2;
            case FAILED -> 3;
            case COMPLETED -> 4;
        };
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