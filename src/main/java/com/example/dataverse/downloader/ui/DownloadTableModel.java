package com.example.dataverse.downloader.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.example.dataverse.downloader.model.DownloadTask;

public class DownloadTableModel extends AbstractTableModel {
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
        int index = tasks.indexOf(task);
        if (index < 0) {
            tasks.add(task);
            fireTableRowsInserted(tasks.size() - 1, tasks.size() - 1);
        } else {
            fireTableRowsUpdated(index, index);
        }
    }

    private String actionLabel(DownloadTask task) {
        return switch (task.getStatus()) {
            case RUNNING, QUEUED -> "⏸";
            case PAUSED, FAILED -> "▶";
            default -> "";
        };
    }
}