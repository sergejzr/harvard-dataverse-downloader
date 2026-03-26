package de.unibn.hrz.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

public class CheckboxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private static final long serialVersionUID = 7492464622082795087L;

    private static final Color COMPLETED_COLOR = new Color(0, 140, 0);
    private static final Color PARTIAL_COLOR = new Color(200, 120, 0);

    private final JCheckBox checkBox = new JCheckBox();
    private final JLabel statusLabel = new JLabel();
    private final DefaultTreeCellRenderer delegate = new DefaultTreeCellRenderer();

    public CheckboxTreeCellRenderer() {
        setLayout(new BorderLayout(4, 0));
        setOpaque(false);

        checkBox.setOpaque(false);
        statusLabel.setOpaque(false);

        add(checkBox, BorderLayout.WEST);
        add(statusLabel, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        removeAll();

        checkBox.setOpaque(false);
        statusLabel.setOpaque(false);

        add(checkBox, BorderLayout.WEST);
        add(statusLabel, BorderLayout.CENTER);

        JLabel label = (JLabel) delegate.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof CheckboxTreeNode node) {
            checkBox.setSelected(node.isSelected());

            CheckboxTreeNode.DownloadState state = node.getDownloadState();
            if (state == CheckboxTreeNode.DownloadState.COMPLETED) {
                statusLabel.setText("\u2714");
                statusLabel.setForeground(COMPLETED_COLOR);
            } else if (state == CheckboxTreeNode.DownloadState.PARTIAL) {
                statusLabel.setText("..");
                statusLabel.setForeground(PARTIAL_COLOR);
            } else {
                statusLabel.setText("  ");
                statusLabel.setForeground(label.getForeground());
            }
        } else {
            checkBox.setSelected(false);
            statusLabel.setText("  ");
            statusLabel.setForeground(label.getForeground());
        }

        add(label, BorderLayout.EAST);
        return this;
    }
}