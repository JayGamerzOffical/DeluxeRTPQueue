package org.JayGamerz;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
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
        return "YourName";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equals("players")) {
            return String.valueOf(plugin.getQueueSize());
        }
        return null;
    }
}