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

import java.util.Objects;

/**
 * Represents a single file or directory entry within a Dataverse dataset.
 *
 * <p>This model is populated from Dataverse API responses and used throughout
 * the application for:</p>
 *
 * <ul>
 *   <li>displaying dataset contents in the UI,</li>
 *   <li>constructing download paths,</li>
 *   <li>verifying file integrity via checksums, and</li>
 *   <li>tracking file identity during processing.</li>
 * </ul>
 *
 * <p>A dataset entry may represent either a file or a directory. Directories
 * are virtual constructs derived from Dataverse metadata and are indicated
 * via {@link #isDirectory()}.</p>
 *
 * <p>Equality is based on the Dataverse file identifier together with its
 * path and name to ensure stable identity across reloads.</p>
 *
 * @author Sergej Zerr
 */
public class DatasetFileEntry {

    /**
     * Unique Dataverse file identifier.
     */
    private long fileId;

    /**
     * File or directory name (without path).
     */
    private String name;

    /**
     * Relative directory path inside the dataset (may be {@code null} or empty).
     */
    private String path;

    /**
     * File size in bytes. For directories, this is typically {@code 0}.
     */
    private long size;

    /**
     * Checksum algorithm (e.g., MD5, SHA-1) as provided by Dataverse.
     */
    private String checksumAlgorithm;

    /**
     * Checksum value used for integrity verification after download.
     */
    private String checksumValue;

    /**
     * Indicates whether this entry represents a directory instead of a file.
     */
    private boolean directory;

    /**
     * Default constructor for frameworks and deserialization.
     */
    public DatasetFileEntry() {
    }

    /**
     * Creates a fully initialized dataset file entry.
     *
     * @param fileId unique Dataverse file identifier
     * @param name file or directory name
     * @param path relative directory path inside the dataset
     * @param size file size in bytes
     * @param checksumAlgorithm checksum algorithm
     * @param checksumValue checksum value
     * @param directory whether this entry represents a directory
     */
    public DatasetFileEntry(
            long fileId,
            String name,
            String path,
            long size,
            String checksumAlgorithm,
            String checksumValue,
            boolean directory) {

        this.fileId = fileId;
        this.name = name;
        this.path = path;
        this.size = size;
        this.checksumAlgorithm = checksumAlgorithm;
        this.checksumValue = checksumValue;
        this.directory = directory;
    }

    public long getFileId() { return fileId; }
    public void setFileId(long fileId) { this.fileId = fileId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }

    public String getChecksumValue() { return checksumValue; }
    public void setChecksumValue(String checksumValue) { this.checksumValue = checksumValue; }

    public boolean isDirectory() { return directory; }
    public void setDirectory(boolean directory) { this.directory = directory; }

    /**
     * Returns the relative path of this entry including its file name.
     *
     * <p>If no path is defined, the file name alone is returned. Otherwise,
     * the path and name are concatenated using a forward slash.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code name="file.txt", path=null -> "file.txt"}</li>
     *   <li>{@code name="file.txt", path="data" -> "data/file.txt"}</li>
     * </ul>
     *
     * @return relative path including the file name
     */
    public String getRelativePath() {
        return path == null || path.isBlank() ? name : path + "/" + name;
    }

    /**
     * Returns a human-readable representation for UI display.
     *
     * <p>Directories are suffixed with "/" to distinguish them visually.</p>
     *
     * @return display name of the entry
     */
    @Override
    public String toString() {
        return directory ? name + "/" : name;
    }

    /**
     * Computes a hash code based on stable identity fields.
     *
     * @return hash code for this entry
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileId, path, name);
    }

    /**
     * Compares this entry to another for equality.
     *
     * <p>Two entries are considered equal if they share the same Dataverse
     * file identifier, path, and name.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if both objects represent the same dataset entry
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DatasetFileEntry other)) return false;
        return fileId == other.fileId
                && Objects.equals(path, other.path)
                && Objects.equals(name, other.name);
    }
}