package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import com.example.dataverse.downloader.model.AppPreferences;
import com.example.dataverse.downloader.model.DatasetInfo;
import com.example.dataverse.downloader.model.DownloadTask;
import com.example.dataverse.downloader.persistence.PreferencesService;
import com.example.dataverse.downloader.service.DatasetLoadListener;
import com.example.dataverse.downloader.service.DatasetService;
import com.example.dataverse.downloader.service.DownloadManager;
import com.example.dataverse.downloader.service.DownloadService;

public class MainFrame extends JFrame {
    private final PreferencesService preferencesService;
    private final DatasetService datasetService;
    private final DownloadService downloadService;

    private AppPreferences preferences;
    private DatasetInfo currentDataset;

    private final JComboBox<String> datasetField = new JComboBox<>();
    private final DatasetTreePanel datasetTreePanel = new DatasetTreePanel();
    private final StatusBar statusBar = new StatusBar();

    public MainFrame(
            PreferencesService preferencesService,
            DatasetService datasetService,
            DownloadService downloadService,
            DownloadManager downloadManager) {

        super("Dataverse Downloader");

        this.preferencesService = preferencesService;
        this.datasetService = datasetService;
        this.downloadService = downloadService;
        this.preferences = preferencesService.load();

        setWindowIcon();

        datasetField.setEditable(true);
        datasetField.setPrototypeDisplayValue(
                "https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/EXAMPLE123456");
        refreshDatasetHistory();
        installDatasetHistoryPopupBehavior();
        installDatasetEditorContextMenu();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        setJMenuBar(createMenuBar());
        add(createTopPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                datasetTreePanel,
                new DownloadManagerPanel(downloadManager, downloadService));
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu optionsMenu = new JMenu("Options");

        JMenuItem preferencesItem = new JMenuItem("Preferences...");
        preferencesItem.addActionListener(e -> editPreferences());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());

        optionsMenu.add(preferencesItem);
        optionsMenu.addSeparator();
        optionsMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");

        JMenuItem aboutItem = new JMenuItem("About...");
        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(aboutItem);

        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.add(new JLabel("Dataset URL / DOI:"), BorderLayout.WEST);
        inputPanel.add(datasetField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        buttonsPanel.add(new JButton(new AbstractAction("Load Dataset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadDataset();
            }
        }));

