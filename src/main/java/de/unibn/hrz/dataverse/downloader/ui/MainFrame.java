package de.unibn.hrz.dataverse.downloader.ui;

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import de.unibn.hrz.dataverse.downloader.model.AppPreferences;
import de.unibn.hrz.dataverse.downloader.model.DatasetInfo;
import de.unibn.hrz.dataverse.downloader.model.DownloadTask;
import de.unibn.hrz.dataverse.downloader.persistence.PreferencesService;
import de.unibn.hrz.dataverse.downloader.service.DatasetLoadListener;
import de.unibn.hrz.dataverse.downloader.service.DatasetLoadStatus;
import de.unibn.hrz.dataverse.downloader.service.DatasetService;
import de.unibn.hrz.dataverse.downloader.service.DownloadManager;
import de.unibn.hrz.dataverse.downloader.service.DownloadService;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = -1563570326661428157L;

	private static final String NON_DATAVERSE_MESSAGE_PREFIX = "This DOI does not resolve to a Dataverse repository.";

	private final PreferencesService preferencesService;
	private final DatasetService datasetService;
	private final DownloadService downloadService;
	private final DownloadManager downloadManager;
	private final String startupDatasetInput;

	private AppPreferences preferences;
	private DatasetInfo currentDataset;

	private final JComboBox<String> datasetField = new JComboBox<>();
	private final DatasetTreePanel datasetTreePanel = new DatasetTreePanel();
	private final StatusBar statusBar = new StatusBar();

	public MainFrame(PreferencesService preferencesService, DatasetService datasetService,
			DownloadService downloadService, DownloadManager downloadManager, String startupDatasetInput) {

		super("Dataverse Downloader");

		this.preferencesService = preferencesService;
		this.datasetService = datasetService;
		this.downloadService = downloadService;
		this.downloadManager = downloadManager;
		this.startupDatasetInput = startupDatasetInput;
		this.preferences = preferencesService.load();

		statusBar.setDownloadFolder(this.preferences.getOutputFolder());

		setWindowIcon();

		datasetField.setEditable(true);
		datasetField.setPrototypeDisplayValue(
				"https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/EXAMPLE123456");
		refreshDatasetHistory();

		installDatasetEditorContextMenu();
		installDownloadStateRefresh();
		installDownloadProgressRefresh();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1100, 700);
		// setLocationRelativeTo(null);
		setInitialLocationNearMousePointer();
		setLayout(new BorderLayout(8, 8));

		setJMenuBar(createMenuBar());
		add(createTopPanel(), BorderLayout.NORTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, datasetTreePanel,
				new DownloadManagerPanel(downloadManager, downloadService));
		splitPane.setResizeWeight(0.6);

		add(splitPane, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);

		refreshDownloadProgressStatus();
		autoLoadStartupInput();
	}

	private void setInitialLocationNearMousePointer() {
	    try {
	        java.awt.PointerInfo pointerInfo = java.awt.MouseInfo.getPointerInfo();
	        if (pointerInfo == null) {
	            setLocationRelativeTo(null);
	            return;
	        }

	        java.awt.Point mouse = pointerInfo.getLocation();
	        java.awt.GraphicsDevice targetDevice = null;

	        for (java.awt.GraphicsDevice device : java.awt.GraphicsEnvironment
	                .getLocalGraphicsEnvironment()
	                .getScreenDevices()) {

	            java.awt.GraphicsConfiguration config = device.getDefaultConfiguration();
	            java.awt.Rectangle bounds = config.getBounds();

	            if (bounds.contains(mouse)) {
	                targetDevice = device;
	                break;
	            }
	        }

	        if (targetDevice == null) {
	            setLocationRelativeTo(null);
	            return;
	        }

	        java.awt.GraphicsConfiguration config = targetDevice.getDefaultConfiguration();
	        java.awt.Rectangle bounds = config.getBounds();
	        java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);

	        int usableX = bounds.x + insets.left;
	        int usableY = bounds.y + insets.top;
	        int usableWidth = bounds.width - insets.left - insets.right;
	        int usableHeight = bounds.height - insets.top - insets.bottom;

	        int x = mouse.x - (getWidth() / 2);
	        int y = mouse.y - (getHeight() / 2);

	        x = Math.max(usableX, Math.min(x, usableX + usableWidth - getWidth()));
	        y = Math.max(usableY, Math.min(y, usableY + usableHeight - getHeight()));

	        setLocation(x, y);
	    } catch (Exception e) {
	        setLocationRelativeTo(null);
	    }
	}

	private void autoLoadStartupInput() {
		if (startupDatasetInput == null || startupDatasetInput.isBlank()) {
			return;
		}

		SwingUtilities.invokeLater(() -> {
			datasetField.getEditor().setItem(startupDatasetInput.trim());
			loadDataset();
		});
	}

	private void installDownloadStateRefresh() {
		downloadManager.addListener(task -> SwingUtilities.invokeLater(() -> datasetTreePanel.refreshDownloadState()));
	}

	private void installDownloadProgressRefresh() {
		downloadManager.addListener(task -> SwingUtilities.invokeLater(this::refreshDownloadProgressStatus));
	}

	private void refreshDownloadProgressStatus() {
		DownloadManager.DownloadStats stats = downloadManager.getDownloadStats();

		if (stats.isEmpty()) {
			statusBar.setMessage("Ready");
			return;
		}

		statusBar.setDownloadProgress(stats.getCompleted(), stats.getTotal(), stats.getRemaining(),
				stats.getPercentCompleted());
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
			private static final long serialVersionUID = -4526430372865962623L;

			@Override
			public void actionPerformed(ActionEvent e) {
				loadDataset();
			}
		}));

		buttonsPanel.add(new JButton(new AbstractAction("Download Selected") {
			private static final long serialVersionUID = 6492323721422057537L;

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
			JOptionPane.showMessageDialog(this, "Enter a dataset URL or DOI.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		preferencesService.recordDatasetUrl(datasetValue, preferences.getDatasetUrlHistorySize());
		refreshDatasetHistory();
		datasetField.getEditor().setItem(datasetValue);

		LoadingDialog loadingDialog = new LoadingDialog(this, "Loading dataset...");
		setUiBusy(true);
		statusBar.setMessage("Loading dataset...");

		SwingWorker<DatasetInfo, Runnable> worker = new SwingWorker<>() {
			@Override
			protected DatasetInfo doInBackground() throws Exception {
				return datasetService.loadDataset(preferences.getServerUrl(), preferences.getApiKey(), datasetValue,
						new DatasetLoadListener() {
							@Override
							public void onStatusChanged(DatasetLoadStatus previous, DatasetLoadStatus current) {
								publish(() -> loadingDialog.setTransition(previous, current));
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
								publish(() -> loadingDialog.setDetail(totalFiles > 0
										? "Loaded metadata for " + loadedFiles + " / " + totalFiles + " files"
										: "Loaded metadata for " + loadedFiles + " files"));
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
					datasetTreePanel.setDataset(currentDataset, destinationRoot(currentDataset));
					statusBar.setMessage("Loaded: " + currentDataset.getTitle() + " ("
							+ currentDataset.getFiles().size() + " files)");
				} catch (Exception e) {
					currentDataset = null;
					datasetTreePanel.setDataset(null, null);
					statusBar.setMessage("Load failed");

					Throwable cause = rootCause(e);
					String message = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
					cause.printStackTrace();

					boolean unsupportedRepository = isUnsupportedRepositoryMessage(message);

					JOptionPane.showMessageDialog(MainFrame.this, message,
							unsupportedRepository ? "Unsupported repository" : "Error",
							unsupportedRepository ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
				} finally {
					setUiBusy(false);
					loadingDialog.dispose();
					refreshDownloadProgressStatus();
				}
			}
		};

		worker.execute();
		loadingDialog.setVisible(true);
	}

	private void downloadSelected() {
		if (currentDataset == null) {
			JOptionPane.showMessageDialog(this, "Load a dataset first.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		final String effectiveServerUrl = currentDataset.getServerUrl() != null
				&& !currentDataset.getServerUrl().isBlank() ? currentDataset.getServerUrl()
						: preferences.getServerUrl();

		List<DownloadTask> tasks = datasetTreePanel
				.getSelectedFiles().stream().map(file -> new DownloadTask(file, currentDataset.getPersistentId(),
						effectiveServerUrl, preferences.getApiKey(), destinationRoot(currentDataset)))
				.collect(Collectors.toList());

		if (tasks.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Select at least one file or folder.", "Info",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if (!showLicenseAgreementIfNeeded(currentDataset)) {
			statusBar.setMessage("Download cancelled - license not accepted");
			return;
		}

		downloadService.queueDownloads(tasks);
		refreshDownloadProgressStatus();
	}

	private boolean showLicenseAgreementIfNeeded(DatasetInfo datasetInfo) {
		return DatasetTermsDialog.showDialog(this, datasetInfo);
	}

	private Path destinationRoot(DatasetInfo datasetInfo) {
		return preferences.getOutputFolder().resolve(datasetFolderName(datasetInfo));
	}

	private String datasetFolderName(DatasetInfo datasetInfo) {
		String pid = datasetInfo != null ? datasetInfo.getPersistentId() : null;
		if (pid != null && !pid.isBlank()) {
			return filesystemSafeDoi(pid);
		}

		String title = datasetInfo != null ? datasetInfo.getTitle() : null;
		if (title != null && !title.isBlank()) {
			return safeFolderName(title);
		}

		return "dataset";
	}

	private String filesystemSafeDoi(String value) {
		String normalized = value.trim();
		normalized = normalized.replace(':', '_');
		normalized = normalized.replace('/', '_');
		normalized = normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
		normalized = normalized.replaceAll("_+", "_");
		normalized = normalized.replaceAll("^[\\s._-]+", "");
		normalized = normalized.replaceAll("[\\s._-]+$", "");
		return normalized.isBlank() ? "dataset" : normalized;
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
			AppPreferences updatedPreferences = dialog.getValidatedPreferences();
			if (updatedPreferences == null) {
				throw new IllegalStateException("Preferences were not validated.");
			}

			preferences = updatedPreferences;
			preferencesService.save(preferences);
			statusBar.setDownloadFolder(preferences.getOutputFolder());
			refreshDatasetHistory();
			refreshDownloadProgressStatus();
			statusBar.setMessage("Preferences saved");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Could not save preferences: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			statusBar.setMessage("Preferences not saved");
		}
	}

	private void showAboutDialog() {
		AboutDialog dialog = new AboutDialog(this);
		dialog.setVisible(true);
	}

	private void setUiBusy(boolean busy) {
		setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());

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
		Object currentValue = datasetField.isEditable() ? datasetField.getEditor().getItem()
				: datasetField.getSelectedItem();

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

	private void installDatasetEditorContextMenu() {
		JTextComponent editor = getDatasetEditor();
		if (editor == null) {
			return;
		}

		MouseAdapter adapter = new MouseAdapter() {
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
				showDatasetEditorPopup(e.getComponent(), e.getX(), e.getY());
			}
		};

		editor.addMouseListener(adapter);
		datasetField.addMouseListener(adapter);
	}

	private void showDatasetEditorPopup(Component invoker, int x, int y) {
		JTextComponent editor = getDatasetEditor();
		if (editor == null) {
			return;
		}

		JPopupMenu popup = new JPopupMenu();

		JMenuItem pasteItem = new JMenuItem("Paste");
		pasteItem.addActionListener(e -> editor.paste());
		popup.add(pasteItem);

		JMenuItem pasteAndLoadItem = new JMenuItem("Paste and Load");
		pasteAndLoadItem.addActionListener(e -> {
			editor.paste();
			loadDataset();
		});
		popup.add(pasteAndLoadItem);

		JMenuItem pasteFromClipboardAndLoadItem = new JMenuItem("Paste DOI/URL from Clipboard and Load");
		pasteFromClipboardAndLoadItem.addActionListener(e -> {
			try {
				Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
				if (data != null) {
					editor.setText(data.toString().trim());
					loadDataset();
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Could not read clipboard: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		popup.add(pasteFromClipboardAndLoadItem);

		List<String> history = preferencesService.loadDatasetUrlHistory(preferences.getDatasetUrlHistorySize());
		if (!history.isEmpty()) {
			popup.addSeparator();

			for (String item : history) {
				JMenuItem historyItem = new JMenuItem(item);
				historyItem.addActionListener(e -> datasetField.getEditor().setItem(item));
				popup.add(historyItem);
			}

			popup.addSeparator();

			JMenuItem removeItem = new JMenuItem("Remove selected from history");
			removeItem.addActionListener(e -> removeSelectedHistoryEntry());
			popup.add(removeItem);

			JMenuItem clearItem = new JMenuItem("Clear history");
			clearItem.addActionListener(e -> clearDatasetHistory());
			popup.add(clearItem);
		}

		popup.show(invoker, x, y);
	}

	private JTextComponent getDatasetEditor() {
		Component editorComponent = datasetField.getEditor().getEditorComponent();
		return editorComponent instanceof JTextComponent textComponent ? textComponent : null;
	}

	private Throwable rootCause(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private boolean isUnsupportedRepositoryMessage(String message) {
		return message != null && message.startsWith(NON_DATAVERSE_MESSAGE_PREFIX);
	}

	private void setWindowIcon() {
		try {
			URL iconUrl = MainFrame.class.getResource("/logos/dataverse_min.png");
			if (iconUrl != null) {
				Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
				setIconImage(icon);
			}
		} catch (Exception ignored) {
			// Ignore icon loading failures.
		}
	}

	private static final class LoadingDialog extends JDialog {
		private static final long serialVersionUID = -3422256654292648919L;

		private final JLabel statusLabel = new JLabel("Starting...");
		private final JLabel transitionLabel = new JLabel(" ");
		private final JLabel detailLabel = new JLabel(" ");
		private final JProgressBar progressBar = new JProgressBar(0, 100);

		private LoadingDialog(Frame owner, String title) {
			super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);

			JPanel textPanel = new JPanel(new BorderLayout(0, 4));
			textPanel.add(statusLabel, BorderLayout.NORTH);
			textPanel.add(transitionLabel, BorderLayout.CENTER);
			textPanel.add(detailLabel, BorderLayout.SOUTH);

			JPanel panel = new JPanel(new BorderLayout(10, 10));
			panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
			panel.add(textPanel, BorderLayout.NORTH);
			panel.add(progressBar, BorderLayout.CENTER);

			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			setContentPane(panel);
			setSize(520, 170);
			setResizable(false);
			setLocationRelativeTo(owner);
		}

		void setTransition(DatasetLoadStatus previous, DatasetLoadStatus current) {
			setStatus(current == null ? "" : current.getDisplayMessage());

			if (previous == null) {
				transitionLabel.setText(" ");
				return;
			}

			transitionLabel.setText(
					"Previous: " + previous.getDisplayMessage()
							+ " → Current: " + (current == null ? "" : current.getDisplayMessage()));
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

	private void removeSelectedHistoryEntry() {
		String selected = getDatasetFieldText();
		if (selected.isBlank()) {
			statusBar.setMessage("No history entry selected");
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Remove this entry from history?\n\n" + selected,
				"Remove history entry", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

		if (result != JOptionPane.YES_OPTION) {
			return;
		}

		try {
			preferencesService.removeDatasetUrlHistoryEntry(selected);
			refreshDatasetHistory();
			datasetField.getEditor().setItem("");
			statusBar.setMessage("History entry removed");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Could not remove history entry: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			statusBar.setMessage("History entry not removed");
		}
	}

	private void clearDatasetHistory() {
		int result = JOptionPane.showConfirmDialog(this, "Clear the complete dataset URL history?", "Clear history",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (result != JOptionPane.YES_OPTION) {
			return;
		}

		try {
			preferencesService.clearDatasetUrlHistory();
			refreshDatasetHistory();
			datasetField.getEditor().setItem("");
			statusBar.setMessage("History cleared");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Could not clear history: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			statusBar.setMessage("History not cleared");
		}
	}
}