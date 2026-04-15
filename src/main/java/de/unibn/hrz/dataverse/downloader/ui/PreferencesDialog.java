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
package de.unibn.hrz.dataverse.downloader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.unibn.hrz.dataverse.downloader.model.AppPreferences;
import de.unibn.hrz.dataverse.downloader.validation.PreferencesValidator;

/**
 * Modal preferences dialog for editing user-configurable application settings.
 */
public class PreferencesDialog extends JDialog {
    private static final long serialVersionUID = -6379304123008788645L;

    private final JTextField serverField = new JTextField();
    private final JPasswordField apiKeyField = new JPasswordField();
    private final JTextField outputField = new JTextField();
    private final JTextField parallelField = new JTextField();
    private final JTextField historySizeField = new JTextField();
    private final JComboBox<String> overwriteCombo =
            new JComboBox<>(new String[] { "SKIP", "OVERWRITE" });

    private final JLabel validationLabel = new JLabel(" ");
    private final JButton saveButton = new JButton("Save");

    private boolean saved;
    private AppPreferences validatedPreferences;

    public PreferencesDialog(Frame owner, AppPreferences preferences) {
        super(owner, "Preferences", true);
        setLayout(new BorderLayout(8, 8));

        serverField.setText(preferences.getServerUrl());
        apiKeyField.setText(preferences.getApiKey());
        outputField.setText(
                preferences.getOutputFolder() == null
                        ? ""
                        : preferences.getOutputFolder().toString());
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

        validationLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        validationLabel.setForeground(new Color(180, 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            saved = false;
            validatedPreferences = null;
            setVisible(false);
        });

        saveButton.addActionListener(e -> savePreferences());

        buttons.add(cancelButton);
        buttons.add(saveButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(validationLabel, BorderLayout.CENTER);
        southPanel.add(buttons, BorderLayout.EAST);

        add(southPanel, BorderLayout.SOUTH);

        installLiveValidation();
        updateValidationState();

        setSize(650, 340);
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
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose output folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        String currentValue = outputField.getText().trim();
        if (!currentValue.isEmpty()) {
            chooser.setCurrentDirectory(Paths.get(currentValue).toFile());
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            outputField.setText(chooser.getSelectedFile().getAbsolutePath());
        }

        updateValidationState();
    }

    private void savePreferences() {
        try {
            AppPreferences candidate = toPreferences();
            PreferencesValidator.validate(candidate);

            validatedPreferences = candidate;
            saved = true;
            setVisible(false);
        } catch (IllegalArgumentException ex) {
            validationLabel.setText(ex.getMessage());
            saveButton.setEnabled(false);
        }
    }

    private void installLiveValidation() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateValidationState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateValidationState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateValidationState();
            }
        };

        serverField.getDocument().addDocumentListener(listener);
        apiKeyField.getDocument().addDocumentListener(listener);
        outputField.getDocument().addDocumentListener(listener);
        parallelField.getDocument().addDocumentListener(listener);
        historySizeField.getDocument().addDocumentListener(listener);

        overwriteCombo.addActionListener(e -> updateValidationState());
    }

    private void updateValidationState() {
        try {
            AppPreferences candidate = toPreferences();
            PreferencesValidator.validate(candidate);

            validationLabel.setText(" ");
            saveButton.setEnabled(true);
        } catch (IllegalArgumentException ex) {
            validationLabel.setText(ex.getMessage());
            saveButton.setEnabled(false);
        }
    }

    /**
     * Builds an {@link AppPreferences} instance from the current field values.
     *
     * <p>This method performs parsing and normalization but not semantic
     * validation beyond integer conversion.</p>
     *
     * @return preferences created from the current dialog field values
     * @throws IllegalArgumentException if a numeric field cannot be parsed
     */
    public AppPreferences toPreferences() {
        AppPreferences preferences = new AppPreferences();

        preferences.setServerUrl(normalizeServerUrl(serverField.getText()));
        preferences.setApiKey(new String(apiKeyField.getPassword()));
        preferences.setOutputFolder(parseOutputFolder(outputField.getText()));
        preferences.setParallelDownloads(parseInteger(parallelField.getText(), "Parallel downloads"));
        preferences.setDatasetUrlHistorySize(
                parseInteger(historySizeField.getText(), "Dataset URL history size"));
        preferences.setOverwritePolicy((String) overwriteCombo.getSelectedItem());

        return preferences;
    }

    /**
     * Returns the validated preferences after a successful save.
     *
     * @return validated preferences, or {@code null} if the dialog was cancelled
     *         or not yet saved successfully
     */
    public AppPreferences getValidatedPreferences() {
        return validatedPreferences;
    }

    public boolean isSaved() {
        return saved;
    }

    private String normalizeServerUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        trimmed = trimmed.replaceAll("/+$", "");
        return trimmed;
    }

    private Path parseOutputFolder(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : Paths.get(trimmed);
    }

    private int parseInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }
}