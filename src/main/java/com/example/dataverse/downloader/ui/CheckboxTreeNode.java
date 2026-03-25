package com.example.dataverse.downloader.ui;

import javax.swing.tree.DefaultMutableTreeNode;

public class CheckboxTreeNode extends DefaultMutableTreeNode {
    private boolean selected;

    public CheckboxTreeNode(Object userObject) {
        super(userObject);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        for (int i = 0; i < getChildCount(); i++) {
            Object child = getChildAt(i);
            if (child instanceof CheckboxTreeNode checkboxTreeNode) {
                checkboxTreeNode.setSelected(selected);
            }
        }
    }
}
