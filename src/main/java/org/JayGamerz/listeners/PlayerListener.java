package org.JayGamerz.listeners;

import org.JayGamerz.DeluxeRTPQueue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    DeluxeRTPQueue plugin;

    public PlayerListener(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAreaQueueManager().startQueueAreaTask();
        if (plugin.actionBarMessageEnabled) {
            if (!plugin.isActionTaskEnabled()) {
                plugin.startTasks();
            }
        }
    }

}
