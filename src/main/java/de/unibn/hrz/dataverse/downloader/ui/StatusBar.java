package de.unibn.hrz.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusBar extends JPanel {
    private static final long serialVersionUID = -415720112700631320L;

    private final JLabel messageLabel = new JLabel("Ready");
    private final JLabel folderLabel = new JLabel();

    public StatusBar() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        add(messageLabel, BorderLayout.CENTER);
        add(folderLabel, BorderLayout.EAST);
        setDownloadFolder(null);
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    public void setDownloadFolder(Path folder) {
        folderLabel.setText("Download folder: " + (folder == null ? "-" : folder.toString()));
    }

    public void setDownloadProgress(int completed, int total, int remaining, int percent) {
        if (total <= 0) {
            setMessage("Ready");
            return;
        }

        messageLabel.setText(
                "Downloaded " + completed + " / " + total
                        + " | Left: " + remaining
                        + " | " + percent + "%");
    }
}