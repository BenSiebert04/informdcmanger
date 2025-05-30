package com.sie.informDcManger;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PlayerCountUpdater extends JavaPlugin implements Listener {

    private String baseUrl;
    private String containerName;
    private static final String JSON_TEMPLATE = "{\"currentPlayers\":%d,\"maxPlayers\":%d}";

    @Override
    public void onEnable() {
        // Konfiguration laden
        saveDefaultConfig();
        baseUrl = getConfig().getString("baseUrl", "http://192.168.178.108:12346");
        containerName = getConfig().getString("containerName", "minecraft-paper-server");

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerCountUpdater aktiviert! Verbinde mit: " + baseUrl + ", Container: " + containerName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerCount();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(this, this::updatePlayerCount, 1);
    }

    private void updatePlayerCount() {
        int playerCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        sendUpdateRequest(playerCount, maxPlayers);
    }

    private void sendUpdateRequest(int playerCount, int maxPlayers) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // Container-Name URL-encoden
                String encodedContainerName = URLEncoder.encode(containerName, StandardCharsets.UTF_8.toString());
                String fullUrl = baseUrl + "/api/containers/" + encodedContainerName + "/players-with-max";

                URL url = new URL(fullUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                String jsonBody = String.format(JSON_TEMPLATE, playerCount, maxPlayers);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    getLogger().info("Spieleranzahl (" + playerCount + "/" + maxPlayers + ") erfolgreich aktualisiert");
                } else {
                    getLogger().warning("API-Fehler: " + responseCode + " - " + connection.getResponseMessage());
                }

                connection.disconnect();
            } catch (Exception e) {
                getLogger().warning("Kommunikationsfehler: " + e.getMessage());
            }
        });
    }
}