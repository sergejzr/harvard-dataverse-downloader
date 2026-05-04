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
package de.unibn.hrz.dataverse.downloader.service;

/**
 * Stable lifecycle states for dataset loading.
 *
 * <p>Each enum constant provides the human-readable message that should be
 * shown in the user interface.</p>
 *
 * @author Sergej Zerr
 */
public enum DatasetLoadStatus {
    RESOLVING_DATASET("Resolving dataset..."),
    FETCHING_FILE_COUNT("Fetching file count..."),
    LOADING_FILE_METADATA("Loading file metadata..."),
    FETCHING_DATASET_TITLE("Fetching dataset title..."),
    FINISHED("Finished"),
    FAILED("Failed");

    private final String displayMessage;

    DatasetLoadStatus(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }
}
