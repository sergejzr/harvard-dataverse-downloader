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
