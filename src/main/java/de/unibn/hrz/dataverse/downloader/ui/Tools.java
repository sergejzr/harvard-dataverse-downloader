package de.unibn.hrz.dataverse.downloader.ui;

import java.awt.Image;

import javax.swing.ImageIcon;

public class Tools {

	public static ImageIcon scaleIconPreserveRatio(ImageIcon icon, int maxSize) {
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();

        if (width <= 0 || height <= 0) {
            return icon;
        }

        double scale = Math.min(
                (double) maxSize / width,
                (double) maxSize / height);

        int newWidth = (int) Math.round(width * scale);
        int newHeight = (int) Math.round(height * scale);

        Image scaled = icon.getImage().getScaledInstance(
                newWidth,
                newHeight,
                Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }
}
