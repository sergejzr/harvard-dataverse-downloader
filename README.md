# Harvard Dataverse Downloader

![Java](https://img.shields.io/badge/Java-17-blue)
![Platform](https://img.shields.io/badge/platform-desktop-lightgrey)
![Status](https://img.shields.io/badge/status-initial%20release-green)


A Java Swing desktop application for browsing and downloading files from a Dataverse dataset by DOI or dataset URL.
Downloading from the dataverse webapplication can be challenging as there are many limitation - subsets of files (for instance folders) can not be selected for download, large datasets can not be downloaded as a whole, broken downloads start from scratch each time, etc.

The application lets you:

- browse files in a tree grouped by folders
- select individual files or whole folders
- download selected files to a local output folder
- resume partial downloads from `.part` files
- verify downloaded files with checksums when available

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

<img width="700" height="711" alt="image" src="https://github.com/user-attachments/assets/31594c89-84ce-4750-8b12-3b2900ec9df3" />
<img width="700" height="696" alt="image" src="https://github.com/user-attachments/assets/80d2b3bb-f5cb-49f9-8df4-b0b49065cf2c" />
<img width="700" height="695" alt="image" src="https://github.com/user-attachments/assets/9c26b0ba-342f-4238-bed4-1cf1727d6ac0" />





## Requirements

- Java 17 or newer
- Maven 3.9+ recommended
- Network access to the Dataverse host you want to use

## Start
You can download an executable jar from the release assets section https://github.com/sergejzr/harvard-dataverse-downloader/releases

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
   - number of arallel downloads
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
