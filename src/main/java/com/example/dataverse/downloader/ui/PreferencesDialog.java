package com.example.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.example.dataverse.downloader.model.AppPreferences;

public class PreferencesDialog extends JDialog {
    private final JTextField serverField = new JTextField();
    private final JPasswordField apiKeyField = new JPasswordField();
    private final JTextField outputField = new JTextField();
    private final JTextField parallelField = new JTextField();
    private final JTextField historySizeField = new JTextField();
    private final JComboBox<String> overwriteCombo = new JComboBox<>(new String[] { "SKIP", "OVERWRITE" });

    private boolean saved;

    public PreferencesDialog(Frame owner, AppPreferences preferences) {
        super(owner, "Preferences", true);
        setLayout(new BorderLayout(8, 8));

        serverField.setText(preferences.getServerUrl());
        apiKeyField.setText(preferences.getApiKey());
        outputField.setText(preferences.getOutputFolder().toString());
        parallelField.setText(String.valueOf(preferences.getParallelDownloads()));
        historySizeField.setText(String.valueOf(preferences.getDatasetUrlHistorySize()));
        overwriteCombo.setSelectedItem(preferences.getOverwritePolicy());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addRow(form, gbc, "Dataverse host URL", serverField);
        addRow(form, gbc, "API key", apiKeyField);

        JPanel outputPanel = new JPanel(new BorderLayout(6, 0));
        outputPanel.add(outputField, BorderLayout.CENTER);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> chooseOutputFolder());
        outputPanel.add(browseButton, BorderLayout.EAST);
        addRow(form, gbc, "Output folder", outputPanel);

        addRow(form, gbc, "Parallel downloads", parallelField);
        addRow(form, gbc, "Dataset URL history size", historySizeField);
        addRow(form, gbc, "Overwrite policy", overwriteCombo);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            saved = true;
            setVisible(false);
        });

        buttons.add(cancelButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);

        setSize(650, 320);
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String label, java.awt.Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridy++;
    }

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser(outputField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            outputField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public AppPreferences toPreferences() {
        AppPreferences preferences = new AppPreferences();
        preferences.setServerUrl(serverField.getText().trim());
        preferences.setApiKey(new String(apiKeyField.getPassword()).trim());
        preferences.setOutputFolder(Paths.get(outputField.getText().trim()));
        preferences.setParallelDownloads(Integer.parseInt(parallelField.getText().trim()));
        preferences.setDatasetUrlHistorySize(Integer.parseInt(historySizeField.getText().trim()));
        preferences.setOverwritePolicy(String.valueOf(overwriteCombo.getSelectedItem()));
        return preferences;
    }
}