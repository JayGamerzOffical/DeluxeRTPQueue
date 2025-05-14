package org.JayGamerz.managers;

import org.JayGamerz.DeluxeRTPQueue;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MenuManager implements Listener {
    // GUI size constant (3 rows)s
    private static final int GUI_SIZE = 27;
    private final DeluxeRTPQueue plugin;
    private final Set<UUID> openGUIs = new HashSet<>();
    private final Map<UUID, String> confirmationMap = new HashMap<>();
    Map<UUID, List<String>> pendingActions = new HashMap<>();
    private YamlConfiguration menuConfig;

    public MenuManager(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * (Re)loads menu.yml from disk
     */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Opens the 3-row GUI, fills with background + world icons
     */
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, getGUITitle());

        // 1) Fill background
        ItemStack bg = createItem("background");
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, bg);
        }

        // 2) Get enabled worlds from config
        List<String> enabledWorlds = plugin.getConfig().getStringList("rtp_enabled_worlds");
        Set<String> enabledWorldSet = new HashSet<>(enabledWorlds);

        // 3) Prepare config section
        ConfigurationSection worldsSection = menuConfig.getConfigurationSection("worlds");
        if (worldsSection == null) {
            worldsSection = menuConfig.createSection("worlds");
        }

        // 4) Get current config keys
        Set<String> existingKeys = new HashSet<>(worldsSection.getKeys(false));

        // 5) Determine if config update is needed
        boolean configNeedsUpdate = !existingKeys.equals(enabledWorldSet);

        if (configNeedsUpdate) {
            // Remove keys not in enabled list
            for (String key : existingKeys) {
                if (!enabledWorldSet.contains(key)) {
                    worldsSection.set(key, null);
                }
            }

            // Add/update enabled world entries
            for (String worldName : enabledWorlds) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                String type;
                switch (world.getEnvironment()) {
                    case NORMAL:
                        type = "Overworld";
                        break;
                    case NETHER:
                        type = "Nether";
                        break;
                    case THE_END:
                        type = "End";
                        break;
                    default:
                        type = "Unknown";
                        break;
                }

                ConfigurationSection section = worldsSection.getConfigurationSection(worldName);
                if (section == null) {
                    section = worldsSection.createSection(worldName);

                    switch (type) {
                        case "Overworld":
                            section.set("slot", 11);
                            section.set("material", "GRASS_BLOCK");
                            section.set("name", "&aOverworld");
                            section.set("lore", Arrays.asList("&7Teleport to the Overworld"));
                            break;
                        case "Nether":
                            section.set("slot", 13);
                            section.set("material", "NETHERRACK");
                            section.set("name", "&cNether");
                            section.set("lore", Arrays.asList("&7Teleport to the Nether"));
                            break;
                        case "End":
                            section.set("slot", 15);
                            section.set("material", "END_STONE");
                            section.set("name", "&5The End");
                            section.set("lore", Arrays.asList("&7Teleport to the End"));
                            break;
                        default:
                            break;
                    }
                }

                section.set("type", type);
            }

            plugin.saveConfig();
        }

        // 6) Load items to GUI
        for (String key : worldsSection.getKeys(false)) {
            String path = "worlds." + key;
            int slot = menuConfig.getInt(path + ".slot", 13);
            inv.setItem(slot, createItem(path));
        }

        openGUIs.add(player.getUniqueId());
        player.openInventory(inv);
    }


    /**
     * Called by InventoryClickEvent to clean up
     */
    private void closeGUI(UUID uuid) {
        openGUIs.remove(uuid);
    }

    /**
     * Utility: builds an ItemStack from menu.yml at given path
     */
    private ItemStack createItem(String path) {
        String matName = menuConfig.getString(path + ".material", "STONE");
        Material mat = null;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (Throwable t) {
            if (path.contains("background")) {
                mat = Material.valueOf("STAINED_GLASS_PANE");
            }
        }
        if (mat == null) {
            plugin.getLogger().info("Unknown/Unsupported material: " + matName + ". Using STONE instead. Please update menu.yml.");
            mat = Material.STONE;
        }
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = menuConfig.getString(path + ".name", "");
            List<String> lore = menuConfig.getStringList(path + ".lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getGUITitle() {
        return ChatColor.translateAlternateColorCodes('&',
                menuConfig.getString("title", "&aQueue Menu"));
    }

    public void openConfirmationGUI(Player player, String worldName) {
        Inventory confirmInv = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Confirm Teleport");

        // Background
        ItemStack bg = createItem("background");
        for (int i = 0; i < confirmInv.getSize(); i++) {
            confirmInv.setItem(i, bg);
        }

        // Confirm Item (Green Wool)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Yes, Teleport!");
        confirmMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to confirm teleport to " + worldName));
        confirm.setItemMeta(confirmMeta);
        confirmInv.setItem(11, confirm);

        // Cancel Item (Red Wool)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to cancel teleportation"));
        cancel.setItemMeta(cancelMeta);
        confirmInv.setItem(15, cancel);

        // Store which world the player is confirming for
        confirmationMap.put(player.getUniqueId(), worldName);
        player.openInventory(confirmInv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();


        // Confirm GUI
        if (title.equals(ChatColor.GREEN + "Confirm Teleport")) {
            e.setCancelled(true);
            int slot = e.getRawSlot();

            if (slot == 11) { // Confirm
                String worldKey = confirmationMap.remove(p.getUniqueId());
                List<String> actions = pendingActions.remove(p.getUniqueId());

                if (worldKey != null && actions != null) {
                    for (String action : actions) {
                        executeAction(p, action);
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "Something went wrong with the confirmation.");
                }
            } else if (slot == 15) { // Cancel
                p.sendMessage(ChatColor.YELLOW + "Teleport cancelled.");
                confirmationMap.remove(p.getUniqueId());
                pendingActions.remove(p.getUniqueId());
            }

            p.closeInventory();
            return;
        }

        // Main GUI
        if (!title.equals(getGUITitle())) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();

        if (e.getInventory() == null || slot >= e.getInventory().getSize()) {
            closeGUI(p.getUniqueId());
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == createItem("background").getType()) {
            return;
        }

        ConfigurationSection worlds = menuConfig.getConfigurationSection("worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                String path = "worlds." + key;
                int worldSlot = menuConfig.getInt(path + ".slot", -1);
                if (slot == worldSlot) {
                    List<String> actions = menuConfig.getStringList("worlds." + key + ".actions");
                    if (actions == null || actions.isEmpty()) {
                        p.sendMessage(ChatColor.RED + "No actions defined for this world.");
                        return;
                    }
                    pendingActions.put(p.getUniqueId(), actions);
                    openConfirmationGUI(p, key);
                    closeGUI(p.getUniqueId());
                    return;
                }
            }
        }
    }

    private void executeAction(Player p, String action) {
        if (action.startsWith("[playercommand] ")) {
            String cmd = action.substring(16);
            p.performCommand(cmd);
        } else if (action.startsWith("[consolecommand] ")) {
            String cmd = action.substring(17);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        } else if (action.startsWith("[consolecommandchance] ")) {
            String[] parts = action.substring(23).split(";", 2);
            if (parts.length == 2) {
                try {
                    double chance = Double.parseDouble(parts[0]);
                    if (Math.random() * 100 <= chance) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[1].replace("%player%", p.getName()));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (action.startsWith("[title] ")) {
            String[] parts = action.substring(8).split(";", 2);
            if (parts.length == 2) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', parts[0]), ChatColor.translateAlternateColorCodes('&', parts[1]), 10, 70, 20);
            }
        } else if (action.startsWith("[subtitle] ")) {
            String msg = ChatColor.translateAlternateColorCodes('&', action.substring(11));
            p.sendTitle("", msg, 10, 70, 20);
        } else if (action.startsWith("[actionbar] ")) {
            String msg = ChatColor.translateAlternateColorCodes('&', action.substring(12));
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
        } else if (action.startsWith("[sound] ")) {
            String soundName = action.substring(8);
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.playSound(p.getLocation(), sound, 1f, 1f);
            } catch (IllegalArgumentException ex) {
                p.sendMessage(ChatColor.RED + "Invalid sound: " + soundName);
            }
        } else if (action.startsWith("[broadcast] ")) {
            String msg = ChatColor.translateAlternateColorCodes('&', action.substring(12));
            Bukkit.broadcastMessage(msg.replace("%player%", p.getName()));
        }
    }


    /**
     * Hook this into your RTP queue manager to enqueue for `worldKey`
     */
    private void onWorldSelect(Player player, String worldKey) {
        player.performCommand("rtpqueue join " + worldKey);
    }
}
