package org.JayGamerz;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
public class Config {
    private String defaultWorld;
    private Messages messages;
    private int maxAttempts;
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private List<Material> safeMaterials;

    public Config(org.bukkit.configuration.Configuration config) {
        reload(config);
    }

    public void reload(org.bukkit.configuration.Configuration config) {
        defaultWorld = config.getString("default_world", "world");
        messages = new Messages(config.getConfigurationSection("messages"));
        maxAttempts = config.getInt("teleport.max_attempts", 5);
        minX = config.getInt("teleport.min_x", -1000);
        maxX = config.getInt("teleport.max_x", 1000);
        minZ = config.getInt("teleport.min_z", -1000);
        maxZ = config.getInt("teleport.max_z", 1000);
        safeMaterials = new ArrayList<>();
        for (String materialName : config.getStringList("safe_location_checks")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) safeMaterials.add(material);
        }
    }

    public String getDefaultWorld() {
        return defaultWorld;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Messages getMessages() {
        return messages;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public List<Material> getSafeMaterials() {
        return safeMaterials;
    }

    public String getMessage(String s) {
        return (messages.getPrefix ())+ (DeluxeRTPQueue.instance.getConfig ().getString(s));
    }
}