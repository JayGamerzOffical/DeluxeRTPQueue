package org.JayGamerz.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.JayGamerz.DeluxeRTPQueue;
import org.JayGamerz.managers.AreaQueueManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DeluxeRTPPlaceholder extends PlaceholderExpansion {
    private final DeluxeRTPQueue plugin;

    public DeluxeRTPPlaceholder(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rtpqueue";
    }

    @Override
    public String getAuthor() {
        return "JayGamerz";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /*
        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            return null;
        }*/
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("players")) {
            return String.valueOf(getTotalQueuedPlayers());
        }

        if (params.startsWith("players_")) {
            String worldName = params.substring("players_".length());
            return String.valueOf(getWorldQueueSize(worldName));
        }

        if (params.startsWith("timeleft_")) {
            String arena = params.substring("timeleft_".length());
            return getTimeLeft(arena);
        }

        return null;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("players")) {
            return String.valueOf(getTotalQueuedPlayers());
        }

        if (params.startsWith("players_")) {
            String worldName = params.substring("players_".length());
            return String.valueOf(getWorldQueueSize(worldName));
        }

        if (params.startsWith("timeleft_")) {
            String arena = params.substring("timeleft_".length());
            return getTimeLeft(arena);
        }

        return null;
    }

    private String getTimeLeft(String arenaName) {
        AreaQueueManager.QueueTask task = plugin.getAreaQueueManager().runningQueues.get(arenaName);
        if (task != null) {
            return String.valueOf(task.getCountdown());
        } else {
            return "N/A";
        }
    }

    private int getWorldQueueSize(String worldName) {
        // Check if the world exists in the worldQueues map, return its size, or 0 if the world isn't found
        return plugin.worldQueues.getOrDefault(worldName, new HashMap<>()).size();
    }

    private int getTotalQueuedPlayers() {
        return plugin.worldQueues.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
