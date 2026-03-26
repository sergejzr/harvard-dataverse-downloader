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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parses startup arguments for the Dataverse Downloader.
 *
 * <p>Supported forms:</p>
 *
 * <ul>
 *   <li>a direct dataset URL,</li>
 *   <li>a direct DOI value,</li>
 *   <li>a custom deep link such as
 *       {@code hvdvdl://open?url=https%3A%2F%2F...},</li>
 *   <li>a custom deep link such as
 *       {@code hvdvdl://open?doi=doi%3A10.7910%2FDVN%2F...}.</li>
 * </ul>
 */
public final class StartupLinkParser {

    private static final String CUSTOM_SCHEME_PREFIX = "hvdvdl://";

    private StartupLinkParser() {
    }

    /**
     * Parses the startup input from the application argument list.
     *
     * @param args command-line arguments
     * @return the decoded dataset URL or DOI to load, or {@code null} if none
     *         could be extracted
     */
    public static String parseStartupInput(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (String arg : args) {
            String parsed = parseSingleArgument(arg);
            if (parsed != null && !parsed.isBlank()) {
                return parsed;
            }
        }

        return null;
    }

    private static String parseSingleArgument(String arg) {
        if (arg == null) {
            return null;
        }

        String trimmed = arg.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        if (!trimmed.regionMatches(true, 0, CUSTOM_SCHEME_PREFIX, 0, CUSTOM_SCHEME_PREFIX.length())) {
            return trimmed;
        }

        return parseCustomScheme(trimmed);
    }

    private static String parseCustomScheme(String value) {
        try {
            URI uri = URI.create(value);
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return null;
            }

            for (String part : query.split("&")) {
                int idx = part.indexOf('=');
                if (idx <= 0) {
                    continue;
                }

                String key = part.substring(0, idx);
                String rawValue = part.substring(idx + 1);

                if ("url".equalsIgnoreCase(key) || "doi".equalsIgnoreCase(key)) {
                    String decoded = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                    return decoded == null || decoded.isBlank() ? null : decoded.trim();
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed startup URLs and fall back to no startup input.
        }

        return null;
    }
}