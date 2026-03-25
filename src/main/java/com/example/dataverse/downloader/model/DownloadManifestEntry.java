package com.example.dataverse.downloader.model;

public class DownloadManifestEntry {
    private long fileId;
    private String relativePath;
    private long bytesDownloaded;
    private String checksumAlgorithm;
    private String checksumValue;
    private String status;

    public long getFileId() { return fileId; }
    public void setFileId(long fileId) { this.fileId = fileId; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public void setBytesDownloaded(long bytesDownloaded) { this.bytesDownloaded = bytesDownloaded; }
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
    public String getChecksumValue() { return checksumValue; }
    public void setChecksumValue(String checksumValue) { this.checksumValue = checksumValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