        buttonsPanel.add(new JButton(new AbstractAction("Download Selected") {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelected();
            }
        }));

        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.EAST);

        return panel;
    }

    private void loadDataset() {
        String datasetValue = getDatasetFieldText();
        if (datasetValue.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a dataset URL or DOI.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        preferencesService.recordDatasetUrl(
                datasetValue,
                preferences.getDatasetUrlHistorySize());
        refreshDatasetHistory();
        datasetField.getEditor().setItem(datasetValue);

        LoadingDialog loadingDialog = new LoadingDialog(this, "Loading dataset...");
        setUiBusy(true);
        statusBar.setMessage("Loading dataset...");

        SwingWorker<DatasetInfo, Runnable> worker = new SwingWorker<>() {
            @Override
            protected DatasetInfo doInBackground() throws Exception {
                return datasetService.loadDataset(
                        preferences.getServerUrl(),
                        preferences.getApiKey(),
                        datasetValue,
                        new DatasetLoadListener() {
                            @Override
                            public void onStatus(String message) {
                                publish(() -> loadingDialog.setStatus(message));
                            }

                            @Override
                            public void onProgress(int percent) {
                                publish(() -> loadingDialog.setProgress(percent));
                            }

                            @Override
                            public void onFileCountDiscovered(int totalFiles) {
                                publish(() -> loadingDialog.setDetail("Found " + totalFiles + " files"));
                            }

                            @Override
                            public void onFilesLoaded(int loadedFiles, int totalFiles) {
                                publish(() -> loadingDialog.setDetail(
                                        totalFiles > 0
                                                ? "Loaded metadata for " + loadedFiles + " / " + totalFiles + " files"
                                                : "Loaded metadata for  " + loadedFiles + " files"));
                            }
                        });
            }

            @Override
            protected void process(List<Runnable> chunks) {
                for (Runnable runnable : chunks) {
                    runnable.run();
                }
            }

            @Override
            protected void done() {
                try {
                    currentDataset = get();
                    datasetTreePanel.setDataset(currentDataset);

                    statusBar.setMessage(
                            "Loaded: " + currentDataset.getTitle()
                                    + " (" + currentDataset.getFiles().size() + " files)");
                } catch (Exception e) {
                    currentDataset = null;
                    statusBar.setMessage("Load failed");

                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String message = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
                    cause.printStackTrace();

                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            message,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setUiBusy(false);
                    loadingDialog.dispose();
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }

    private void downloadSelected() {
        if (currentDataset == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Load a dataset first.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final String effectiveServerUrl =
                currentDataset.getServerUrl() != null && !currentDataset.getServerUrl().isBlank()
                        ? currentDataset.getServerUrl()
                        : preferences.getServerUrl();

        List<DownloadTask> tasks = datasetTreePanel.getSelectedFiles().stream()
                .map(file -> new DownloadTask(
                        file,
                        currentDataset.getPersistentId(),
                        effectiveServerUrl,
                        preferences.getApiKey(),
                        destinationRoot(currentDataset)))
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select at least one file or folder.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        downloadService.queueDownloads(tasks);
        statusBar.setMessage("Queued " + tasks.size() + " download(s)");
    }

    private Path destinationRoot(DatasetInfo datasetInfo) {
        return preferences.getOutputFolder().resolve(safeFolderName(datasetInfo.getTitle()));
    }

    private String safeFolderName(String value) {
        if (value == null || value.isBlank()) {
            return "dataset";
        }
        return value.replaceAll("[^a-zA-Z0-9._ -]", "_");
    }

    private void editPreferences() {
        PreferencesDialog dialog = new PreferencesDialog(this, preferences);
        dialog.setVisible(true);

        if (!dialog.isSaved()) {
            return;
        }

        try {
            AppPreferences updatedPreferences = dialog.toPreferences();

            if (updatedPreferences.getServerUrl() == null || updatedPreferences.getServerUrl().isBlank()) {
                throw new IllegalArgumentException("Dataverse host URL is required.");
            }
            if (updatedPreferences.getOutputFolder() == null) {
                throw new IllegalArgumentException("Output folder is required.");
            }
            if (updatedPreferences.getParallelDownloads() <= 0) {
                throw new IllegalArgumentException("Parallel downloads must be greater than 0.");
            }
            if (updatedPreferences.getDatasetUrlHistorySize() < 0) {
                throw new IllegalArgumentException("Dataset URL history size must be 0 or greater.");
            }

            preferences = updatedPreferences;
            preferencesService.save(preferences);
            refreshDatasetHistory();
            statusBar.setMessage("Preferences saved");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save preferences: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusBar.setMessage("Preferences not saved");
        }
    }

    private void showAboutDialog() {
        AboutDialog dialog = new AboutDialog(this);
        dialog.setVisible(true);
    }

    private void setUiBusy(boolean busy) {
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());

        setComponentsEnabled(getContentPane(), !busy);

        if (getJMenuBar() != null) {
            for (int i = 0; i < getJMenuBar().getMenuCount(); i++) {
                JMenu menu = getJMenuBar().getMenu(i);
                if (menu != null) {
                    menu.setEnabled(!busy);
                }
            }
        }
    }

    private void setComponentsEnabled(Container container, boolean enabled) {
        for (Component component : container.getComponents()) {
            component.setEnabled(enabled);
            if (component instanceof Container child) {
                setComponentsEnabled(child, enabled);
            }
        }
    }

    private void refreshDatasetHistory() {
        List<String> history = preferencesService.loadDatasetUrlHistory(preferences.getDatasetUrlHistorySize());
        Object currentValue = datasetField.isEditable() ? datasetField.getEditor().getItem() : datasetField.getSelectedItem();

        datasetField.removeAllItems();
        for (String item : history) {
            datasetField.addItem(item);
        }

        if (currentValue != null) {
            datasetField.getEditor().setItem(currentValue);
        }
    }

    private String getDatasetFieldText() {
        Object editorItem = datasetField.getEditor().getItem();
        return editorItem == null ? "" : editorItem.toString().trim();
    }

    private void installDatasetHistoryPopupBehavior() {
        Component editor = datasetField.getEditor().getEditorComponent();
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger() && datasetField.getItemCount() > 0) {
                    datasetField.showPopup();
                }
            }
        });
    }

    private void installDatasetEditorContextMenu() {
        Component editorComponent = datasetField.getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextComponent textComponent)) {
            return;
        }

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.addActionListener(e -> textComponent.cut());

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> textComponent.copy());

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.addActionListener(e -> textComponent.paste());

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> textComponent.selectAll());

        popupMenu.add(cutItem);
        popupMenu.add(copyItem);
        popupMenu.add(pasteItem);
        popupMenu.addSeparator();
        popupMenu.add(selectAllItem);

        textComponent.setComponentPopupMenu(popupMenu);

        textComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }

                textComponent.requestFocusInWindow();

                boolean hasSelection = textComponent.getSelectionStart() != textComponent.getSelectionEnd();
                boolean hasText = !textComponent.getText().isEmpty();
                boolean editable = textComponent.isEditable();
                boolean clipboardHasString = hasStringInClipboard();

                cutItem.setEnabled(editable && hasSelection);
                copyItem.setEnabled(hasSelection);
                pasteItem.setEnabled(editable && clipboardHasString);
                selectAllItem.setEnabled(hasText);

                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private boolean hasStringInClipboard() {
        try {
            return Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            return true;
        }
    }

    private void setWindowIcon() {
        URL iconUrl = getClass().getResource("/logos/dataverse_min.png");
        if (iconUrl == null) {
            return;
        }

        Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);
        setIconImage(image);
    }

    private static class LoadingDialog extends JDialog {
        private final JLabel statusLabel = new JLabel();
        private final JLabel detailLabel = new JLabel(" ");
        private final JProgressBar progressBar = new JProgressBar(0, 100);

        LoadingDialog(Frame owner, String message) {
            super(owner, "Please wait", Dialog.ModalityType.APPLICATION_MODAL);

            if (owner != null && owner.getIconImage() != null) {
                setIconImage(owner.getIconImage());
            }

            statusLabel.setText(message);
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            panel.add(statusLabel, BorderLayout.NORTH);
            panel.add(progressBar, BorderLayout.CENTER);
            panel.add(detailLabel, BorderLayout.SOUTH);

            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setContentPane(panel);
            setSize(420, 140);
            setResizable(false);
            setLocationRelativeTo(owner);
        }

        void setStatus(String message) {
            statusLabel.setText(message == null ? "" : message);
        }

        void setDetail(String message) {
            detailLabel.setText(message == null || message.isBlank() ? " " : message);
        }

        void setProgress(int percent) {
            progressBar.setValue(Math.max(0, Math.min(100, percent)));
        }
    }
}