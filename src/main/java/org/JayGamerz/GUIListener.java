package org.JayGamerz;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIListener implements Listener {
    private final MenuManager menuManager;

    public GUIListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(menuManager.getGUITitle())) {
            event.setCancelled(true);
            if (event.getRawSlot() == 13) {
                Player player = (Player) event.getWhoClicked();
                player.performCommand("rtpqueue join");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(menuManager.getGUITitle())) {
            menuManager.removePlayerFromGUIList(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menuManager.removePlayerFromGUIList(event.getPlayer().getUniqueId());
    }
}