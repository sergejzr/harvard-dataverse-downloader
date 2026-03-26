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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.unibn.hrz.dataverse.downloader.model.DatasetFileEntry;
import de.unibn.hrz.dataverse.downloader.model.DatasetInfo;
/**
 * Loads dataset metadata and file information from a Dataverse repository.
 *
 * <p>This service resolves the dataset identifier, determines the file count,
 * loads file metadata page by page, and reports progress through a listener.</p>
 *
 * @author Sergej Zerr
 */
public class DatasetService {
    private static final Logger LOG = AppLogger.getLogger(DatasetService.class);
    private static final int PAGE_SIZE = 500;

    private final DataverseApiClient apiClient = new DataverseApiClient();

    public DatasetInfo loadDataset(
            String serverUrl,
            String apiKey,
            String urlOrDoi,
            DatasetLoadListener listener)
            throws IOException, InterruptedException {

        DatasetLoadListener effectiveListener = listener != null ? listener : new NoOpDatasetLoadListener();

        long started = System.nanoTime();
        LOG.info("Starting dataset load for input: " + urlOrDoi);

        String effectiveServerUrl = apiClient.resolveServerUrl(serverUrl, urlOrDoi);
        LOG.info("Using Dataverse host: " + effectiveServerUrl);

        try {
            effectiveListener.onStatus("Resolving dataset...");
            effectiveListener.onProgress(5);

            long t1 = System.nanoTime();
            DataverseApiClient.DatasetRef ref = apiClient.resolveDatasetRefFast(effectiveServerUrl, urlOrDoi, apiKey);
            LOG.info("Resolved dataset ref in " + elapsedMs(t1) + " ms: id=" + ref.getDatasetId()
                    + ", pid=" + ref.getPersistentId()+", title="+ref.getTitle());

            DatasetInfo info = new DatasetInfo();
            info.setDatasetId(ref.getDatasetId());
            info.setPersistentId(ref.getPersistentId());
            info.setTitle(ref.getTitle());
            info.setServerUrl(effectiveServerUrl);

            effectiveListener.onStatus("Fetching file count...");
            effectiveListener.onProgress(15);

            String version = apiClient.defaultVersion();

            long t2 = System.nanoTime();
            int totalFiles = apiClient.fetchFileCount(effectiveServerUrl, info.getDatasetId(), version, apiKey);
            LOG.info("Fetched file count in " + elapsedMs(t2) + " ms: totalFiles=" + totalFiles);

            effectiveListener.onFileCountDiscovered(totalFiles);

            if (totalFiles <= 0) {
                effectiveListener.onStatus("Fetching dataset title...");
                effectiveListener.onProgress(95);

               // fillDatasetTitleFast(info);

                info.setFiles(new ArrayList<>());
                effectiveListener.onStatus("Finished");
                effectiveListener.onProgress(100);
                LOG.info("Dataset load finished with zero files in " + elapsedMs(started) + " ms");
                return info;
            }

            effectiveListener.onStatus("Loading file metadata...");
            effectiveListener.onProgress(20);

            long t3 = System.nanoTime();
            List<DatasetFileEntry> files = loadFilesPaged(
                    effectiveServerUrl,
                    apiKey,
                    info.getDatasetId(),
                    version,
                    totalFiles,
                    effectiveListener);
            info.setFiles(files);

            LOG.info("Loaded " + files.size() + " file metadata rows in " + elapsedMs(t3) + " ms");

            effectiveListener.onStatus("Fetching dataset title...");
            effectiveListener.onProgress(95);

            long t4 = System.nanoTime();
            //fillDatasetTitle(effectiveServerUrl, apiKey, urlOrDoi, info);
            //fillDatasetTitleFast(info);
            LOG.info("Fetched dataset title in " + elapsedMs(t4) + " ms: title=" + info.getTitle());

            effectiveListener.onStatus("Finished");
            effectiveListener.onProgress(100);

            LOG.info("Dataset load finished in " + elapsedMs(started) + " ms");
            return info;
        } catch (IOException | InterruptedException e) {
            LOG.severe("Dataset load failed: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            LOG.severe("Dataset load failed with runtime error: " + e.getMessage());
            throw e;
        }
    }

    private List<DatasetFileEntry> loadFilesPaged(
            String serverUrl,
            String apiKey,
            long datasetId,
            String version,
            int totalFiles,
            DatasetLoadListener listener)
            throws IOException, InterruptedException {

        List<DatasetFileEntry> result = new ArrayList<>();
        int offset = 0;
        int effectiveTotal = totalFiles > 0 ? totalFiles : -1;
        int pageNumber = 0;

        while (true) {
            pageNumber++;
            long pageStart = System.nanoTime();

            DataverseApiClient.FilePage page =
                    apiClient.fetchFilesPage(serverUrl, datasetId, version, PAGE_SIZE, offset, apiKey);

            JsonArray items = page.getItems();
            if (items == null || items.isEmpty()) {
                LOG.info("Stopping file paging: empty page at offset=" + offset);
                break;
            }

            int pageLoaded = 0;
            int pageSkipped = 0;

            for (JsonElement fileElement : items) {
                if (!fileElement.isJsonObject()) {
                    pageSkipped++;
                    continue;
                }

                DatasetFileEntry entry = toFileEntry(fileElement.getAsJsonObject());
                if (entry != null) {
                    result.add(entry);
                    pageLoaded++;
                } else {
                    pageSkipped++;
                }
            }

            int discoveredTotal = page.getTotalCount();
            if (effectiveTotal <= 0 && discoveredTotal > 0) {
                effectiveTotal = discoveredTotal;
                listener.onFileCountDiscovered(effectiveTotal);
            }

            listener.onFilesLoaded(result.size(), effectiveTotal > 0 ? effectiveTotal : result.size());

            int progress = effectiveTotal > 0
                    ? 20 + (int) Math.min(70, Math.round((result.size() * 70.0) / effectiveTotal))
                    : 20 + Math.min(70, pageNumber * 5);
            listener.onProgress(progress);

            LOG.info("Loaded page " + pageNumber
                    + " in " + elapsedMs(pageStart) + " ms"
                    + ", pageItems=" + items.size()
                    + ", pageLoaded=" + pageLoaded
                    + ", pageSkipped=" + pageSkipped
                    + ", accumulated=" + result.size());

            offset += items.size();

            if (effectiveTotal > 0 && result.size() >= effectiveTotal) {
                LOG.info("Stopping file paging: reached expected total");
                break;
            }

            if (items.size() < PAGE_SIZE) {
                LOG.info("Stopping file paging: page smaller than page size");
                break;
            }

            if (pageLoaded == 0) {
                LOG.warning("Stopping file paging: page contained no usable file entries");
                break;
            }
        }

        return result;
    }

    private void fillDatasetTitle(String serverUrl, String apiKey, String urlOrDoi, DatasetInfo info)
            throws IOException, InterruptedException {
        try {
            JsonObject dataset = apiClient.fetchDatasetJson(serverUrl, urlOrDoi, apiKey);

            long fetchedId = extractDatasetId(dataset);
            if (info.getDatasetId() == 0L) {
                info.setDatasetId(fetchedId);
            }

            String fetchedPid = extractPersistentId(dataset);
            if (info.getPersistentId() == null || info.getPersistentId().isBlank()) {
                info.setPersistentId(fetchedPid);
            }

            String title = extractTitle(dataset);
            if (title != null && !title.isBlank()) {
                info.setTitle(title);
            }
        } catch (Exception e) {
            LOG.warning("Could not fetch full dataset title; using fallback title. Reason: " + e.getMessage());
            if (info.getTitle() == null || info.getTitle().isBlank()) {
                if (info.getPersistentId() != null && !info.getPersistentId().isBlank()) {
                    info.setTitle(info.getPersistentId());
                } else {
                    info.setTitle("dataset");
                }
            }
        }
    }
    private void fillDatasetTitleFast(DatasetInfo info)
          {}
    private long extractDatasetId(JsonObject dataset) {
        if (dataset.has("id") && !dataset.get("id").isJsonNull()) {
            return dataset.get("id").getAsLong();
        }
        throw new IllegalStateException("Could not find dataset id in API response.");
    }

    private String extractTitle(JsonObject dataset) {
        if (dataset.has("latestVersion") && dataset.get("latestVersion").isJsonObject()) {
            JsonObject latestVersion = dataset.getAsJsonObject("latestVersion");
            if (latestVersion.has("metadataBlocks") && latestVersion.get("metadataBlocks").isJsonObject()) {
                JsonObject metadataBlocks = latestVersion.getAsJsonObject("metadataBlocks");
                for (String key : metadataBlocks.keySet()) {
                    JsonObject block = metadataBlocks.getAsJsonObject(key);
                    if (block == null || !block.has("fields") || !block.get("fields").isJsonArray()) {
                        continue;
                    }

                    JsonArray fields = block.getAsJsonArray("fields");
                    for (JsonElement fieldElement : fields) {
                        if (!fieldElement.isJsonObject()) {
                            continue;
                        }

                        JsonObject field = fieldElement.getAsJsonObject();
                        if (field.has("typeName")
                                && !field.get("typeName").isJsonNull()
                                && "title".equals(field.get("typeName").getAsString())
                                && field.has("value")
                                && !field.get("value").isJsonNull()) {
                            return field.get("value").getAsString();
                        }
                    }
                }
            }
        }

        if (dataset.has("persistentId") && !dataset.get("persistentId").isJsonNull()) {
            return dataset.get("persistentId").getAsString();
        }

        return "dataset";
    }

    private String extractPersistentId(JsonObject dataset) {
        if (dataset.has("persistentId") && !dataset.get("persistentId").isJsonNull()) {
            return dataset.get("persistentId").getAsString();
        }
        throw new IllegalStateException("Could not find dataset persistent identifier in API response.");
    }

    private DatasetFileEntry toFileEntry(JsonObject item) {
        if (item == null || !item.has("dataFile") || !item.get("dataFile").isJsonObject()) {
            return null;
        }

        JsonObject dataFile = item.getAsJsonObject("dataFile");

        if (!dataFile.has("id") || dataFile.get("id").isJsonNull()
                || !dataFile.has("filename") || dataFile.get("filename").isJsonNull()) {
            return null;
        }

        DatasetFileEntry entry = new DatasetFileEntry();
        entry.setFileId(dataFile.get("id").getAsLong());
        entry.setName(dataFile.get("filename").getAsString());
        entry.setSize(dataFile.has("filesize") && !dataFile.get("filesize").isJsonNull()
                ? dataFile.get("filesize").getAsLong()
                : 0L);

        if (dataFile.has("checksum") && dataFile.get("checksum").isJsonObject()) {
            JsonObject checksum = dataFile.getAsJsonObject("checksum");
            entry.setChecksumAlgorithm(checksum.has("type") && !checksum.get("type").isJsonNull()
                    ? checksum.get("type").getAsString()
                    : null);
            entry.setChecksumValue(checksum.has("value") && !checksum.get("value").isJsonNull()
                    ? checksum.get("value").getAsString()
                    : null);
        }

        entry.setPath(item.has("directoryLabel") && !item.get("directoryLabel").isJsonNull()
                ? item.get("directoryLabel").getAsString()
                : "");

        return entry;
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private static final class NoOpDatasetLoadListener implements DatasetLoadListener {
        @Override
        public void onStatus(String message) {
        }

        @Override
        public void onProgress(int percent) {
        }
    }
}