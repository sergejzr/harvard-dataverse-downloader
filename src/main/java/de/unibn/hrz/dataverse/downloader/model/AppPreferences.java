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
package de.unibn.hrz.dataverse.downloader.model;

import java.nio.file.Path;

/**
 * Stores user-configurable application preferences.
 *
 * <p>This model contains the persistent settings required to connect to a
 * Dataverse installation and control local download behavior, such as the
 * target output folder, the number of parallel downloads, overwrite handling,
 * and the size of the stored dataset URL history.</p>
 *
 * <p>The class is intentionally kept as a simple data holder so that it can be
 * used easily by the persistence and user interface layers.</p>
 *
 * @author Sergej Zerr
 */
public class AppPreferences {

    /**
     * Base URL of the Dataverse installation to use by default.
     */
    private String serverUrl = "https://dataverse.harvard.edu";

    /**
     * Optional API key used for authenticated Dataverse requests.
     */
    private String apiKey = "";

    /**
     * Local target directory for downloaded dataset files.
     */
    private Path outputFolder;

    /**
     * Maximum number of files to download concurrently.
     */
    private int parallelDownloads = 3;

    /**
     * Strategy used when a target file already exists.
     */
    private String overwritePolicy = "SKIP";

    /**
     * Maximum number of previously used dataset URLs to retain.
     */
    private int datasetUrlHistorySize = 10;

    /**
     * Returns the default Dataverse server URL.
     *
     * @return the configured Dataverse base URL
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Sets the default Dataverse server URL.
     *
     * @param serverUrl the Dataverse base URL to store
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Returns the configured Dataverse API key.
     *
     * @return the API key, or an empty string if none is configured
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the Dataverse API key.
     *
     * @param apiKey the API key to store
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns the local output folder for downloads.
     *
     * @return the output folder, or {@code null} if none has been configured
     */
    public Path getOutputFolder() {
        return outputFolder;
    }

    /**
     * Sets the local output folder for downloads.
     *
     * @param outputFolder the output folder to store
     */
    public void setOutputFolder(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * Returns the configured number of parallel downloads.
     *
     * @return the maximum number of concurrent download tasks
     */
    public int getParallelDownloads() {
        return parallelDownloads;
    }

    /**
     * Sets the number of parallel downloads.
     *
     * @param parallelDownloads the maximum number of concurrent download tasks
     */
    public void setParallelDownloads(int parallelDownloads) {
        this.parallelDownloads = parallelDownloads;
    }

    /**
     * Returns the configured overwrite policy.
     *
     * @return the overwrite policy identifier
     */
    public String getOverwritePolicy() {
        return overwritePolicy;
    }

    /**
     * Sets the overwrite policy.
     *
     * @param overwritePolicy the overwrite policy identifier to store
     */
    public void setOverwritePolicy(String overwritePolicy) {
        this.overwritePolicy = overwritePolicy;
    }

    /**
     * Returns the maximum number of dataset URLs kept in history.
     *
     * @return the dataset URL history size
     */
    public int getDatasetUrlHistorySize() {
        return datasetUrlHistorySize;
    }

    /**
     * Sets the maximum number of dataset URLs kept in history.
     *
     * @param datasetUrlHistorySize the history size to store
     */
    public void setDatasetUrlHistorySize(int datasetUrlHistorySize) {
        this.datasetUrlHistorySize = datasetUrlHistorySize;
    }
}