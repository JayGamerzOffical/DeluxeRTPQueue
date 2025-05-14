package org.JayGamerz.managers;

import org.JayGamerz.DeluxeRTPQueue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AreaManager {
    private final DeluxeRTPQueue plugin;
    private final Map<String, Area> areas = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public AreaManager(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
        loadFile();
        loadAreas();
    }

    public void loadFile() {
        file = new File(plugin.getDataFolder(), "areas.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAreas() {
        if (config.getConfigurationSection("areas") == null) return;
        for (String areaName : config.getConfigurationSection("areas").getKeys(false)) {
            String path = "areas." + areaName + ".";
            String worldName = config.getString(path + "world");
            int time = config.getInt(path + "time");
            double x1 = config.getDouble(path + "x1");
            double y1 = config.getDouble(path + "y1");
            double z1 = config.getDouble(path + "z1");
            double x2 = config.getDouble(path + "x2");
            double y2 = config.getDouble(path + "y2");
            double z2 = config.getDouble(path + "z2");

            if (worldName == null) continue;
            Location pos1 = new Location(Bukkit.getWorld(worldName), x1, y1, z1);
            Location pos2 = new Location(Bukkit.getWorld(worldName), x2, y2, z2);

            areas.put(areaName, new Area(areaName, pos1, pos2, time));
        }
    }

    public void teleportToArea(Player player, String areaName) {
        Area area = getArea(areaName);
        if (area == null) {
            player.sendMessage(plugin.getMessage("messages.area_not_found", areaName));
            return;
        }

        Location randomLoc = area.getRandomLocation();
        player.teleport(randomLoc);
        player.sendMessage(plugin.getMessage("messages.teleported_to_area", areaName));
    }

    public void saveArea(String name, Location pos1, Location pos2, int time) {
        String path = "areas." + name + ".";
        config.set(path + "world", pos1.getWorld().getName());
        config.set(path + "time", time);
        config.set(path + "x1", pos1.getX());
        config.set(path + "y1", pos1.getY());
        config.set(path + "z1", pos1.getZ());
        config.set(path + "x2", pos2.getX());
        config.set(path + "y2", pos2.getY());
        config.set(path + "z2", pos2.getZ());

        areas.put(name, new Area(name, pos1, pos2, time));
        plugin.getAreaQueueManager().startQueueTask(name, time);
        saveFile();
    }

    public boolean addAreaFromSelection(Player player, String areaName, SelectionManager selectionManager, int time) {
        Location pos1 = selectionManager.getFirst(player);
        Location pos2 = selectionManager.getSecond(player);
        if (pos1 == null || pos2 == null) return false;

        selectionManager.clearSelection(player);
        saveArea(areaName, pos1, pos2, time);
        return true;
    }

    public Area getArea(String name) {
        return areas.getOrDefault(name, null);
    }

    public Set<String> getAllAreaNames() {
        return areas.keySet();
    }

    public void removeArea(String name) {
        areas.remove(name);
        config.set("areas." + name, null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Area> getAllActiveAreas() {
        return new ArrayList<>(areas.values());
    }

    public static class Area {
        private final String name;
        private final Location pos1;
        private final Location pos2;
        private int time;


        public Area(String name, Location pos1, Location pos2, int time) {
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public Location getPos1() {
            return pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public Location getRandomLocation() {
            Random random = new Random();
            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            double x = minX + (maxX - minX) * random.nextDouble();
            double y = minY + (maxY - minY) * random.nextDouble();
            double z = minZ + (maxZ - minZ) * random.nextDouble();

            return new Location(pos1.getWorld(), x, y, z);
        }


        public boolean isInside(Location loc) {
            if (loc.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) return false;

            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            return x >= Math.min(pos1.getX(), pos2.getX()) && x <= Math.max(pos1.getX(), pos2.getX())
                    && y >= Math.min(pos1.getY(), pos2.getY()) && y <= Math.max(pos1.getY(), pos2.getY())
                    && z >= Math.min(pos1.getZ(), pos2.getZ()) && z <= Math.max(pos1.getZ(), pos2.getZ());
        }

        public int getAreaTime() {
            return time;
        }
    }
}
