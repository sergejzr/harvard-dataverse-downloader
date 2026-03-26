package de.unibn.hrz.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import de.unibn.hrz.dataverse.downloader.model.DatasetFileEntry;
import de.unibn.hrz.dataverse.downloader.model.DatasetInfo;
import de.unibn.hrz.dataverse.downloader.model.DownloadManifestEntry;
import de.unibn.hrz.dataverse.downloader.service.ManifestService;

public class DatasetTreePanel extends JPanel {
    private static final long serialVersionUID = 7034553359151504280L;

    private final CheckboxTreeNode root = new CheckboxTreeNode("No dataset loaded");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final ManifestService manifestService = new ManifestService();

    private final AtomicBoolean refreshQueued = new AtomicBoolean(false);

    private DatasetInfo currentDataset;
    private Path currentDestinationRoot;

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
                    checkboxTreeNode.setSelected(!checkboxTreeNode.isSelected(), true);
                    refreshFolderStates();
                    tree.repaint();
                }
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void setDataset(DatasetInfo datasetInfo, Path destinationRoot) {
        currentDataset = datasetInfo;
        currentDestinationRoot = destinationRoot;

        if (datasetInfo == null) {
            root.removeAllChildren();
            root.setSelected(false, false);
            root.setDownloadState(CheckboxTreeNode.DownloadState.NONE);
            root.setUserObject("No dataset loaded");
            treeModel.reload();
            return;
        }

        root.removeAllChildren();
        root.setSelected(false, false);
        root.setDownloadState(CheckboxTreeNode.DownloadState.NONE);
        root.setUserObject(datasetInfo.getTitle() + " (" + datasetInfo.getPersistentId() + ")");

        Map<String, CheckboxTreeNode> folderNodes = new HashMap<>();
        folderNodes.put("", root);

        Map<Long, DownloadManifestEntry> manifestByFileId = loadManifestEntriesByFileId(destinationRoot);

        List<DatasetFileEntry> sorted = new ArrayList<>(datasetInfo.getFiles());
        sorted.sort(Comparator.comparing(DatasetFileEntry::getRelativePath));

        for (DatasetFileEntry entry : sorted) {
            String path = entry.getPath() == null ? "" : entry.getPath();
            CheckboxTreeNode parent = ensureFolders(path, folderNodes);

            CheckboxTreeNode fileNode = new CheckboxTreeNode(entry);
            applyFileDownloadState(fileNode, entry, destinationRoot, manifestByFileId);

            parent.add(fileNode);
        }

        refreshFolderStates();
        treeModel.reload();

        if (tree.getRowCount() > 0) {
            tree.expandRow(0);
        }
    }

    public void refreshDownloadState() {
        if (currentDataset == null || currentDestinationRoot == null) {
            return;
        }

        if (!refreshQueued.compareAndSet(false, true)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                Map<Long, DownloadManifestEntry> manifestByFileId =
                        loadManifestEntriesByFileId(currentDestinationRoot);

                Enumeration<?> enumeration = root.depthFirstEnumeration();
                while (enumeration.hasMoreElements()) {
                    Object node = enumeration.nextElement();
                    if (node instanceof CheckboxTreeNode checkboxNode
                            && checkboxNode.getUserObject() instanceof DatasetFileEntry entry) {
                        applyFileDownloadState(
                                checkboxNode,
                                entry,
                                currentDestinationRoot,
                                manifestByFileId);
                    }
                }

                refreshFolderStates();
                tree.repaint();
            } finally {
                refreshQueued.set(false);
            }
        });
    }

    private void applyFileDownloadState(
            CheckboxTreeNode fileNode,
            DatasetFileEntry entry,
            Path destinationRoot,
            Map<Long, DownloadManifestEntry> manifestByFileId) {

        CheckboxTreeNode.DownloadState state =
                resolveDownloadState(entry, destinationRoot, manifestByFileId);

        fileNode.setDownloadState(state);

        if (state == CheckboxTreeNode.DownloadState.COMPLETED
                || state == CheckboxTreeNode.DownloadState.PARTIAL) {
            fileNode.setSelected(true, false);
        }
    }

    private CheckboxTreeNode.DownloadState resolveDownloadState(
            DatasetFileEntry entry,
            Path destinationRoot,
            Map<Long, DownloadManifestEntry> manifestByFileId) {

        if (destinationRoot == null || entry == null) {
            return CheckboxTreeNode.DownloadState.NONE;
        }

        Path finalFile = destinationRoot.resolve(entry.getRelativePath());
        Path partFile = finalFile.resolveSibling(finalFile.getFileName() + ".part");
        DownloadManifestEntry manifestEntry = manifestByFileId.get(entry.getFileId());

        try {
            if (isCompleted(finalFile, entry)) {
                return CheckboxTreeNode.DownloadState.COMPLETED;
            }

            if (isPartial(partFile, manifestEntry, entry)) {
                return CheckboxTreeNode.DownloadState.PARTIAL;
            }
        } catch (Exception ignored) {
            // Fall through to NONE.
        }

        return CheckboxTreeNode.DownloadState.NONE;
    }

    private Map<Long, DownloadManifestEntry> loadManifestEntriesByFileId(Path destinationRoot) {
        Map<Long, DownloadManifestEntry> result = new HashMap<>();

        if (destinationRoot == null) {
            return result;
        }

        Path manifestPath = destinationRoot.resolve("download-manifest.json");
        List<DownloadManifestEntry> entries = manifestService.read(manifestPath);

        for (DownloadManifestEntry entry : entries) {
            result.put(entry.getFileId(), entry);
        }

        return result;
    }

    private boolean isCompleted(
            Path finalFile,
            DatasetFileEntry datasetEntry) throws Exception {

        if (!Files.exists(finalFile) || Files.isDirectory(finalFile)) {
            return false;
        }

        if (datasetEntry.getSize() > 0 && Files.size(finalFile) != datasetEntry.getSize()) {
            return false;
        }

        return true;
    }

    private boolean isPartial(
            Path partFile,
            DownloadManifestEntry manifestEntry,
            DatasetFileEntry datasetEntry) throws Exception {

        boolean partFileLooksPartial = Files.exists(partFile)
                && !Files.isDirectory(partFile)
                && Files.size(partFile) > 0
                && (datasetEntry.getSize() <= 0 || Files.size(partFile) < datasetEntry.getSize());

        boolean manifestHasPartialBytes = manifestEntry != null
                && manifestEntry.getBytesDownloaded() > 0
                && (datasetEntry.getSize() <= 0 || manifestEntry.getBytesDownloaded() < datasetEntry.getSize());

        boolean manifestStatusIndicatesIncomplete = manifestEntry != null
                && manifestEntry.getBytesDownloaded() > 0
                && ("PAUSED".equalsIgnoreCase(manifestEntry.getStatus())
                    || "FAILED".equalsIgnoreCase(manifestEntry.getStatus())
                    || "RUNNING".equalsIgnoreCase(manifestEntry.getStatus())
                    || "QUEUED".equalsIgnoreCase(manifestEntry.getStatus()));

        return partFileLooksPartial || manifestHasPartialBytes || manifestStatusIndicatesIncomplete;
    }

    private void refreshFolderStates() {
        updateFolderStates(root);
    }

    private void updateFolderStates(CheckboxTreeNode node) {
        if (node.isLeaf()) {
            return;
        }

        boolean allChildrenSelected = node.getChildCount() > 0;
        boolean allChildrenCompleted = node.getChildCount() > 0;
        boolean hasAnyPartialOrCompleted = false;

        for (int i = 0; i < node.getChildCount(); i++) {
            CheckboxTreeNode child = (CheckboxTreeNode) node.getChildAt(i);
            updateFolderStates(child);

            allChildrenSelected &= child.isSelected();
            allChildrenCompleted &= child.getDownloadState() == CheckboxTreeNode.DownloadState.COMPLETED;

            if (child.getDownloadState() == CheckboxTreeNode.DownloadState.PARTIAL
                    || child.getDownloadState() == CheckboxTreeNode.DownloadState.COMPLETED) {
                hasAnyPartialOrCompleted = true;
            }
        }

        node.setSelected(allChildrenSelected, false);

        if (allChildrenCompleted) {
            node.setDownloadState(CheckboxTreeNode.DownloadState.COMPLETED);
        } else if (hasAnyPartialOrCompleted) {
            node.setDownloadState(CheckboxTreeNode.DownloadState.PARTIAL);
        } else {
            node.setDownloadState(CheckboxTreeNode.DownloadState.NONE);
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