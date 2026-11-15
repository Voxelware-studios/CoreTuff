package org.voxelware.coretuff.Utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.voxelware.coretuff.CoreTuff;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String API_URL =
            "https://api.modrinth.com/v2/project/coretuff/version";
    private static final String DOWNLOAD_URL =
            "https://modrinth.com/plugin/coretuff";

    private final CoreTuff plugin;
    private String latestReleaseVersion;

    public UpdateChecker(
            CoreTuff plugin
    ) {

        this.plugin = plugin;
    }

    public void check() {

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                HttpURLConnection connection =
                                        (HttpURLConnection)
                                                new URL(
                                                        API_URL
                                                ).openConnection();

                                connection.setRequestMethod(
                                        "GET"
                                );

                                connection.setConnectTimeout(
                                        5000
                                );

                                connection.setReadTimeout(
                                        5000
                                );

                                BufferedReader reader =
                                        new BufferedReader(
                                                new InputStreamReader(
                                                        connection.getInputStream()
                                                )
                                        );

                                JsonArray array =
                                        JsonParser.parseReader(reader)
                                                .getAsJsonArray();

                                reader.close();

                                if (array.isEmpty()) {
                                    return;
                                }

                                JsonObject latest = null;
                                for (var element : array) {
                                    JsonObject version = element.getAsJsonObject();
                                    if ("release".equals(version.get("version_type").getAsString())) {
                                        latest = version;
                                        break;
                                    }
                                }

                                if (latest == null) {
                                    return;
                                }

                                String latestVersion =
                                        latest.get("version_number")
                                                .getAsString();

                                String currentVersion =
                                        plugin.getPluginMeta()
                                                .getVersion();

                                if (!currentVersion.equalsIgnoreCase(
                                        latestVersion
                                )) {

                                    latestReleaseVersion = latestVersion;

                                    plugin.getLogger()
                                            .warning(
                                                    "========================================"
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    "New CoreTuff update available!"
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    "Current Version: "
                                                            + currentVersion
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    "Latest Version: "
                                                            + latestVersion
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    "Download:"
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    DOWNLOAD_URL
                                            );

                                    plugin.getLogger()
                                            .warning(
                                                    "========================================"
                                            );
                                }

                            } catch (Exception e) {

                                plugin.getLogger()
                                        .warning(
                                                "Failed to check for updates."
                                        );
                            }
                        }
                );
    }

    public void notifyPlayer(Player player) {
        if (latestReleaseVersion == null || !player.isOp()) return;

        Component message = Component.text()
                .append(Component.text("[", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("CoreTuff", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("]: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("There is an new version ", NamedTextColor.GRAY))
                .append(Component.text(latestReleaseVersion, NamedTextColor.YELLOW))
                .append(Component.text(" availible ", NamedTextColor.GRAY))
                .append(Component.text("click here to download", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.openUrl(DOWNLOAD_URL)))
                .build();

        player.sendMessage(message);
    }
}