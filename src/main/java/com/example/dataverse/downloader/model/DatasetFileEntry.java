package com.example.dataverse.downloader.model;

import java.util.Objects;

public class DatasetFileEntry {
    private long fileId;
    private String name;
    private String path;
    private long size;
    private String checksumAlgorithm;
    private String checksumValue;
    private boolean directory;

    public DatasetFileEntry() {
    }

    public DatasetFileEntry(long fileId, String name, String path, long size, String checksumAlgorithm, String checksumValue,
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

    public String getRelativePath() {
        return path == null || path.isBlank() ? name : path + "/" + name;
    }

    @Override
    public String toString() {
        return directory ? name + "/" : name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, path, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DatasetFileEntry other)) return false;
        return fileId == other.fileId && Objects.equals(path, other.path) && Objects.equals(name, other.name);
    }
}
