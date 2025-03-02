package org.JayGamerz;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPReloadCommand implements CommandExecutor {
    private final DeluxeRTPQueue plugin;

    public RTPReloadCommand(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("deluxertpqueue.reload")) {
            sender.sendMessage("§cYou don't have permission to reload the plugin.");
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfigManager().reload(plugin.getConfig ());
        sender.sendMessage("§aPlugin configuration and menu reloaded successfully.");
        return true;
    }
}