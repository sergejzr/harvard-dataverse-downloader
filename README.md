# Harvard Dataverse Downloader

![Java](https://img.shields.io/badge/Java-17-blue)
![Platform](https://img.shields.io/badge/platform-desktop-lightgrey)
![Status](https://img.shields.io/badge/status-beta-orange)

------------------------------------------------------------------------

A Java Swing desktop application for browsing and downloading files from Dataverse datasets via DOI or dataset URL.

Downloading directly from Dataverse web interfaces can be limiting:
- no folder-level downloads
- difficult handling of large datasets
- interrupted downloads restart from scratch

This tool solves those problems with selective downloads, resume support, and a structured UI.

------------------------------------------------------------------------

## ✨ Features

- 🌳 Browse files in a folder-based tree
- ✅ Select individual files or whole folders
- ⬇️ Download selected files to local directory
- 🔁 Resume partial downloads using `.part` files
- 🔐 Verify files using checksums (if available)
- ⚡ Parallel downloads
- 💾 Persistent preferences (SQLite)
- 🎨 Modern UI (FlatLaf)
- 🔗 Browser integration via `hvdvdl://`
- 📦 Native installers (Windows & Linux)


------------------------------------------------------------------------

## 📸 Screenshots

<img width="700" src="https://github.com/user-attachments/assets/31594c89-84ce-4750-8b12-3b2900ec9df3" />
<img width="700" src="https://github.com/user-attachments/assets/80d2b3bb-f5cb-49f9-8df4-b0b49065cf2c" />
<img width="700" src="https://github.com/user-attachments/assets/9c26b0ba-342f-4238-bed4-1cf1727d6ac0" />

------------------------------------------------------------------------

## 📦 Download

Get the latest release here:
👉 https://github.com/sergejzr/harvard-dataverse-downloader/releases

### Available Assets

- 🟦 **Fat JAR (cross-platform)**
  - `dataverse-downloader_0.1.1_fatjar.jar` (simplest if you have java installed)

- 🪟 **Windows Installer**
  - `dataverse-downloader_0.1.1_windows.msi`

- 🐧 **Linux Installer**
  - `dataverse-downloader_0.1.1_amd64.deb`

------------------------------------------------------------------------

## ▶️ Run (JAR)

```bash
java -jar dataverse-downloader_0.1.1_fatjar.jar
```

------------------------------------------------------------------------

## 🔗 Browser Integration

After installing via the native installer, the app registers a custom protocol:

```
hvdvdl://open?url=<DATASET_URL>
```

Example:

```
hvdvdl://open?url=https%3A%2F%2Fbonndata.uni-bonn.de%2Fdataset.xhtml%3FpersistentId%3Ddoi%3A10.60507%2FFK2%2FSBBP9G
```

➡️ Clicking such links will:
- Launch the application
- Automatically load the dataset
- Integration into dataverse is planned (at least for bonndata at Bonn University) 
------------------------------------------------------------------------

## 🧩 Features in Detail

### 📦 Dataset Loading

- Supports DOI and dataset URLs
- Handles `persistentId=...`
- Automatic host detection

### 🌳 File Browser

- Folder-based tree structure
- Recursive selection
- Download works even if folders are collapsed

### ⬇️ Download Engine

- Parallel downloads
- Resume via HTTP range requests
- `.part` file handling
- Manifest file: `download-manifest.json`

### 📊 Download Manager

- Progress table
- Pause / resume support
- Error handling

### 🔐 Integrity Verification

- MD5, SHA-1, SHA-256, SHA-512

### ⚙️ Preferences

- Dataverse host URL
- API key (optional)
- Output folder
- Parallel downloads
- Overwrite policy

------------------------------------------------------------------------

## 🧪 Requirements

- Java 17+
- Maven 3.9+
- Network access to Dataverse host

------------------------------------------------------------------------

## 🛠️ Build

```bash
mvn clean package
```

------------------------------------------------------------------------

## ▶️ Run (Dev)

```bash
mvn exec:java -Dexec.mainClass="com.example.dataverse.downloader.App"
```

------------------------------------------------------------------------

## 📘 Usage

1. Open **Options > Preferences**
2. Configure:
   - Dataverse host URL
   - API key (optional)
   - Parallel downloads
   - Output folder
3. Enter DOI or dataset URL
4. Click **Load Dataset**
5. Select files/folders
6. Click **Download Selected**

------------------------------------------------------------------------

## 🔁 Resume Downloads

- Keep `.part` files
- Use same output directory
- Restart download → resumes automatically

------------------------------------------------------------------------

## ⚠️ Notes

- Parallel downloads apply on startup
- Restart app after changing preferences

------------------------------------------------------------------------

## 🗺️ Roadmap

- 🔁 Advanced retry/backoff
- 🔎 Dataset/file search & filtering
- 🎨 UI improvements
- 📦 macOS installer

------------------------------------------------------------------------

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

------------------------------------------------------------------------

## ⭐ Support

If you like this project, consider starring the repository!
