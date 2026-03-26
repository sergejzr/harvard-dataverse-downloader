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
package de.unibn.hrz.dataverse.downloader;

import java.nio.file.Path;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import de.unibn.hrz.dataverse.downloader.model.AppPreferences;
import de.unibn.hrz.dataverse.downloader.persistence.PreferencesService;
import de.unibn.hrz.dataverse.downloader.service.AppLogger;
import de.unibn.hrz.dataverse.downloader.service.DatasetService;
import de.unibn.hrz.dataverse.downloader.service.DownloadManager;
import de.unibn.hrz.dataverse.downloader.service.DownloadService;
import de.unibn.hrz.dataverse.downloader.service.StartupLinkParser;
import de.unibn.hrz.dataverse.downloader.ui.MainFrame;

/**
 * Main entry point for the Dataverse Downloader desktop application.
 *
 * <p>This bootstrap class performs the minimal startup sequence required to
 * launch the Swing user interface:</p>
 *
 * <ul>
 *   <li>initializes application-wide logging,</li>
 *   <li>applies the FlatLaf light look and feel,</li>
 *   <li>loads persisted user preferences,</li>
 *   <li>forces selection of a download folder on first start,</li>
 *   <li>creates the core service objects, and</li>
 *   <li>opens the main application window.</li>
 * </ul>
 *
 * <p>The actual user interface is created on the Swing Event Dispatch Thread
 * to keep Swing usage thread-safe.</p>
 *
 * @author Sergej Zerr
 */
public final class App {

    /**
     * Application logger used for startup and lifecycle messages.
     */
    private static final Logger LOG = AppLogger.getLogger(App.class);

    /**
     * Utility class; not intended to be instantiated.
     */
    private App() {
    }

    /**
     * Starts the Dataverse Downloader application.
     *
     * <p>The startup sequence initializes logging and the look and feel
     * immediately, then defers all Swing-related initialization to the
     * Event Dispatch Thread.</p>
     *
     * @param args command-line arguments; may contain a dataset URL, DOI,
     *             or a custom deep-link such as hvdvdl://open?url=...
     */
    public static void main(String[] args) {
        AppLogger.init();
        LOG.info("Starting Dataverse Downloader");

        configureFlatLafForRobustStartup();
        FlatLightLaf.setup();

        String startupInput = StartupLinkParser.parseStartupInput(args);

        SwingUtilities.invokeLater(() -> {
            PreferencesService preferencesService = new PreferencesService();
            boolean firstRun = !preferencesService.hasStoredPreferences();
            AppPreferences preferences = preferencesService.load();

            /*
             * A valid output folder is required before the main window can be
             * used for downloads. On first run, or when no folder is stored,
             * prompt the user to choose one and persist the selection.
             */
            if (firstRun || preferences.getOutputFolder() == null) {
                Path chosenFolder = chooseInitialDownloadFolder(preferences.getOutputFolder());
                if (chosenFolder == null) {
                    JOptionPane.showMessageDialog(
                            null,
                            "A download folder is required on first start.",
                            "Download folder required",
                            JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                    return;
                }

                preferences.setOutputFolder(chosenFolder);
                preferencesService.save(preferences);
            }

            /*
             * Create the core application services. The download manager keeps
             * track of download tasks, while the download service executes them
             * using the configured degree of parallelism.
             */
            DownloadManager downloadManager = new DownloadManager();
            DownloadService downloadService =
                    new DownloadService(downloadManager, preferences.getParallelDownloads());
            DatasetService datasetService = new DatasetService();

            MainFrame frame = new MainFrame(
                    preferencesService,
                    datasetService,
                    downloadService,
                    downloadManager,
                    startupInput);
            frame.setVisible(true);

            LOG.info("Main window initialized");
        });
    }

    /**
     * Configures FlatLaf in a conservative way to avoid startup issues when the
     * application is launched from network/UNC paths or virtual shared folders.
     *
     * <p>These properties disable FlatLaf features that rely on loading or
     * locating native helper libraries for custom window decorations.</p>
     */
    private static void configureFlatLafForRobustStartup() {
        System.setProperty("flatlaf.useWindowDecorations", "false");
        System.setProperty("flatlaf.useNativeLibrary", "false");
    }

    /**
     * Opens a directory chooser for selecting the initial download folder.
     *
     * <p>This method is used during application startup when no output folder
     * has been configured yet. Only directories can be selected.</p>
     *
     * @param initialFolder the folder to preselect when opening the chooser;
     *        may be {@code null}
     * @return the selected folder as a path, or {@code null} if the user
     *         cancels the dialog
     */
    private static Path chooseInitialDownloadFolder(Path initialFolder) {
        JFileChooser chooser = new JFileChooser(
                initialFolder != null ? initialFolder.toFile() : null);
        chooser.setDialogTitle("Choose download folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }
}