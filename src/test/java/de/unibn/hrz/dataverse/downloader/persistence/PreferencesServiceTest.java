package de.unibn.hrz.dataverse.downloader.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.unibn.hrz.dataverse.downloader.model.AppPreferences;

class PreferencesServiceTest {

    @TempDir
    Path tempDir;

    private Path dbPath;
    private PreferencesService service;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("prefs.db");
        service = new PreferencesService(dbPath);
    }

    @Test
    void hasStoredPreferencesIsFalseInitially() {
        assertFalse(service.hasStoredPreferences());
    }

    @Test
    void loadReturnsDefaultsWhenNothingStored() {
        AppPreferences prefs = service.load();

        assertNotNull(prefs);
        assertEquals("https://dataverse.harvard.edu", prefs.getServerUrl());
        assertEquals("", prefs.getApiKey());
        assertNull(prefs.getOutputFolder());
        assertEquals(3, prefs.getParallelDownloads());
        assertEquals("SKIP", prefs.getOverwritePolicy());
        assertEquals(10, prefs.getDatasetUrlHistorySize());
    }

    @Test
    void saveThenLoadRoundTripPreservesPreferences() throws Exception {
        Path outputFolder = Files.createDirectories(tempDir.resolve("downloads"));

        AppPreferences prefs = new AppPreferences();
        prefs.setServerUrl("https://bonndata.uni-bonn.de");
        prefs.setApiKey("secret-key");
        prefs.setOutputFolder(outputFolder);
        prefs.setParallelDownloads(5);
        prefs.setOverwritePolicy("OVERWRITE");
        prefs.setDatasetUrlHistorySize(25);

        service.save(prefs);

        assertTrue(service.hasStoredPreferences());

        AppPreferences loaded = service.load();
        assertEquals("https://bonndata.uni-bonn.de", loaded.getServerUrl());
        assertEquals("secret-key", loaded.getApiKey());
        assertEquals(outputFolder, loaded.getOutputFolder());
        assertEquals(5, loaded.getParallelDownloads());
        assertEquals("OVERWRITE", loaded.getOverwritePolicy());
        assertEquals(25, loaded.getDatasetUrlHistorySize());
    }

    @Test
    void saveRejectsNullOutputFolder() {
        AppPreferences prefs = new AppPreferences();
        prefs.setOutputFolder(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.save(prefs));

        assertEquals("Output folder must not be null", ex.getMessage());
    }

    @Test
    void loadDatasetUrlHistoryReturnsEmptyListWhenLimitIsZeroOrLess() {
        assertTrue(service.loadDatasetUrlHistory(0).isEmpty());
        assertTrue(service.loadDatasetUrlHistory(-1).isEmpty());
    }

    @Test
    void loadDatasetUrlHistoryReturnsMostRecentEntriesUpToLimit() throws Exception {
        insertHistory("https://example.org/dataset/1", "2026-04-15 10:00:00");
        insertHistory("https://example.org/dataset/2", "2026-04-15 11:00:00");
        insertHistory("https://example.org/dataset/3", "2026-04-15 12:00:00");

        List<String> history = service.loadDatasetUrlHistory(2);

        assertIterableEquals(
                List.of(
                        "https://example.org/dataset/3",
                        "https://example.org/dataset/2"),
                history);
    }

    private void insertHistory(String url, String timestamp) throws Exception {
        String sql = """
                INSERT INTO dataset_url_history (url, last_used_at)
                VALUES (?, ?)
                """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setString(2, timestamp);
            ps.executeUpdate();
        }
    }
}