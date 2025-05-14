package org.JayGamerz.managers;

import org.JayGamerz.DeluxeRTPQueue;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AreaQueueManager {
    public static AreaQueueManager instance;
    public Map<String, QueueTask> runningQueues = new HashMap<>();
    AreaManager areaManager;
    private DeluxeRTPQueue plugin;
    private HashMap<UUID, String> playersData = new HashMap<>();
    private boolean queueEnabled;

    public AreaQueueManager(DeluxeRTPQueue plugin) {
        instance = this;
        this.plugin = plugin;
        this.areaManager = plugin.getAreaManager();
        queueEnabled = false;
    }

    public static AreaQueueManager getInstance() {
        return instance;
    }

    public void startQueueAreaTask() {
        if (queueEnabled) return;
        for (AreaManager.Area areaName : areaManager.getAllActiveAreas()) {
            startQueueTask(areaName.getName(), areaName.getAreaTime());
        }
        queueEnabled = true;
    }

    public void startQueueTask(String areaName, int seconds) {
        QueueTask task = new QueueTask(areaName, seconds);
        task.runTaskTimerAsynchronously(plugin, 0L, 20L);
        runningQueues.put(areaName, task);
    }

    List<Player> getPlayersInArea(String areaName) {
        AreaManager.Area area = areaManager.getArea(areaName);
        if (area == null) return Collections.emptyList();

        List<Player> inArea = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (area.isInside(p.getLocation())) {
                inArea.add(p);
                playersData.put(p.getUniqueId(), areaName);
            }
        }
        return inArea;
    }

    public void sendCountdownHotbar(Player p, int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder formattedTime = new StringBuilder();
        if (hours > 0) formattedTime.append(hours).append("h ");
        if (minutes > 0) formattedTime.append(minutes).append("m ");
        if (secs > 0 || formattedTime.length() == 0) formattedTime.append(secs).append("s");
        if (plugin.soundsEnabled) {
            try {
                p.playSound(p.getLocation(), Sound.valueOf("NOTE_PLING"), 1F, 1F);
            } catch (Throwable e) {
                p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1F, 1F);
            }
        }
        plugin.sendActionBar(p, plugin.getMessage("messages.teleport_countdown", formattedTime.toString().trim(), true));
    }

    private boolean isInArea(Player p, String area) {
        return getPlayersInArea(area).contains(p);
    }

    public void teleportPlayersToAreaAsync(List<Player> players) {
        final World world = players.get(0).getWorld();
        if (world == null) {
            players.forEach(p -> p.sendMessage(getMessage("noLocationFound")));
            plugin.getLogger().warning("World '" + getConfig().getString("world") + "' not found!");
            return;
        }

        // Build your restricted-blocks set once
        Set<Material> restricted = getConfig().getStringList("restricted-blocks").stream()
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Unknown material in restricted-blocks: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        asyceSubTeleportation(players, world, restricted);
    }

    private void asyceSubTeleportation(List<Player> players, World world, Set<Material> restricted) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int areaRadius = (int) (world.getWorldBorder().getSize() / 2.0);
            if (plugin.isDebug()) plugin.getLogger().info("Area radius: " + areaRadius);

            double spacing = getConfig().getDouble("spawn-spacing", 5.0);
            double circleRadius = Math.max(
                    getConfig().getDouble("circle-radius", 10.0),
                    spacing * players.size() / (2 * Math.PI)
            );
            if (plugin.isDebug()) plugin.getLogger().info("Calculated circle radius: " + circleRadius);

            Queue<Location> queue = plugin.preloadedLocations.getOrDefault(world.getName(), new ConcurrentLinkedQueue<>());
            Location center = queue.poll();
            if (plugin.isDebug()) plugin.getLogger().info("Initial center location: " + center);

            if (center == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    players.forEach(p -> p.sendMessage(getMessage("teleportError")));
                    if (plugin.isDebug()) plugin.getLogger().warning("Failed to find a safe center.");
                });
                return;
            }

            Map<UUID, Location> targets = new HashMap<>();
            int n = players.size();

            for (int i = 0; i < n; i++) {
                double angle = 2 * Math.PI * i / n;
                Location found = null;

                for (int attempt = 0; attempt < 20; attempt++) {
                    double x = center.getX() + circleRadius * Math.cos(angle);
                    double z = center.getZ() + circleRadius * Math.sin(angle);
                    int y = 0;
                    try {
                        y = Bukkit.getScheduler().callSyncMethod(plugin, () -> world.getHighestBlockYAt((int) x, (int) z) + 5).get();
                    } catch (InterruptedException | ExecutionException e) {
                        if (plugin.isDebug())
                            plugin.getLogger().warning("Y-coordinate fetch failed: " + e.getMessage());
                    }

                    Location candidate = new Location(world, x, y, z);
                    if (isSafeLocation(candidate.clone().subtract(0, 5, 0).getBlock())) {
                        found = candidate;
                        if (plugin.isDebug())
                            plugin.getLogger().info("Found safe location for player " + players.get(i).getName() + ": " + candidate);
                        break;
                    }

                    angle += (new Random().nextDouble() - 0.5) * (Math.PI / 8);
                }

                if (found != null) {
                    targets.put(players.get(i).getUniqueId(), found);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player p : players) {
                    Location dest = targets.get(p.getUniqueId());
                    if (dest != null && p.teleport(dest)) {
                        if (plugin.levitationEffectEnabled) {
                            try {
                                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, plugin.levitationEffectDuration * 20, getConfig().getInt("levitation-effect-amplifier", 0)));
                            } catch (Throwable e) {
                                plugin.fakeLevitation(p, plugin.levitationEffectDuration * 20, getConfig().getDouble("levitation-effect-amplifier", 0.1) * 0.1);
                            }
                        }
                        if (plugin.adjustFacingEnabled) {
                            plugin.adjustFacing(p, center);
                        }
                        p.sendMessage(getMessage("teleportMessage"));
                        if (plugin.soundsEnabled) {
                            try {
                                p.playSound(p.getLocation(), Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"), 1F, 1F);
                            } catch (Throwable e) {
                                p.playSound(p.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 1F, 1F);
                            }
                        }
                        if (plugin.isDebug()) plugin.getLogger().info("Teleported " + p.getName() + " to " + dest);
                    } else {
                        p.sendMessage(getMessage("teleportError"));
                        if (plugin.isDebug()) plugin.getLogger().warning("Failed to teleport " + p.getName());
                    }
                }
            });
        });
    }

    /**
     * Check if location is safe (no restricted blocks for a few blocks below).
     */
    private boolean isSafeLocation(Block block) {
        return !plugin.restrictedBlocks.contains(block.getType().toString().toUpperCase());
    }

    private Configuration getConfig() {
        return plugin.getConfig();
    }

    private String getMessage(String teleportError) {
        return plugin.getMessage(teleportError);
    }

    public class QueueTask extends BukkitRunnable {
        private final String areaName;
        AreaManager.Area area;
        private int countdown;

        QueueTask(String areaName, int seconds) {
            this.areaName = areaName;
            this.countdown = seconds;
            area = areaManager.getArea(areaName);
        }

        public int getCountdown() {
            return countdown;
        }

        @Override
        public void run() {
            if (area == null) {
                runningQueues.remove(areaName);
                cancel();
                return;
            }
            List<Player> players = getPlayersInArea(areaName);


            for (Player p : players) {
                if (!area.isInside(p.getLocation())) {
                    plugin.sendActionBar(p, plugin.getMessage("leave_queue"));
                    playersData.remove(p.getUniqueId());
                }
                if (plugin.actionBarMessageEnabled) {
                    sendCountdownHotbar(p, countdown);
                }
            }

            countdown--;
            if (countdown < 0) {
                if (players.isEmpty()) {
                    runningQueues.remove(areaName);
                    startQueueTask(area.getName(), area.getAreaTime());
                    cancel();
                    return;
                }
                teleportPlayersToAreaAsync(players);
                for (Player p : players) {
                    playersData.remove(p.getUniqueId());
                }
                cancel();
                runningQueues.remove(areaName);
                startQueueTask(area.getName(), area.getAreaTime());
            }
        }
    }


}
