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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class AboutDialog extends JDialog {

    private static final String RDM_URL = "https://www.forschungsdaten.uni-bonn.de";
    private static final String UBONN_URL = "https://www.uni-bonn.de";
    private static final String DATAVERSE_URL = "https://dataverse.org";
    private static final String GITHUB_URL = "https://github.com/sergejzr/harvard-dataverse-downloader";

    public AboutDialog(Frame owner) {
        super(owner, "About", true);

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        content.add(createHeader(), BorderLayout.NORTH);
        content.add(createCenterPanel(), BorderLayout.CENTER);
        content.add(createFooter(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);

        setSize(620, 520);
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(owner);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dataverse Downloader");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Download datasets from Dataverse repositories");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createLogoRow("/logos/rdm.png",
                "Service Center for Research Data Management, University of Bonn",
                RDM_URL,
                170, 50));

        panel.add(Box.createVerticalStrut(14));

        panel.add(createLogoRow("/logos/uni_bonn.png",
                "University of Bonn",
                UBONN_URL,
                220, 50));

        panel.add(Box.createVerticalStrut(14));

        panel.add(createLogoRow("/logos/dataverse.png",
                "The Dataverse Project",
                DATAVERSE_URL,
                320, 50));

        return panel;
    }

    private JPanel createLogoRow(String path, String tooltip, String url, int maxWidth, int maxHeight) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel logo = createLogo(path, tooltip, url, maxWidth, maxHeight);
        row.add(logo);

        return row;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel copyright = new JLabel("© 2026 Service Center for Research Data Management, University of Bonn");
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel github = createLinkLabel("GitHub repository", GITHUB_URL);
        github.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel website = createLinkLabel("Service Center website", RDM_URL);
        website.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(copyright);
        panel.add(Box.createVerticalStrut(8));
        panel.add(github);
        panel.add(Box.createVerticalStrut(4));
        panel.add(website);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        buttons.add(close);
        return buttons;
    }

    private JLabel createLogo(String path, String tooltip, String url, int maxWidth, int maxHeight) {
        java.net.URL resource = getClass().getResource(path);
        JLabel label;

        if (resource == null) {
            label = new JLabel("[missing: " + path + "]");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setToolTipText("Resource not found: " + path);
            return label;
        }

        ImageIcon icon = new ImageIcon(resource);
        Image scaled = scaleToFit(icon, maxWidth, maxHeight);
        label = new JLabel(new ImageIcon(scaled));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setToolTipText(tooltip);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openLink(url);
            }
        });

        return label;
    }

    private JLabel createLinkLabel(String text, String url) {
        JLabel label = new JLabel("<html><a href=''>" + text + "</a></html>");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setToolTipText(url);
        label.setFont(UIManager.getFont("Label.font"));
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openLink(url);
            }
        });
        return label;
    }

    private Image scaleToFit(ImageIcon icon, int maxWidth, int maxHeight) {
        int originalWidth = icon.getIconWidth();
        int originalHeight = icon.getIconHeight();

        if (originalWidth <= 0 || originalHeight <= 0) {
            return icon.getImage();
        }

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int scaledWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        return icon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }
}