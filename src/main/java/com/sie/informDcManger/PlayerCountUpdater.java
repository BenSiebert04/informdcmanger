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
import java.nio.charset.StandardCharsets;

public class PlayerCountUpdater extends JavaPlugin implements Listener {

    private static final String API_URL = "http://192.168.178.108:8080/api/applications/Minecraft-Paper";
    private static final String JSON_TEMPLATE = "{\"currentPlayers\":%d}";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerCountUpdater aktiviert! Verbinde mit: " + API_URL);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerCount();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Kurze Verzögerung, damit die Spieleranzahl nach dem Quit aktualisiert ist
        Bukkit.getScheduler().runTaskLater(this, this::updatePlayerCount, 1);
    }

    private void updatePlayerCount() {
        int playerCount = Bukkit.getOnlinePlayers().size();
        sendUpdateRequest(playerCount);
    }

    private void sendUpdateRequest(int playerCount) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                String jsonBody = String.format(JSON_TEMPLATE, playerCount);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    getLogger().info("Spieleranzahl (" + playerCount + ") erfolgreich aktualisiert");
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