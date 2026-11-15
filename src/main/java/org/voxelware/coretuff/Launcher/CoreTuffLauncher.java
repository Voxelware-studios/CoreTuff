package org.voxelware.coretuff.Launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class CoreTuffLauncher {

	public static String getVersion() {
		try (InputStream is = CoreTuffLauncher.class
				.getClassLoader()
				.getResourceAsStream("paper-plugin.yml")) {

			if (is == null) return "Unknown";

			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;

			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("version:")) {
					return line.split(":", 2)[1].trim();
				}
			}

		} catch (Exception ignored) {
		}

		return "Unknown";
	}

	public static void main(String[] args) {

		try {

			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName()
			);

			String version = getVersion();

			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout(10, 10));
			panel.setBorder(new EmptyBorder(15, 15, 15, 15));
			panel.setBackground(Color.WHITE);

			JPanel topPanel = new JPanel(new BorderLayout(10, 0));
			topPanel.setOpaque(false);

			JLabel logo = new JLabel();

			try {

				java.net.URL url =
						CoreTuffLauncher.class
								.getClassLoader()
								.getResource("logo.png");

				if (url != null) {

					ImageIcon icon =
							new ImageIcon(url);

					Image scaled =
							icon.getImage()
									.getScaledInstance(
											48,
											48,
											Image.SCALE_SMOOTH
									);

					logo.setIcon(
							new ImageIcon(scaled)
					);
				}

			} catch (Exception ex) {

				ex.printStackTrace();
			}

			JLabel title = new JLabel("CoreTuff");
			title.setFont(new Font("Segoe UI", Font.BOLD, 18));

			JLabel versionLabel = new JLabel("v" + version);
			versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
			versionLabel.setForeground(Color.GRAY);

			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
			titlePanel.setOpaque(false);

			titlePanel.add(title);
			titlePanel.add(versionLabel);

			topPanel.add(logo, BorderLayout.WEST);
			topPanel.add(titlePanel, BorderLayout.CENTER);

			JEditorPane content = new JEditorPane();

			content.setContentType("text/html");
			content.setEditable(false);
			content.setOpaque(false);

			content.setText("""
					    <html>
					    <body style='font-family:Segoe UI; font-size:12px;'>
					
					         CoreTuff is a Minecraft plugin.<br>
					         LAMO Place this jar in your server's <b>plugins</b> folder XD.<br>
					        <br>
					        You cannot run this file directly, I swear on God.<br><br>
					
					        Built by <b>Voxelware Studios</b> with ❤<br><br>
					
					        <a href='https://www.voxelware.org/'>Website</a><br>
					        <a href='https://discord.gg/dnV3fazVRF'>Discord Server</a>
					
					    </body>
					    </html>
					""");

			content.addHyperlinkListener(e -> {

				if (e.getEventType()
						== javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {

					try {

						if (Desktop.isDesktopSupported()) {

							Desktop.getDesktop()
									.browse(
											new URI(
													e.getURL()
															.toString()
											)
									);
						}

					} catch (Exception ex) {

						ex.printStackTrace();
					}
				}
			});

			panel.add(topPanel, BorderLayout.NORTH);
			panel.add(content, BorderLayout.CENTER);

			JOptionPane.showMessageDialog(
					null,
					panel,
					"CoreTuff v" + version,
					JOptionPane.PLAIN_MESSAGE
			);

		} catch (Exception e) {

			e.printStackTrace();

			JOptionPane.showMessageDialog(
					null,
					e.toString(),
					"CoreTuff Launcher Error",
					JOptionPane.ERROR_MESSAGE
			);
		}
	}
}