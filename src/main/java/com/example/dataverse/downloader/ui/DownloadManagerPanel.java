package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.example.dataverse.downloader.model.DownloadTask;
import com.example.dataverse.downloader.model.DownloadTask.Status;
import com.example.dataverse.downloader.service.DownloadManager;
import com.example.dataverse.downloader.service.DownloadService;

public class DownloadManagerPanel extends JPanel implements DownloadManager.Listener {
    private final DownloadTableModel tableModel = new DownloadTableModel();
    private final JTable table;

    public DownloadManagerPanel(DownloadManager downloadManager, DownloadService downloadService) {
        setLayout(new BorderLayout());

        table = new JTable(tableModel);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(4).setCellRenderer(new ActionButtonRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row < 0 || col != 4) {
                    return;
                }

                DownloadTask task = tableModel.getTaskAt(row);

                if (task.getStatus() == Status.RUNNING || task.getStatus() == Status.QUEUED) {
                    downloadService.pauseDownload(task);
                } else if (task.getStatus() == Status.PAUSED || task.getStatus() == Status.FAILED) {
                    downloadService.resumeDownload(task);
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        downloadManager.addListener(this);
    }

    @Override
    public void onTaskUpdated(DownloadTask task) {
        javax.swing.SwingUtilities.invokeLater(() -> tableModel.upsert(task));
    }

    private static class ActionButtonRenderer extends JButton implements TableCellRenderer {
        ActionButtonRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }
}