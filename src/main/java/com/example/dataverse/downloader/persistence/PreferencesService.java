package com.example.dataverse.downloader.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.dataverse.downloader.model.AppPreferences;

public class PreferencesService {
    private static final String APP_DIR = ".dataverse-downloader";
    private static final String DB_NAME = "app.db";

    private final Path dbPath;

    public PreferencesService() {
        this.dbPath = Paths.get(System.getProperty("user.home"), APP_DIR, DB_NAME);
        initializeDatabase();
    }

    public AppPreferences load() {
        AppPreferences preferences = new AppPreferences();

        String sql = """
                SELECT server_url, api_key, output_folder, parallel_downloads, overwrite_policy, dataset_url_history_size
                FROM app_preferences
                WHERE id = 1
                """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            if (rs.next()) {
                preferences.setServerUrl(rs.getString("server_url"));
                preferences.setApiKey(nvl(rs.getString("api_key")));
                preferences.setOutputFolder(Paths.get(rs.getString("output_folder")));
                preferences.setParallelDownloads(rs.getInt("parallel_downloads"));
                preferences.setOverwritePolicy(rs.getString("overwrite_policy"));
                preferences.setDatasetUrlHistorySize(rs.getInt("dataset_url_history_size"));
            } else {
                save(preferences);
            }

            return preferences;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load preferences from SQLite", e);
        }
    }

    public void save(AppPreferences preferences) {
        String sql = """
                INSERT INTO app_preferences (
                    id, server_url, api_key, output_folder, parallel_downloads, overwrite_policy, dataset_url_history_size
                ) VALUES (1, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    server_url = excluded.server_url,
                    api_key = excluded.api_key,
                    output_folder = excluded.output_folder,
                    parallel_downloads = excluded.parallel_downloads,
                    overwrite_policy = excluded.overwrite_policy,
                    dataset_url_history_size = excluded.dataset_url_history_size
                """;

        try (Connection connection = connect();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, preferences.getServerUrl());
            ps.setString(2, nvl(preferences.getApiKey()));
            ps.setString(3, preferences.getOutputFolder().toString());
            ps.setInt(4, preferences.getParallelDownloads());
            ps.setString(5, preferences.getOverwritePolicy());
            ps.setInt(6, preferences.getDatasetUrlHistorySize());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Could not save preferences to SQLite", e);
        }
    }

    public List<String> loadDatasetUrlHistory(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        String sql = """
                SELECT url
                FROM dataset_url_history
                ORDER BY last_used_at DESC
                LIMIT ?
                """;

        List<String> result = new ArrayList<>();

        try (Connection connection = connect();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("url"));
                }
            }

            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load dataset URL history", e);
        }
    }

    public void recordDatasetUrl(String url, int maxEntries) {
        String normalized = normalizeHistoryUrl(url);
        if (normalized.isBlank() || maxEntries <= 0) {
            return;
        }

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement upsert = connection.prepareStatement("""
                    INSERT INTO dataset_url_history (url, last_used_at)
                    VALUES (?, ?)
                    ON CONFLICT(url) DO UPDATE SET
                        last_used_at = excluded.last_used_at
                    """)) {
                upsert.setString(1, normalized);
                upsert.setLong(2, System.currentTimeMillis());
                upsert.executeUpdate();
            }

            trimHistory(connection, maxEntries);
            connection.commit();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update dataset URL history", e);
        }
    }

    private void trimHistory(Connection connection, int maxEntries) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM dataset_url_history
                WHERE url IN (
                    SELECT url
                    FROM dataset_url_history
                    ORDER BY last_used_at DESC
                    LIMIT -1 OFFSET ?
                )
                """)) {
            ps.setInt(1, maxEntries);
            ps.executeUpdate();
        }
    }

    private void initializeDatabase() {
        try {
            Files.createDirectories(dbPath.getParent());
            try (Connection connection = connect();
                 Statement statement = connection.createStatement()) {

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS app_preferences (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            server_url TEXT NOT NULL,
                            api_key TEXT,
                            output_folder TEXT NOT NULL,
                            parallel_downloads INTEGER NOT NULL,
                            overwrite_policy TEXT NOT NULL,
                            dataset_url_history_size INTEGER NOT NULL DEFAULT 10
                        )
                        """);

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS dataset_url_history (
                            url TEXT PRIMARY KEY,
                            last_used_at INTEGER NOT NULL
                        )
                        """);

                try {
                    statement.execute("""
                            ALTER TABLE app_preferences
                            ADD COLUMN dataset_url_history_size INTEGER NOT NULL DEFAULT 10
                            """);
                } catch (Exception ignored) {
                    // Column already exists.
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize SQLite database", e);
        }
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String normalizeHistoryUrl(String value) {
        return value == null ? "" : value.trim();
    }
}