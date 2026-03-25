package com.example.dataverse.downloader.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumVerifier {

    public boolean verify(Path file, String algorithm, String expectedChecksum) throws IOException {
        if (algorithm == null || expectedChecksum == null || expectedChecksum.isBlank()) {
            return true;
        }
        return expectedChecksum.equalsIgnoreCase(compute(file, algorithm));
    }

    public String compute(Path file, String algorithm) throws IOException {
        MessageDigest digest = digest(algorithm);
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private MessageDigest digest(String algorithm) {
        String normalized = switch (algorithm.toUpperCase()) {
            case "MD5" -> "MD5";
            case "SHA1", "SHA-1" -> "SHA-1";
            case "SHA256", "SHA-256" -> "SHA-256";
            case "SHA512", "SHA-512" -> "SHA-512";
            default -> throw new IllegalArgumentException("Unsupported checksum algorithm: " + algorithm);
        };
        try {
            return MessageDigest.getInstance(normalized);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Checksum algorithm unavailable: " + normalized, e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
