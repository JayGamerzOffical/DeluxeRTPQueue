package org.JayGamerz;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MenuManager {
    private final DeluxeRTPQueue plugin;
    private YamlConfiguration menuConfig;
    private final Set<UUID> playersWithGUIOpen = new HashSet<>();

    public MenuManager(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) plugin.saveResource("menu.yml", false);
        menuConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, getGUITitle());

        // Fill background
        ItemStack bgItem = createItem("background");
        for (int i = 0; i < 27; i++) inv.setItem(i, bgItem);

        // Center item
        if (plugin.getQueueSize() > 0) {
            UUID queueUUID = plugin.getQueue().get(0);
            Player queuePlayer = Bukkit.getPlayer(queueUUID);
            if (queuePlayer != null) {
                inv.setItem(13, createMiddleItem (queuePlayer));
            }
        } else {
            inv.setItem(13, createItem("center-item-0"));
        }

        playersWithGUIOpen.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void removePlayerFromGUIList(UUID uuid) {
        playersWithGUIOpen.remove(uuid);
    }

    private ItemStack createItem(String path) {
        try {
            // Retrieve the material, display name, and lore from the config.
            String materialName = menuConfig.getString(path + ".material", "STAINED_GLASS_PANE");
            String name = menuConfig.getString(path + ".name", "");
            List<String> lore = menuConfig.getStringList(path + ".lore");

            // Match the material using Bukkit's Material enum. If not found, default to STONE.
            Material mat = Material.matchMaterial(materialName.toUpperCase());

            if (mat == null) {
                mat = Material.valueOf ("GRAY_STAINED_GLASS_PANE");
            }

            ItemStack item = new ItemStack(mat, 1);

            // Get and update the item's metadata.
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                List<String> coloredLore = lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                meta.setLore(coloredLore);
                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
return new ItemStack (Material.valueOf ("STAINED_GLASS_PANE"), 1);
        }
    }


    private ItemStack createMiddleItem(Player player) {
        try {
            // Create a Nether Star as the middle item.
            ItemStack item = new ItemStack(Material.matchMaterial (menuConfig.getString ("center-item-1.material").toUpperCase (  )), 1);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                throw new IllegalStateException("ItemMeta is null. Cannot create middle item.");
            }

            // Retrieve the display name from configuration and replace the placeholder with the player's name.
            String displayName = menuConfig.getString("center-item-1.name", "&e%player%");
            displayName = displayName.replace("%player%", player.getName());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            // Set a lore line indicating the RTP waiting status.
            List<String> lore = new ArrayList<>();
            for (String loreName : menuConfig.getStringList("center-item-1.lore")) {
                loreName = loreName.replace("%rtpqueue_player%", player.getName ());
                lore.add(ChatColor.translateAlternateColorCodes('&', loreName));
            }
            meta.setLore(lore);
            // Apply the updated metadata back to the item.
            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            return new ItemStack(Material.DIAMOND);
        }
    }


    public void updateGUI() {
        for (UUID uuid : new HashSet<>(playersWithGUIOpen)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && player.getOpenInventory() != null) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv != null && inv.getViewers().contains(player)) {
                    inv.clear();

                    ItemStack bgItem = createItem("background");
                    for (int i = 0; i < 27; i++) inv.setItem(i, bgItem);

                    if (plugin.getQueueSize() > 0) {
                        UUID queueUUID = plugin.getQueue().get(0);
                        Player queuePlayer = Bukkit.getPlayer(queueUUID);
                        if (queuePlayer != null) {
                            inv.setItem(13, createMiddleItem (queuePlayer));
                        }
                    } else {
                        inv.setItem(13, createItem("center-item-0"));
                    }
                }
            } else {
                playersWithGUIOpen.remove(uuid);
            }
        }
    }

    public String getGUITitle() {
        return ChatColor.translateAlternateColorCodes('&', menuConfig.getString("title", "&aQueue Menu"));
    }
}