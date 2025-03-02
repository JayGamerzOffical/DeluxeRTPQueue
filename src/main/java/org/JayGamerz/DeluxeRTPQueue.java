package org.JayGamerz;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public  class DeluxeRTPQueue extends JavaPlugin {
    final List<int[]> deltas = new ArrayList<>();
public static DeluxeRTPQueue instance;

    private List<UUID> queue = new ArrayList<>();
    private int timerTaskId = -1;
    private MenuManager menuManager;
    public Config config;

    @Override
    public void onEnable() {
        instance=this;
        initializeDeltas();
        saveDefaultConfig();
        reloadConfig();
this.config = new Config (this.getConfig ());
        this.menuManager = new MenuManager(this);

        getCommand("rtpqueue").setExecutor(new RTPQueueCommand(this, menuManager));
        getCommand("rtpreload").setExecutor(new RTPReloadCommand(this));
        Bukkit.getPluginManager().registerEvents(new GUIListener(menuManager), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DeluxeRTPPlaceholder(this).register();
        }
    }

    public void joinQueue(Player player) {
        if (!player.hasPermission("deluxertpqueue.join")) {
            player.sendMessage("§cYou don't have permission to join the queue.");
            return;
        }
        if (queue.size() >= 2) {
            player.sendMessage(config.getMessages().getQueueFull());
            return;
        }
        if (queue.contains(player.getUniqueId())) {
            player.sendMessage(config.getMessages().getAlrQueue()); // Assuming this is meant for already in queue
            return;
        }
        queue.add(player.getUniqueId());
        sendActionBar(player, config.getMessages().getActionBarJoin());
        if (queue.size() == 1) {
            sendAnnouncement(player);
        } else if (queue.size() == 2) {
           teleportPlayers ();
        }
        menuManager.updateGUI();
    }
    private void initializeDeltas() {
        // Add all valid delta combinations (dx, dz) where 3 ≤ distance ≤ 4
        deltas.addAll(List.of(
                new int[]{0, 3}, new int[]{0, -3}, new int[]{0, 4}, new int[]{0, -4},
                new int[]{3, 0}, new int[]{-3, 0}, new int[]{4, 0}, new int[]{-4, 0},
                new int[]{1, 3}, new int[]{1, -3}, new int[]{-1, 3}, new int[]{-1, -3},
                new int[]{3, 1}, new int[]{3, -1}, new int[]{-3, 1}, new int[]{-3, -1},
                new int[]{2, 3}, new int[]{2, -3}, new int[]{-2, 3}, new int[]{-2, -3},
                new int[]{3, 2}, new int[]{3, -2}, new int[]{-3, 2}, new int[]{-3, -2}
        ));
    }

    public void leaveQueue(Player player) {
        queue.remove(player.getUniqueId());
        sendActionBar(player, config.getMessages().getActionBarLeave());
        menuManager.updateGUI();
    }

    private void sendAnnouncement(Player player) {
        // Retrieve the raw message from the config and replace the placeholder with the player's name.
        String rawMessage = config.getMessages().getQueueAnnouncement().replace("%player%", player.getName());
        // Translate color codes (e.g., &a, &b) into Minecraft color formatting.
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', rawMessage);

        // Create a TextComponent from the legacy colored message.
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(coloredMessage));

        // Set a click event so players can join the queue by clicking the message.
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtpqueue join"));

        // Add a hover event with a polished tooltip message.
        message.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder (ChatColor.GOLD + "Click here to join the RTP queue!").create()
        ));

        // Send the formatted announcement to all online players.
        Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(message));
    }




    public void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        player.spigot().sendMessage( new TextComponent(ChatColor.translateAlternateColorCodes('&', (message))));
    }
    private void adjustFacing(Player player, Location target) {
        Location playerLoc = player.getLocation();
        double dx = target.getX() - playerLoc.getX();
        double dz = target.getZ() - playerLoc.getZ();
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }

        float pitch = playerLoc.getPitch();
        playerLoc.setYaw(yaw);
        playerLoc.setPitch(pitch);
        player.teleport(playerLoc);
    }
    private void teleportPlayers() {
        if (queue.size() < 2) return;

        Player p1 = Bukkit.getPlayer(queue.get(0));
        Player p2 = Bukkit.getPlayer(queue.get(1));

        if (p1 == null || p2 == null) return;

        new BukkitRunnable() {
            int timeLeft = 5;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Clear queue and update GUI immediately
                    DeluxeRTPQueue.getInstance().getQueue().clear();
                    menuManager.updateGUI();
                    // Create a new TeleportManager instance
                    TeleportManager teleportManager = new TeleportManager(DeluxeRTPQueue.this);

                    // Call the asynchronous method with a callback
                    teleportManager.findValidLocationsAsync(
                            Bukkit.getWorld(getConfigManager().getDefaultWorld()),
                            locations -> {
                                if (locations == null) {
                                    p1.sendMessage(ChatColor.RED + "Failed to find valid locations after "
                                            + getConfigManager().getMaxAttempts() + " attempts!");
                                    p2.sendMessage(ChatColor.RED + "Failed to find valid locations after "
                                            + getConfigManager().getMaxAttempts() + " attempts!");
                                } else {
                                    p1.teleport(locations[0].add (0,2,0));
                                    p2.teleport(locations[1].add (0,2,0));
                                    adjustFacing (p1,p2.getLocation ());
                                    adjustFacing (p2,p1.getLocation ());
                                    playTeleportSound(p1);
                                    playTeleportSound(p2);
                                }
                            }
                    );

                    cancel();
                } else {
                    playTimerSound(p1);
                    String countdownMessage = config.getMessages().getTeleportCountdown()
                            .replace("%time%", String.valueOf(timeLeft));
                    sendActionBar(p1, countdownMessage);
                    sendActionBar(p2, countdownMessage);
                    timeLeft--;
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }


    private static DeluxeRTPQueue getInstance() {
        return instance;
    }

    private void playTeleportSound(Player player) {
        try{
            player.playSound (player.getLocation ( ), Sound.valueOf ( "ENTITY_PLAYER_LEVELUP" ), 1.0F, 1.0F);
        }catch(Exception e){
            player.playSound (player.getLocation ( ), Sound.valueOf ("LEVEL_UP"), 1.0F, 1.0F);
        }
    }
    private void playTimerSound(Player player) {
        try{
            player.playSound (player.getLocation ( ), Sound.valueOf ( "BLOCK_NOTE_BLOCK_PLING" ), 1.0F, 1.0F);
        }catch(Exception e){
            player.playSound (player.getLocation ( ), Sound.valueOf ("NOTE_PLING"), 1.0F, 1.0F);
        }
    }

    @Override
    public void onDisable() {
        if (timerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(timerTaskId);
            timerTaskId = -1;
        }
        queue.clear();
        menuManager.updateGUI();
    }

    public int getQueueSize() {
        return queue.size();
    }


    public List<UUID> getQueue() {
        return queue;
    }

    public Config getConfigManager() {
        return config;
    }
}