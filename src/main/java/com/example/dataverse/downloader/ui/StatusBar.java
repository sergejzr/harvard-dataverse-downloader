package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusBar extends JPanel {
    private final JLabel label = new JLabel("Ready");

    public StatusBar() {
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }

    public void setMessage(String message) {
        label.setText(message);
    }
}
