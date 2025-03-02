package org.JayGamerz;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RTPQueueCommand implements CommandExecutor, TabCompleter {
    private final DeluxeRTPQueue plugin;
    private final MenuManager menuManager;

    public RTPQueueCommand(DeluxeRTPQueue plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("deluxertpqueue.use")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (player.hasPermission("deluxertpqueue.gui")) {
                menuManager.openGUI(player);
            } else {
                player.sendMessage("§cYou don't have permission to open the GUI.");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                plugin.joinQueue(player);

                return true;
            case "leave":
                plugin.leaveQueue(player);
                return true;
            case "gui":
                menuManager.openGUI(player);
                return true;
            default:
              //  player.sendMessage("§cUsage: /rtpqueue [join|leave|gui]");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "leave", "gui");
        }
        return new ArrayList<>();
    }
}
