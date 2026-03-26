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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import de.unibn.hrz.dataverse.downloader.model.DownloadTask;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask.Status;
import de.unibn.hrz.dataverse.downloader.service.DownloadManager;
import de.unibn.hrz.dataverse.downloader.service.DownloadService;

public class DownloadManagerPanel extends JPanel implements DownloadManager.Listener {
    private static final long serialVersionUID = -2859989094229163062L;

    private static final Color COMPLETED_GREEN = new Color(0, 140, 0);

    private final DownloadTableModel tableModel = new DownloadTableModel();
    private final JTable table;

    public DownloadManagerPanel(DownloadManager downloadManager, DownloadService downloadService) {
        setLayout(new BorderLayout());

        table = new JTable(tableModel);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
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

                if (task.getStatus() == Status.RUNNING) {
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
        SwingUtilities.invokeLater(() -> tableModel.upsert(task));
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -2928949906498914656L;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (value instanceof Status status) {
                setText(status.name());

                if (!isSelected && status == Status.COMPLETED) {
                    setForeground(COMPLETED_GREEN);
                } else {
                    setForeground(isSelected
                            ? table.getSelectionForeground()
                            : table.getForeground());
                }
            }

            return component;
        }
    }

    private static final class ActionButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 7469380186351450113L;

        ActionButtonRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            String text = value == null ? "" : value.toString();
            setText(text);

            Object statusValue = table.getModel().getValueAt(row, 1);
            Status status = statusValue instanceof Status ? (Status) statusValue : null;

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());

                if (status == Status.COMPLETED) {
                    setForeground(COMPLETED_GREEN);
                } else {
                    setForeground(table.getForeground());
                }
            }

            return this;
        }
    }
}