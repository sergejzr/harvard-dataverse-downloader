package de.unibn.hrz.dataverse.downloader.ui;

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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import de.unibn.hrz.dataverse.downloader.model.DatasetInfo;

/**
 * Modal dialog shown before download when a dataset provides either a standard
 * license object or custom Dataverse terms of use fields.
 *
 * <p>
 * The dialog shows:
 * </p>
 * <ul>
 * <li>dataset title and DOI,</li>
 * <li>an optional license icon loaded from the Dataverse metadata,</li>
 * <li>repository-provided terms assembled from parsed dataset metadata,
 * and</li>
 * <li>the content of the license URL, loaded asynchronously when
 * available.</li>
 * </ul>
 *
 * <p>
 * The user must explicitly check the agreement box before the dialog returns
 * acceptance.
 * </p>
 */
public class DatasetTermsDialog extends JDialog {
	private static final long serialVersionUID = 8030924430330517260L;

	private static final int ICON_SIZE = 100;
	private static final int DIALOG_WIDTH = 860;
	private static final int DIALOG_HEIGHT = 640;

	private final DatasetInfo datasetInfo;

	private boolean accepted;

	private final JLabel iconLabel = new JLabel();
	private final JLabel datasetLabel = new JLabel();
	private final JEditorPane repositoryTermsPane = new JEditorPane();
	private final JEditorPane licensePagePane = new JEditorPane();
	private final JLabel remoteStatusLabel = new JLabel("Loading license page...");
	private final JCheckBox agreeBox = new JCheckBox("I have read and agree to the dataset license / terms.");

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10)).build();

	public DatasetTermsDialog(Window owner, DatasetInfo datasetInfo) {
		super(owner instanceof Frame ? (Frame) owner : null, "Dataset Terms Agreement", ModalityType.APPLICATION_MODAL);

		if (datasetInfo == null) {
			throw new IllegalArgumentException("datasetInfo must not be null");
		}

		this.datasetInfo = datasetInfo;

		buildUi();
		startAsyncLoads();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		pack();
		setMinimumSize(new Dimension(720, 520));
		setLocationRelativeTo(owner);
	}

	/**
	 * Opens the dialog and returns whether the user accepted the terms.
	 *
	 * @param owner       parent window
	 * @param datasetInfo dataset metadata including license / terms
	 * @return {@code true} if accepted, otherwise {@code false}
	 */
	public static boolean showDialog(Window owner, DatasetInfo datasetInfo) {
		if (datasetInfo == null || !datasetInfo.hasLicenseOrTerms()) {
			return true;
		}

		DatasetTermsDialog dialog = new DatasetTermsDialog(owner, datasetInfo);
		dialog.setVisible(true);
		return dialog.accepted;
	}

	private void buildUi() {
		setLayout(new BorderLayout(10, 10));

		JPanel content = new JPanel(new BorderLayout(10, 10));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		content.add(createHeaderPanel(), BorderLayout.NORTH);
		content.add(createCenterPanel(), BorderLayout.CENTER);
		content.add(createFooterPanel(), BorderLayout.SOUTH);

		add(content, BorderLayout.CENTER);
		add(createButtonsPanel(), BorderLayout.SOUTH);
	}

	private JPanel createHeaderPanel() {
		JPanel header = new JPanel(new BorderLayout(10, 0));
		header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		iconLabel.setPreferredSize(new Dimension(ICON_SIZE + 8, ICON_SIZE + 8));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		iconLabel.setText("");

		datasetLabel.setVerticalAlignment(SwingConstants.TOP);
		datasetLabel.setText(buildHeaderHtml());

		header.add(iconLabel, BorderLayout.WEST);
		header.add(datasetLabel, BorderLayout.CENTER);

		return header;
	}

	private JPanel createCenterPanel() {
		JTabbedPane tabs = new JTabbedPane();

		repositoryTermsPane.setEditable(false);
		repositoryTermsPane.setContentType("text/html");
		repositoryTermsPane.setText(buildRepositoryTermsHtml());
		repositoryTermsPane.setCaretPosition(0);

		JScrollPane repositoryScrollPane = new JScrollPane(repositoryTermsPane);
		repositoryScrollPane.setBorder(BorderFactory.createEmptyBorder());

		tabs.addTab("Repository Terms", repositoryScrollPane);

		licensePagePane.setEditable(false);
		licensePagePane.setContentType("text/plain");
		licensePagePane.setText(initialLicensePageText());
		licensePagePane.setCaretPosition(0);

		JPanel remotePanel = new JPanel(new BorderLayout(8, 8));
		remotePanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		remotePanel.add(remoteStatusLabel, BorderLayout.NORTH);
		remotePanel.add(new JScrollPane(licensePagePane), BorderLayout.CENTER);

		tabs.addTab("License Page", remotePanel);

		JPanel center = new JPanel(new BorderLayout());
		center.add(tabs, BorderLayout.CENTER);
		return center;
	}

	private String buildRepositoryTermsHtml() {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><body style='font-family: sans-serif;'>");

		appendHtmlLine(sb, "<b>Dataset</b>", datasetInfo.getTitle());
		appendHtmlLine(sb, "<b>DOI</b>", datasetInfo.getPersistentId());

		if (datasetInfo.hasLicense()) {
			sb.append("<h3>License</h3>");
			appendHtmlLine(sb, "<b>Name</b>", datasetInfo.getLicenseName());
			appendHtmlLine(sb, "<b>URL</b>", datasetInfo.getLicenseUri());
		}

		if (datasetInfo.hasCustomTerms()) {
			sb.append("<h3>Custom Terms of Use</h3>");

			appendHtmlLine(sb, "<b>Terms of Use</b>", datasetInfo.getTermsOfUse());
			appendHtmlLine(sb, "<b>Confidentiality Declaration</b>", datasetInfo.getConfidentialityDeclaration());
			appendHtmlLine(sb, "<b>Special Permissions</b>", datasetInfo.getSpecialPermissions());
			appendHtmlLine(sb, "<b>Restrictions</b>", datasetInfo.getRestrictions());
			appendHtmlLine(sb, "<b>Citation Requirements</b>", datasetInfo.getCitationRequirements());
			appendHtmlLine(sb, "<b>Depositor Requirements</b>", datasetInfo.getDepositorRequirements());
			appendHtmlLine(sb, "<b>Conditions</b>", datasetInfo.getConditions());
			appendHtmlLine(sb, "<b>Disclaimer</b>", datasetInfo.getDisclaimer());
			appendHtmlLine(sb, "<b>Data Access Place</b>", datasetInfo.getDataAccessPlace());
			appendHtmlLine(sb, "<b>Original Archive</b>", datasetInfo.getOriginalArchive());
			appendHtmlLine(sb, "<b>Availability Status</b>", datasetInfo.getAvailabilityStatus());
			appendHtmlLine(sb, "<b>Contact for Access</b>", datasetInfo.getContactForAccess());
			appendHtmlLine(sb, "<b>Size of Collection</b>", datasetInfo.getSizeOfCollection());
			appendHtmlLine(sb, "<b>Study Completion</b>", datasetInfo.getStudyCompletion());
		}

		sb.append("</body></html>");

		return sb.toString();
	}

	private void appendHtmlLine(StringBuilder sb, String label, String value) {
		if (value != null && !value.isBlank()) {
			sb.append(label).append(": ").append(escapeHtml(value)).append("<br>");
		}
	}

	private JPanel createFooterPanel() {
		JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

		String introText = datasetInfo.hasLicense() && datasetInfo.hasCustomTerms()
				? "This dataset defines both a license and additional custom repository terms."
				: datasetInfo.hasLicense() ? "This dataset defines a license."
						: "This dataset defines custom repository terms.";

		JLabel introLabel = new JLabel(introText);
		introLabel.setAlignmentX(LEFT_ALIGNMENT);

		agreeBox.setAlignmentX(LEFT_ALIGNMENT);

		footer.add(introLabel);
		footer.add(Box.createVerticalStrut(8));
		footer.add(agreeBox);

		return footer;
	}

	private JPanel createButtonsPanel() {
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			accepted = false;
			dispose();
		});

		JButton acceptButton = new JButton("Accept and Download");
		acceptButton.addActionListener(e -> onAccept());

		buttons.add(cancelButton);
		buttons.add(acceptButton);

		return buttons;
	}

	private void onAccept() {
		if (!agreeBox.isSelected()) {
			JOptionPane.showMessageDialog(this, "Please confirm that you agree to the dataset license / terms.",
					"Agreement required", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		accepted = true;
		dispose();
	}

	private String buildHeaderHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<div style='font-size: 14px;'><b>")
				.append(escapeHtml(valueOrFallback(datasetInfo.getTitle(), "Dataset"))).append("</b></div>");
		sb.append("<div style='margin-top: 4px;'>")
				.append(escapeHtml(valueOrFallback(datasetInfo.getPersistentId(), ""))).append("</div>");

		if (notBlank(datasetInfo.getLicenseName())) {
			sb.append("<div style='margin-top: 8px;'>License: ").append(escapeHtml(datasetInfo.getLicenseName()))
					.append("</div>");
		}

		if (notBlank(datasetInfo.getLicenseUri())) {
			sb.append("<div style='margin-top: 2px;'>").append(escapeHtml(datasetInfo.getLicenseUri()))
					.append("</div>");
		}

		sb.append("</html>");
		return sb.toString();
	}

	private String initialLicensePageText() {
		if (!notBlank(datasetInfo.getLicenseUri())) {
			remoteStatusLabel.setText("No license URL provided by repository.");
			return "No license URL available.";
		}
		return "Loading content from:\n" + datasetInfo.getLicenseUri();
	}

	private void startAsyncLoads() {
		loadLicenseIconAsync();
		loadLicensePageAsync();
	}

	private void loadLicenseIconAsync() {
		if (!notBlank(datasetInfo.getLicenseIconUri())) {
			return;
		}

		new SwingWorker<Image, Void>() {
			@Override
			protected Image doInBackground() throws Exception {
				setBusyCursor(true);
				BufferedImage image = ImageIO.read(new URI(datasetInfo.getLicenseIconUri()).toURL());
				return image;
			}

			@Override
			protected void done() {
				try {
					Image image = get();
					if (image != null) {
						iconLabel.setIcon(Tools.scaleIconPreserveRatio(new ImageIcon(image), ICON_SIZE));
						iconLabel.setText("");
					}
				} catch (Exception ignored) {
					// Keep the dialog usable even if icon loading fails.
				} finally {
					setBusyCursor(false);
				}
			}
		}.execute();
	}

	private void loadLicensePageAsync() {
		if (!notBlank(datasetInfo.getLicenseUri())) {
			return;
		}

		new SwingWorker<RemoteLicenseContent, Void>() {
			@Override
			protected RemoteLicenseContent doInBackground() throws Exception {
				setBusyCursor(true);
				return fetchLicenseContent(datasetInfo.getLicenseUri());
			}

			@Override
			protected void done() {
				try {
					RemoteLicenseContent content = get();
					remoteStatusLabel.setText(content.statusText());

					if (content.html()) {
						licensePagePane.setContentType("text/html");
					} else {
						licensePagePane.setContentType("text/plain");
					}

					licensePagePane.setText(content.body());
					licensePagePane.setCaretPosition(0);
				} catch (Exception e) {
					remoteStatusLabel.setText("Could not load license page.");
					licensePagePane.setContentType("text/plain");
					licensePagePane.setText("Could not load license content.\n\nReason: " + e.getMessage() + "\n\nURL: "
							+ datasetInfo.getLicenseUri());
					licensePagePane.setCaretPosition(0);
				} finally {
					setBusyCursor(false);
				}
			}
		}.execute();
	}

	private RemoteLicenseContent fetchLicenseContent(String licenseUrl) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(licenseUrl)).timeout(Duration.ofSeconds(20))
					.header("Accept", "text/html, text/plain;q=0.9, */*;q=0.8").GET().build();

			HttpResponse<String> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

			int status = response.statusCode();
			String contentType = firstHeaderValue(response, "Content-Type");
			String body = response.body() == null ? "" : response.body();

			if (status >= 300) {
				return new RemoteLicenseContent(false, "HTTP " + status + " while loading license page.",
						"Could not load license content.\n\nHTTP " + status + "\n\nURL: " + licenseUrl);
			}

			if (contentType != null && contentType.toLowerCase().contains("text/html")) {
				String safeHtml = wrapHtmlForViewer(body, licenseUrl);
				return new RemoteLicenseContent(true, "Loaded from license URL.", safeHtml);
			}

			if (body.isBlank()) {
				return new RemoteLicenseContent(false, "License page returned no readable content.",
						"The license URL returned an empty response.\n\nURL: " + licenseUrl);
			}

			return new RemoteLicenseContent(false, "Loaded from license URL.",
					"License URL:\n" + licenseUrl + "\n\n" + body);
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			return new RemoteLicenseContent(false, "Could not load license page.",
					"Could not load license content.\n\nURL: " + licenseUrl + "\n\nReason: " + e.getMessage());
		} catch (Exception e) {
			return new RemoteLicenseContent(false, "Could not load license page.",
					"Could not load license content.\n\nURL: " + licenseUrl + "\n\nReason: " + e.getMessage());
		}
	}

	private String wrapHtmlForViewer(String html, String sourceUrl) {
		String safeUrl = escapeHtml(sourceUrl);

		return """
				<html>
				  <body style="font-family: sans-serif; margin: 10px;">
				    <div style="margin-bottom: 12px;">
				      <b>Source:</b> %s
				    </div>
				    <hr/>
				    %s
				  </body>
				</html>
				""".formatted(safeUrl, html == null ? "" : html);
	}

	private String firstHeaderValue(HttpResponse<?> response, String name) {
		return response.headers().firstValue(name).orElse("");
	}

	private boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

	private String valueOrFallback(String value, String fallback) {
		return notBlank(value) ? value : fallback;
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void setBusyCursor(boolean busy) {
		setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
	}

	private record RemoteLicenseContent(boolean html, String statusText, String body) {
	}
}
