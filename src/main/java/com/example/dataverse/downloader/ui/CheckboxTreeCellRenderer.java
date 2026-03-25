package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

public class CheckboxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private final JCheckBox checkBox = new JCheckBox();
    private final DefaultTreeCellRenderer delegate = new DefaultTreeCellRenderer();

    public CheckboxTreeCellRenderer() {
        setLayout(new BorderLayout());
        setOpaque(false);
        add(checkBox, BorderLayout.WEST);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        removeAll();
        checkBox.setOpaque(false);
        add(checkBox, BorderLayout.WEST);
        add(delegate.getTreeCellRendererComponent(tree, "", selected, expanded, leaf, row, hasFocus), BorderLayout.CENTER);
        if (value instanceof CheckboxTreeNode node) {
            checkBox.setSelected(node.isSelected());
            checkBox.setText(String.valueOf(node.getUserObject()));
        }
        return this;
    }
}
