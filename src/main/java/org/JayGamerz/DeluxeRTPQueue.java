package org.JayGamerz;

import net.md_5.bungee.api.ChatColor;
import org.JayGamerz.commands.RTPQueueCommand;
import org.JayGamerz.hooks.DeluxeRTPPlaceholder;
import org.JayGamerz.listeners.PlayerListener;
import org.JayGamerz.listeners.ShearsRegionSelectorListener;
import org.JayGamerz.managers.*;
import org.JayGamerz.utils.SmallTextConverter;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class DeluxeRTPQueue extends JavaPlugin {
    private static DeluxeRTPQueue instance;
    public final Map<String, Queue<Location>> preloadedLocations = new ConcurrentHashMap<>();
    public Map<String, Map<UUID, Long>> worldQueues = new HashMap<>();
    public String prefix;
    public boolean smallTextEnabled;
    public boolean debug;
    public boolean actionBarMessageEnabled;
    public boolean soundsEnabled;
    public boolean levitationEffectEnabled;
    public int levitationEffectDuration;
    public List<String> restrictedBlocks;
    public boolean adjustFacingEnabled;
    private boolean actionTaskEnabled;
    private MenuManager menuManager;
    private RTPQueueCommand queueCommand;
    private SelectionManager selectionManager;
    private AreaManager areaManager;
    private AreaQueueManager areaQueueManager;
    private YamlConfiguration messagesConfig;
    private File messagesFile;

    public static Plugin getPlugin() {
        return getInstance();
    }

    public static DeluxeRTPQueue getInstance() {
        return instance;
    }


    public boolean isActionTaskEnabled() {
        return actionTaskEnabled;
    }


    public MenuManager getMenuManager() {
        return menuManager;
    }

    public AreaQueueManager getAreaQueueManager() {
        return areaQueueManager;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadMessages();
        loadFields();
        this.menuManager = new MenuManager(this);
        selectionManager = new SelectionManager();
        areaManager = new AreaManager(this);
        // Register command executors
        queueCommand = new RTPQueueCommand(this, menuManager);
        getCommand("rtpqueue").setExecutor(queueCommand);
        getCommand("rtpqueue").setTabCompleter(queueCommand);

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShearsRegionSelectorListener(selectionManager), this);

        // Register PlaceholderAPI expansion if available
        this.areaQueueManager = new AreaQueueManager(this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DeluxeRTPPlaceholder(this).register();
            getLogger().info("PlaceholderAPI detected! RTPQueue placeholders successfully registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found! Some features may not work as expected.");
        }

        startLocationPreloader();
        if (actionBarMessageEnabled) {
            startTasks();
        }
    }

    public void loadFields() {
        this.prefix = messagesConfig.getString("prefix", "&e[RTP Queue] &7 >> ");
        this.debug = getConfig().getBoolean("debug", false);
        this.soundsEnabled = getConfig().getBoolean("sounds-enabled", true);
        this.restrictedBlocks = getConfig().getStringList("restricted-blocks");
        this.adjustFacingEnabled = getConfig().getBoolean("adjust-face", true);
        this.levitationEffectDuration = getConfig().getInt("levitation-effect-duration", 5);
        this.actionBarMessageEnabled = getConfig().getBoolean("action-bar-enabled", true);
        this.levitationEffectEnabled = getConfig().getBoolean("levitation-effect-enabled", true);
        this.smallTextEnabled = this.getConfig().getBoolean("small-text-enabled", true);
    }

    public void startLocationPreloader() {
        List<String> worlds = getConfig().getStringList("rtp_enabled_worlds");
        new BukkitRunnable() {
            @Override
            public void run() {
                int totalLocations = 0;

                for (String worldName : worlds) {
                    preloadedLocations.putIfAbsent(worldName, new ConcurrentLinkedQueue<>());
                    totalLocations += preloadedLocations.get(worldName).size();
                }

                // Decide delay based on how full the queues are
                long delay;
                if (totalLocations < 3) {
                    delay = 100L;  // fast
                } else if (totalLocations < 5) {
                    delay = 150L;  // fast
                } else if (totalLocations < 10) {
                    delay = 200L;  // fast
                } else if (totalLocations < 15) {
                    delay = 500; // medium
                } else if (totalLocations < 20) {
                    delay = 700; // medium
                } else {
                    delay = 800; // slow
                }


                for (String worldName : worlds) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    Queue<Location> queue = preloadedLocations.get(worldName);
                    if (queue.size() >= 100) continue;

                    int radius = getConfig().getInt("radius", 1000);

                    double x = ThreadLocalRandom.current().nextInt(-radius, radius);
                    double z = ThreadLocalRandom.current().nextInt(-radius, radius);

                    Bukkit.getScheduler().callSyncMethod(DeluxeRTPQueue.getInstance(), () -> {
                        Chunk chunk = world.getChunkAt((int) x >> 4, (int) z >> 4);
                        if (!chunk.isLoaded()) return null;

                        int y = world.getHighestBlockYAt((int) x, (int) z) + 5;
                        Location loc = new Location(world, x, y, z);
                        getLogger().info("Preloading " + loc);
                        if (isSafeLocation(loc.clone().subtract(0, 5, 0))) {
                            getLogger().info("loaded " + loc);
                            queue.add(loc);
                        }
                        return null;
                    });

                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        startLocationPreloader(); // recursive re-run
                    }
                }.runTaskLaterAsynchronously(DeluxeRTPQueue.getInstance(), delay); // delay in ticks
            }
        }.runTaskAsynchronously(this); // start instantly
    }


    public boolean isSafeLocation(Location loc) {
        Material blockType = loc.getBlock().getType();
        return !restrictedBlocks.contains(blockType.toString().toUpperCase());
    }

    public void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    @Override
    public void onDisable() {
        areaQueueManager.runningQueues.values().forEach(AreaQueueManager.QueueTask::cancel);
    }

    public String getMessage(String path) {
        if (path.contains("messages.")) {
            String message = messagesConfig.getString(path);
            if (message == null || message.isEmpty()) {
                return "Missing message on path: " + path;
            }
            return smallTextEnabled ? SmallTextConverter.toSmallCaps(ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', message))) : ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', message));
        } else {
            String message = messagesConfig.getString("messages." + path);
            if (message == null || message.isEmpty()) {
                return "Missing message on path: " + path;
            }
            return smallTextEnabled ? SmallTextConverter.toSmallCaps(ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', message))) : ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public String getMessage(String path, String arenaName) {

        String message = messagesConfig.getString(path);
        if (message == null || message.isEmpty()) {
            return "Missing message on path: " + path;
        }
        return smallTextEnabled ? SmallTextConverter.toSmallCaps(ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', (message).replaceAll("%area%", arenaName)))) :
                ColorManager.colorize(prefix + ChatColor.translateAlternateColorCodes('&', (message).replaceAll("%area%", arenaName)));

    }

    public String getMessage(String path, String time, boolean op) {

        String message = messagesConfig.getString(path);
        if (message == null || message.isEmpty()) {
            return "Missing message on path: " + path;
        }
        return smallTextEnabled ? SmallTextConverter.toSmallCaps(ChatColor.translateAlternateColorCodes('&', (message).replaceAll("%time%", String.valueOf(time)))) :
                ChatColor.translateAlternateColorCodes('&', (message).replaceAll("%time%", String.valueOf(time)));

    }

    private void updateActionBar() {
        for (Map.Entry<String, Map<UUID, Long>> worldEntry : this.worldQueues.entrySet()) {
            Map<UUID, Long> queue = worldEntry.getValue();

            for (Map.Entry<UUID, Long> entry : queue.entrySet()) {
                UUID playerId = entry.getKey();
                long joinTime = entry.getValue();
                Player player = Bukkit.getPlayer(playerId);

                if (player != null && player.isOnline()) {
                    long elapsedMillis = System.currentTimeMillis() - joinTime;
                    long elapsedSeconds = elapsedMillis / 1000L;
                    long minutes = elapsedSeconds / 60L;
                    long seconds = elapsedSeconds % 60L;
                    String timeString = String.format("§a[%02d:%02d]", minutes, seconds);

                    String message = getMessage("messages.join_queue_time", " " + timeString, true);
                    sendActionBar(player, ColorManager.colorize(message));
                    if (soundsEnabled) {
                        try {
                            player.playSound(player.getLocation(), Sound.valueOf("NOTE_PLING"), 1F, 1F);
                        } catch (Throwable e) {
                            player.playSound(player.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1F, 1F);
                        }
                    }
                }
            }
        }
    }


    public void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(message));
        } catch (Throwable e) {
            player.sendTitle("", message);
        }
    }

    public void startTasks() {
        new BukkitRunnable() {
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    actionTaskEnabled = false;
                    cancel();
                    return;
                }
                actionTaskEnabled = true;
                updateActionBar();
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void checkQueue(String worldName) {
        Map<UUID, Long> queue = worldQueues.get(worldName);
        if (queue == null || queue.size() < 2) return;

        UUID[] playersArray = queue.keySet().toArray(new UUID[0]);
        UUID player1 = playersArray[0];
        UUID player2 = playersArray[1];
        queue.remove(player1);
        queue.remove(player2);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null && p2 != null) {
            teleportPlayers(p1, p2, worldName); // Also pass worldName here
        }
    }

    private void teleportPlayers(Player player1, Player player2, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player1.sendMessage(getMessage("noLocationFound"));
            player2.sendMessage(getMessage("noLocationFound"));
            getLogger().warning("World '" + worldName + "' not found!");
            return;
        }

        Queue<Location> queue = preloadedLocations.getOrDefault(worldName, new ConcurrentLinkedQueue<>());
        if (queue.size() < 2) return;

        Location loc1 = queue.poll();
        Location loc2 = loc1.clone().add(10, 0, 0);

        if (!isSafeLocation(loc2)) {
            loc2 = queue.poll();
            if (loc2 == null) {
                player1.sendMessage(getMessage("teleportError"));
                player2.sendMessage(getMessage("teleportError"));
                return;
            }
        }

        loc1.add(0, 4, 0);
        loc2.add(0, 4, 0);

        if (isDebug())
            getLogger().info("Teleporting " + player1.getName() + " to " + loc1 + " and " + player2.getName() + " to " + loc2);

        if (player1.teleport(loc1) && player2.teleport(loc2)) {
            if (levitationEffectEnabled) {
                try {
                    player1.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationEffectDuration * 20, getConfig().getInt("levitation-effect-amplifier", 0)));
                    player2.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationEffectDuration * 20, getConfig().getInt("levitation-effect-amplifier", 0)));
                } catch (Throwable e) {
                    fakeLevitation(player1, levitationEffectDuration * 20, getConfig().getDouble("levitation-effect-amplifier", 0.1) * 0.1);
                    fakeLevitation(player2, levitationEffectDuration * 20, getConfig().getDouble("levitation-effect-amplifier", 0.1) * 0.1);
                }
            }
            if (adjustFacingEnabled) {
                adjustFacing(player1, loc2);
                adjustFacing(player2, loc1);
            }
            if (soundsEnabled) {
                try {
                    player1.playSound(player1.getLocation(), Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"), 1F, 1F);
                    player2.playSound(player2.getLocation(), Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"), 1F, 1F);
                } catch (Throwable e) {
                    player1.playSound(player1.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 1F, 1F);
                    player2.playSound(player2.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 1F, 1F);
                }
            }
            player1.sendMessage(getMessage("teleportMessage"));
            player2.sendMessage(getMessage("teleportMessage"));
        } else {
            player1.sendMessage(getMessage("teleportError"));
            player2.sendMessage(getMessage("teleportError"));
            if (isDebug()) getLogger().warning("Teleport failed for one or both players.");
        }
    }


    public void fakeLevitation(Player player, int durationTicks, double strength) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                // Apply small upward velocity
                Vector velocity = player.getVelocity();
                velocity.setY(strength);
                player.setVelocity(velocity);

                ticks += 5; // running every 5 ticks
            }
        }.runTaskTimer(this, 0L, 5L); // run every 5 ticks (~0.25 second)
    }

    public void adjustFacing(Player player, Location target) {
        Location playerLoc = player.getLocation();
        double dx = target.getX() - playerLoc.getX();
        double dz = target.getZ() - playerLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
        float pitch = playerLoc.getPitch();
        playerLoc.setYaw(yaw);
        playerLoc.setPitch(pitch);
        player.teleport(playerLoc);
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public String getPrefix() {
        return prefix;
    }

}
