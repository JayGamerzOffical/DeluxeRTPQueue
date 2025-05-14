package org.JayGamerz.listeners;

import org.JayGamerz.DeluxeRTPQueue;
import org.JayGamerz.managers.SelectionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShearsRegionSelectorListener implements Listener {
    private final SelectionManager selectionManager;
    private static Material selectionMat;

    public ShearsRegionSelectorListener(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
        reloadSelectionMaterial ( );
    }

    public static Material getSelectionMat() {
        return selectionMat;
    }

    public static void reloadSelectionMaterial() {
        selectionMat = Material.valueOf (DeluxeRTPQueue.getInstance ( ).getConfig ( ).getString ("selection-item").toUpperCase ( ));
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        if ( selectionMat == null || e.getItem ( ) == null ) {
            return;
        }
        if ( !e.hasItem ( ) || e.getItem ( ).getType ( ) != selectionMat || !e.getPlayer ( ).hasPermission ("deluxertpqueue.admin") )
            return;

        Player player = e.getPlayer ( );
        Action action = e.getAction ( );
        Block clicked = e.getClickedBlock ( );
        if ( clicked == null ) return;

        if ( action == Action.LEFT_CLICK_BLOCK ) {
            selectionManager.setFirstPos (player, clicked.getLocation ( ));
            player.sendMessage (DeluxeRTPQueue.getInstance ( ).getMessage ("messages.first_pos_selected"));
            e.setCancelled (true);
        } else if ( action == Action.RIGHT_CLICK_BLOCK ) {
            selectionManager.setSecondPos (player, clicked.getLocation ( ));
            player.sendMessage (DeluxeRTPQueue.getInstance ( ).getMessage ("messages.second_pos_selected"));
            e.setCancelled (true);
        }
    }
}
