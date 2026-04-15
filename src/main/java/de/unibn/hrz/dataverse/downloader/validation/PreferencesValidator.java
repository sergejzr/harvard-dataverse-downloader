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
package de.unibn.hrz.dataverse.downloader.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import de.unibn.hrz.dataverse.downloader.model.AppPreferences;

/**
 * Validates user-editable application preferences.
 *
 * <p>This class centralizes all validation rules for the preferences dialog so
 * that invalid values are rejected consistently before they are persisted.</p>
 */
public final class PreferencesValidator {

    private static final int MIN_PARALLEL_DOWNLOADS = 1;
    private static final int MAX_PARALLEL_DOWNLOADS = 3;
    private static final int MIN_HISTORY_SIZE = 0;
    private static final int MAX_HISTORY_SIZE = 50;
    private static final Set<String> ALLOWED_OVERWRITE_POLICIES =
            Set.of("SKIP", "OVERWRITE");

    private PreferencesValidator() {
    }

    /**
     * Validates the given preferences instance.
     *
     * @param preferences preferences to validate
     * @throws IllegalArgumentException if any field is invalid
     */
    public static void validate(AppPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("Preferences must not be null.");
        }

        validateServerUrl(preferences.getServerUrl());
        validateOutputFolder(preferences.getOutputFolder());
        validateParallelDownloads(preferences.getParallelDownloads());
        validateDatasetUrlHistorySize(preferences.getDatasetUrlHistorySize());
        validateOverwritePolicy(preferences.getOverwritePolicy());
    }

    private static void validateServerUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("Dataverse host URL is required.");
        }

        try {
            URI uri = new URI(serverUrl.trim());

            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(
                        "Dataverse host URL must start with http:// or https://");
            }

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException(
                        "Dataverse host URL must contain a valid host name.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Dataverse host URL is invalid.");
        }
    }

    private static void validateOutputFolder(Path outputFolder) {
        if (outputFolder == null) {
            throw new IllegalArgumentException("Output folder is required.");
        }

        if (!Files.exists(outputFolder)) {
            throw new IllegalArgumentException("Output folder does not exist.");
        }

        if (!Files.isDirectory(outputFolder)) {
            throw new IllegalArgumentException("Output folder must be a directory.");
        }

        if (!Files.isWritable(outputFolder)) {
            throw new IllegalArgumentException("Output folder is not writable.");
        }
    }

    private static void validateParallelDownloads(int parallelDownloads) {
        if (parallelDownloads < MIN_PARALLEL_DOWNLOADS) {
            throw new IllegalArgumentException(
                    "Parallel downloads must be at least " + MIN_PARALLEL_DOWNLOADS + ".");
        }

        if (parallelDownloads > MAX_PARALLEL_DOWNLOADS) {
            throw new IllegalArgumentException(
                    "Parallel downloads must not be greater than "
                            + MAX_PARALLEL_DOWNLOADS + ".");
        }
    }

    private static void validateDatasetUrlHistorySize(int datasetUrlHistorySize) {
        if (datasetUrlHistorySize < MIN_HISTORY_SIZE) {
            throw new IllegalArgumentException(
                    "Dataset URL history size must be " + MIN_HISTORY_SIZE + " or greater.");
        }

        if (datasetUrlHistorySize > MAX_HISTORY_SIZE) {
            throw new IllegalArgumentException(
                    "Dataset URL history size must not be greater than "
                            + MAX_HISTORY_SIZE + ".");
        }
    }

    private static void validateOverwritePolicy(String overwritePolicy) {
        if (overwritePolicy == null || !ALLOWED_OVERWRITE_POLICIES.contains(overwritePolicy)) {
            throw new IllegalArgumentException("Overwrite policy is invalid.");
        }
    }
}