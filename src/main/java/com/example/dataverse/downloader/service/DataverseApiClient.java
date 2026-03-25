package com.example.dataverse.downloader.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class DataverseApiClient {
	private static final Logger LOG = AppLogger.getLogger(DataverseApiClient.class);
	private static final String DEFAULT_VERSION = ":latest";
	private static final int BODY_PREVIEW_LIMIT = 1000;

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(30)).build();

	public DatasetRef resolveDatasetRefFast(String serverUrl, String doiOrUrl, String apiKey)
			throws IOException, InterruptedException {

		String persistentId = extractPersistentId(doiOrUrl);
		String encodedPid = URLEncoder.encode(persistentId, StandardCharsets.UTF_8);
		String apiUrl = normalizeServerUrl(serverUrl) + "/api/datasets/:persistentId/?persistentId=" + encodedPid;

		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl)).timeout(Duration.ofMinutes(2)).GET();

		if (apiKey != null && !apiKey.isBlank()) {
			builder.header("X-Dataverse-key", apiKey);
		}

		long started = System.nanoTime();
		LOG.info("Fast-resolving dataset ref for PID: " + persistentId + " via " + apiUrl);

		HttpResponse<InputStream> response = httpClient.send(builder.build(),
				HttpResponse.BodyHandlers.ofInputStream());
		long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

		if (response.statusCode() >= 300) {
			String body;
			try (InputStream in = response.body()) {
				body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				LOG.info("got: " + body);
			}
			throw new IOException("API request failed. HTTP " + response.statusCode() + " for " + apiUrl + ". Body: "
					+ abbreviate(body));
		}

		try (InputStream in = response.body();
				InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
				JsonReader reader = new JsonReader(isr)) {

			DatasetRef ref = readDatasetIdFast(reader, persistentId);
			LOG.info("Fast-resolved dataset ref in " + elapsedMs + " ms: id=" + ref.getDatasetId() + ", pid="
					+ ref.getPersistentId() + ", title=" + ref.getTitle());
			return ref;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed fast dataset-ref parse for " + apiUrl, e);
			throw new IOException("Could not resolve dataset id from API response.", e);
		}
	}

	public JsonObject fetchDatasetJson(String serverUrl, String doiOrUrl, String apiKey)
			throws IOException, InterruptedException {
		String persistentId = extractPersistentId(doiOrUrl);
		String encodedPid = URLEncoder.encode(persistentId, StandardCharsets.UTF_8);
		String apiUrl = normalizeServerUrl(serverUrl) + "/api/datasets/:persistentId/?persistentId=" + encodedPid;

		LOG.info("Fetching full dataset metadata for PID: " + persistentId);
		JsonObject data = getDataObject(apiUrl, apiKey);
		LOG.info("Fetched full dataset metadata for PID: " + persistentId);

		return data;
	}

	public int fetchFileCount(String serverUrl, long datasetId, String version, String apiKey)
			throws IOException, InterruptedException {
		String apiUrl = normalizeServerUrl(serverUrl) + "/api/datasets/" + datasetId + "/versions/" + version
				+ "/files/counts";

		LOG.info("Fetching file count for datasetId=" + datasetId + ", version=" + version);
		JsonElement dataElement = getDataElement(apiUrl, apiKey);

		if (!dataElement.isJsonObject()) {
			throw new IOException("Unexpected file count response: 'data' is not an object.");
		}

		JsonObject data = dataElement.getAsJsonObject();

		if (data.has("total") && !data.get("total").isJsonNull()) {
			int total = data.get("total").getAsInt();
			LOG.info("File count resolved from 'total': " + total);
			return total;
		}

		if (data.has("Total") && !data.get("Total").isJsonNull()) {
			int total = data.get("Total").getAsInt();
			LOG.info("File count resolved from 'Total': " + total);
			return total;
		}

		LOG.warning("File count response did not contain total field: " + abbreviate(data.toString()));
		throw new IOException("Could not read total file count from API response.");
	}

	public FilePage fetchFilesPage(String serverUrl, long datasetId, String version, int limit, int offset,
			String apiKey) throws IOException, InterruptedException {
		String apiUrl = normalizeServerUrl(serverUrl) + "/api/datasets/" + datasetId + "/versions/" + version
				+ "/files?limit=" + limit + "&offset=" + offset;

		LOG.info("Fetching files page datasetId=" + datasetId + ", version=" + version + ", limit=" + limit
				+ ", offset=" + offset);

		JsonElement dataElement = getDataElement(apiUrl, apiKey);

		JsonArray items = null;
		int totalCount = -1;

		if (dataElement.isJsonObject()) {
			JsonObject data = dataElement.getAsJsonObject();

			if (data.has("items") && data.get("items").isJsonArray()) {
				items = data.getAsJsonArray("items");
				if (data.has("totalCount") && !data.get("totalCount").isJsonNull()) {
					totalCount = data.get("totalCount").getAsInt();
				}
			} else if (data.has("files") && data.get("files").isJsonArray()) {
				items = data.getAsJsonArray("files");
				totalCount = items.size();
			}
		} else if (dataElement.isJsonArray()) {
			items = dataElement.getAsJsonArray();
			totalCount = items.size();
		}

		if (items == null) {
			LOG.warning("Unexpected file list response body: " + abbreviate(dataElement.toString()));
			throw new IOException("Unexpected file list response: missing files array.");
		}

		LOG.info("Fetched files page offset=" + offset + ", items=" + items.size() + ", totalCount=" + totalCount);

		return new FilePage(items, totalCount);
	}

	public String normalizeServerUrl(String serverUrl) {
		String value = serverUrl == null ? "" : serverUrl.trim();
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}

	public String resolveServerUrl(String configuredServerUrl, String doiOrUrl) {
		String value = doiOrUrl == null ? "" : doiOrUrl.trim();
		if (value.isBlank()) {
			return normalizeServerUrl(configuredServerUrl);
		}

		try {
			URI uri = new URI(value);
			if (uri.isAbsolute() && uri.getScheme() != null
					&& ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					&& uri.getHost() != null && !uri.getHost().isBlank()) {

				StringBuilder builder = new StringBuilder();
				builder.append(uri.getScheme()).append("://").append(uri.getHost());
				if (uri.getPort() >= 0) {
					builder.append(":").append(uri.getPort());
				}

				String resolved = builder.toString();
				LOG.info("Resolved Dataverse host from dataset URL: " + resolved);
				return resolved;
			}
		} catch (URISyntaxException e) {
			LOG.fine("Input is not a full URL, falling back to configured host: " + value);
		}

		return normalizeServerUrl(configuredServerUrl);
	}

	public String extractPersistentId(String doiOrUrl) {
		String value = doiOrUrl == null ? "" : doiOrUrl.trim();

		if (value.isBlank()) {
			throw new IllegalArgumentException("Dataset URL or DOI is required.");
		}

		int queryPidIdx = value.indexOf("persistentId=");
		if (queryPidIdx >= 0) {
			String pid = value.substring(queryPidIdx + "persistentId=".length());
			int amp = pid.indexOf('&');
			return amp >= 0 ? pid.substring(0, amp) : pid;
		}

		int doiIdx = value.indexOf("doi:");
		if (doiIdx >= 0) {
			return value.substring(doiIdx);
		}

		return value.startsWith("doi:") ? value : "doi:" + value;
	}

	public String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private DatasetRef readDatasetIdFast(JsonReader reader, String fallbackPid) throws IOException {
	    Long datasetId = null;
	    String persistentId = null;
	    String title = null;

	    String protocol = null;
	    String authority = null;
	    String separator = "/";
	    String identifier = null;

	    reader.beginObject();
	    while (reader.hasNext()) {
	        String rootName = reader.nextName();
	        if (!"data".equals(rootName)) {
	            reader.skipValue();
	            continue;
	        }

	        reader.beginObject();
	        while (reader.hasNext()) {
	            String name = reader.nextName();

	            switch (name) {
	                case "id" -> {
	                    if (reader.peek() == JsonToken.NULL) reader.nextNull();
	                    else datasetId = reader.nextLong();
	                }
	                case "persistentId" -> persistentId = readNullableString(reader);
	                case "protocol" -> protocol = readNullableString(reader);
	                case "authority" -> authority = readNullableString(reader);
	                case "separator" -> {
	                    String s = readNullableString(reader);
	                    if (s != null) separator = s;
	                }
	                case "identifier" -> identifier = readNullableString(reader);
	                case "latestVersion" -> {
	                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
	                        reader.skipValue();
	                    } else {
	                        LatestVersionInfo info = readLatestVersionInfo(reader);
	                        if (persistentId == null) persistentId = info.datasetPersistentId;
	                        if (title == null) title = info.title;
	                        return new DatasetRef(datasetId, persistentId != null ? persistentId : fallbackPid, title, reader);
	                    }
	                }
	                default -> reader.skipValue();
	            }
	        }
	        reader.endObject();
	    }
	    reader.endObject();

	    if (persistentId == null && protocol != null && authority != null && identifier != null) {
	        persistentId = protocol + ":" + authority + separator + identifier;
	    }

	    if (datasetId == null) {
	        throw new IOException("Could not resolve dataset id from API response.");
	    }

	    return new DatasetRef(datasetId, persistentId != null ? persistentId : fallbackPid, title, reader);
	}

	private LatestVersionInfo readLatestVersionInfo(JsonReader reader) throws IOException {
	    LatestVersionInfo info = new LatestVersionInfo();

	    reader.beginObject();
	    while (reader.hasNext()) {
	        String name = reader.nextName();

	        if ("datasetPersistentId".equals(name)) {
	            info.datasetPersistentId = readNullableString(reader);
	        } else if ("metadataBlocks".equals(name)) {
	            info.title = readTitleFromMetadataBlocks(reader);
	            return info;
	        } else {
	            reader.skipValue();
	        }
	    }
	    reader.endObject();

	    return info;
	}

	private String readTitleFromMetadataBlocks(JsonReader reader) throws IOException {
	    String title = null;

	    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
	        reader.skipValue();
	        return null;
	    }

	    reader.beginObject();
	    while (reader.hasNext()) {
	        String name = reader.nextName();

	        if (!"citation".equals(name)) {
	            reader.skipValue();
	            continue;
	        }

	        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
	            reader.skipValue();
	            continue;
	        }

	        reader.beginObject();
	        while (reader.hasNext()) {
	            String citationName = reader.nextName();

	            if (!"fields".equals(citationName)) {
	                reader.skipValue();
	                continue;
	            }

	            if (reader.peek() != JsonToken.BEGIN_ARRAY) {
	                reader.skipValue();
	                continue;
	            }

	            reader.beginArray();
	            while (reader.hasNext()) {
	                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
	                    reader.skipValue();
	                    continue;
	                }

	                reader.beginObject();
	                boolean isTitleField = false;
	                String value = null;

	                while (reader.hasNext()) {
	                    String fieldName = reader.nextName();

	                    if ("typeName".equals(fieldName)) {
	                        isTitleField = "title".equals(readNullableString(reader));
	                    } else if ("value".equals(fieldName)) {
	                        if (reader.peek() == JsonToken.STRING) { 
	                        
	                        	value = reader.nextString();
	                        	if(isTitleField) {return value;}
	                        	}
	                        else if (reader.peek() == JsonToken.NULL) reader.nextNull();
	                        else reader.skipValue();
	                    } else {
	                        reader.skipValue();
	                    }
	                }
	                reader.endObject();

	                if (isTitleField && value != null) {
	                    title = value;
	                }
	            }
	            reader.endArray();

	        }
	        reader.endObject();
	    }
	    reader.endObject();

	    return title;
	}

	private String readNullableString(JsonReader reader) throws IOException {
	    if (reader.peek() == JsonToken.NULL) {
	        reader.nextNull();
	        return null;
	    }
	    return reader.nextString();
	}

	private static class LatestVersionInfo {
	    String datasetPersistentId;
	    String title;
	}

	private JsonObject getDataObject(String apiUrl, String apiKey) throws IOException, InterruptedException {
		JsonElement dataElement = getDataElement(apiUrl, apiKey);
		if (!dataElement.isJsonObject()) {
			throw new IOException("Unexpected API response: 'data' is not an object.");
		}
		return dataElement.getAsJsonObject();
	}

	private JsonElement getDataElement(String apiUrl, String apiKey) throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl)).timeout(Duration.ofMinutes(5)).GET()
				.header("Accept", "application/json");

		if (apiKey != null && !apiKey.isBlank()) {
			builder.header("X-Dataverse-key", apiKey);
		}

		HttpResponse<InputStream> response = httpClient.send(builder.build(),
				HttpResponse.BodyHandlers.ofInputStream());

		if (response.statusCode() >= 300) {
			String body;
			try (InputStream in = response.body()) {
				body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			throw new IOException("API request failed. HTTP " + response.statusCode() + " for " + apiUrl + ". Body: "
					+ abbreviate(body));
		}

		try (InputStream in = response.body();
				InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8)) {

			JsonObject root = JsonParser.parseReader(isr).getAsJsonObject();
			if (!root.has("data")) {
				throw new IOException("Unexpected API response: missing 'data' field.");
			}
			return root.get("data");
		} catch (Exception e) {
			throw new IOException("Could not parse API response.", e);
		}
	}

	private String abbreviate(String value) {
		if (value == null) {
			return "";
		}
		if (value.length() <= BODY_PREVIEW_LIMIT) {
			return value;
		}
		return value.substring(0, BODY_PREVIEW_LIMIT) + "...";
	}

	public static final class FilePage {
		private final JsonArray items;
		private final int totalCount;

		public FilePage(JsonArray items, int totalCount) {
			this.items = items;
			this.totalCount = totalCount;
		}

		public JsonArray getItems() {
			return items;
		}

		public int getTotalCount() {
			return totalCount;
		}
	}

	public static final class DatasetRef {
		private final long datasetId;
		private final String persistentId;
		private final String title;
		JsonReader restReader;

		public DatasetRef(long datasetId, String persistentId, String title, JsonReader reader) {
			this.datasetId = datasetId;
			this.persistentId = persistentId;
			this.title = title;
			restReader = reader;
		}

		public JsonReader getRestReader() {
			return restReader;
		}

		public String getTitle() {
			return title;
		}

		public long getDatasetId() {
			return datasetId;
		}

		public String getPersistentId() {
			return persistentId;
		}
	}
}