package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.example.dataverse.downloader.model.DatasetFileEntry;
import com.example.dataverse.downloader.model.DatasetInfo;

public class DatasetTreePanel extends JPanel {
    private final CheckboxTreeNode root = new CheckboxTreeNode("No dataset loaded");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);

    public DatasetTreePanel() {
        setLayout(new BorderLayout());

        tree.setCellRenderer(new CheckboxTreeCellRenderer());
        tree.setRootVisible(true);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                Object node = path.getLastPathComponent();
                if (node instanceof CheckboxTreeNode checkboxTreeNode) {
                    checkboxTreeNode.setSelected(!checkboxTreeNode.isSelected());
                    tree.repaint();
                }
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void setDataset(DatasetInfo datasetInfo) {
        root.removeAllChildren();
        root.setSelected(false);
        root.setUserObject(datasetInfo.getTitle() + " (" + datasetInfo.getPersistentId() + ")");

        Map<String, CheckboxTreeNode> folderNodes = new HashMap<>();
        folderNodes.put("", root);

        List<DatasetFileEntry> sorted = new ArrayList<>(datasetInfo.getFiles());
        sorted.sort(Comparator.comparing(DatasetFileEntry::getRelativePath));

        for (DatasetFileEntry entry : sorted) {
            String path = entry.getPath() == null ? "" : entry.getPath();
            CheckboxTreeNode parent = ensureFolders(path, folderNodes);
            parent.add(new CheckboxTreeNode(entry));
        }

        treeModel.reload();

        if (tree.getRowCount() > 0) {
            tree.expandRow(0);
        }
    }

    private CheckboxTreeNode ensureFolders(String path, Map<String, CheckboxTreeNode> folderNodes) {
        if (path == null || path.isBlank()) {
            return root;
        }

        if (folderNodes.containsKey(path)) {
            return folderNodes.get(path);
        }

        String[] parts = path.split("/");
        StringBuilder current = new StringBuilder();
        CheckboxTreeNode parent = root;

        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }

            if (!current.isEmpty()) {
                current.append('/');
            }
            current.append(part);

            String key = current.toString();
            CheckboxTreeNode node = folderNodes.get(key);
            if (node == null) {
                node = new CheckboxTreeNode(part);
                parent.add(node);
                folderNodes.put(key, node);
            }

            parent = node;
        }

        return parent;
    }

    public List<DatasetFileEntry> getSelectedFiles() {
        List<DatasetFileEntry> selected = new ArrayList<>();

        Enumeration<?> enumeration = root.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            Object node = enumeration.nextElement();
            if (node instanceof CheckboxTreeNode checkboxNode
                    && checkboxNode.isSelected()
                    && checkboxNode.getUserObject() instanceof DatasetFileEntry entry) {
                selected.add(entry);
            }
        }

        return selected;
    }
}