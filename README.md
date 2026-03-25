# Dataverse Downloader

A Java Swing desktop application for browsing and downloading files from a Dataverse dataset by DOI or dataset URL.

The application lets you:

- load dataset metadata from a Dataverse host
- browse files in a tree grouped by folders
- select individual files or whole folders
- download selected files to a local output folder
- resume partial downloads from `.part` files
- verify downloaded files with checksums when available
- persist preferences in a local SQLite database

## Features

- Desktop GUI built with Swing
- Preferences dialog for:
  - Dataverse host URL
  - API key
  - output folder
  - parallel downloads
  - overwrite policy
- Preferences stored locally in SQLite
- Supports dataset DOI, Dataverse dataset URL, or URL containing `persistentId=...`
- Folder-based file tree
- Selecting a folder selects all files inside it recursively
- Collapsed folders can still be downloaded if selected
- Download queue with progress table
- Resume support using HTTP range requests and `.part` files
- Download manifest written to `download-manifest.json`
- Checksum verification after download, when checksum metadata is available

## Requirements

- Java 17 or newer
- Maven 3.9+ recommended
- Network access to the Dataverse host you want to use

## Build

Clone the project and run:

```bash
mvn clean package
```
## Run
```bash
mvn exec:java -Dexec.mainClass="com.example.dataverse.downloader.App"
```
## Usage

1. Open **Options > Preferences...**
2. Set:
   - Dataverse host URL
   - API key (optional)
   - Output folder
3. Enter dataset DOI or URL
4. Click **Load Dataset**
5. Select files or folders
6. Click **Download Selected**

## Resume Downloads

- Keep `.part` files
- Use same output folder
- Re-run download → resumes automatically

## Notes

- Parallel downloads are applied at startup
- Restart app after changing preferences