package com.example.dataverse.downloader;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.example.dataverse.downloader.model.AppPreferences;
import com.example.dataverse.downloader.persistence.PreferencesService;
import com.example.dataverse.downloader.service.AppLogger;
import com.example.dataverse.downloader.service.DatasetService;
import com.example.dataverse.downloader.service.DownloadManager;
import com.example.dataverse.downloader.service.DownloadService;
import com.example.dataverse.downloader.ui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

public final class App {
    private static final Logger LOG = AppLogger.getLogger(App.class);

    private App() {
    }

    public static void main(String[] args) {
        AppLogger.init();
        LOG.info("Starting Dataverse Downloader");

        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            PreferencesService preferencesService = new PreferencesService();
            AppPreferences preferences = preferencesService.load();

            DownloadManager downloadManager = new DownloadManager();
            DownloadService downloadService =
                    new DownloadService(downloadManager, preferences.getParallelDownloads());

            DatasetService datasetService = new DatasetService();

            MainFrame frame = new MainFrame(
                    preferencesService,
                    datasetService,
                    downloadService,
                    downloadManager);
            frame.setVisible(true);

            LOG.info("Main window initialized");
        });
    }
}