package org.JayGamerz.commands;

import org.JayGamerz.DeluxeRTPQueue;
import org.JayGamerz.listeners.ShearsRegionSelectorListener;
import org.JayGamerz.managers.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;

public class RTPQueueCommand implements CommandExecutor, TabExecutor {
    private final DeluxeRTPQueue plugin;
    private final MenuManager menuManager;

    public RTPQueueCommand(DeluxeRTPQueue plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.players_only"));
            return true;
        }
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        String worldName = player.getWorld().getName(); // Default to current world


        plugin.worldQueues.putIfAbsent(worldName, new HashMap<>());
        Map<UUID, Long> queue = plugin.worldQueues.get(worldName);

        if (args.length == 0) {
            if (queue.containsKey(playerId)) {
                queue.remove(playerId);
                player.sendMessage(plugin.getMessage("messages.leave_queue"));
                plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_leave_message"));
            } else {
                queue.put(playerId, System.currentTimeMillis());
                player.sendMessage(plugin.getMessage("messages.join_queue"));
                plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_join_message"));
                plugin.checkQueue(worldName);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("join") && sender.hasPermission("deluxertpqueue.join")) {
            // Check if a specific world is given
            if (args.length >= 2) {
                String inputWorld = args[1];
                List<?> enabledWorlds = plugin.getConfig().getList("rtp_enabled_worlds");

                if (enabledWorlds == null || !enabledWorlds.contains(inputWorld)) {
                    sender.sendMessage(plugin.getMessage("messages.world_not_enabled", inputWorld));
                    return true;
                } else if (Bukkit.getWorld(inputWorld) != null) {
                    worldName = inputWorld;
                } else {
                    sender.sendMessage(plugin.getMessage("messages.world_not_found", inputWorld));
                    return true;
                }
            }
            if (queue.containsKey(playerId)) {
                player.sendMessage(plugin.getMessage("messages.already_queue"));
                return true;
            }
            queue.put(playerId, System.currentTimeMillis());
            player.sendMessage(plugin.getMessage("messages.join_queue"));
            plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_join_message"));
            plugin.checkQueue(worldName);
            return true;
        } else if (args[0].equalsIgnoreCase("leave") && sender.hasPermission("deluxertpqueue.leave")) {
            if (!queue.containsKey(playerId)) {
                player.sendMessage(plugin.getMessage("messages.not_in_queue"));
                return true;
            }
            queue.remove(playerId);
            player.sendMessage(plugin.getMessage("messages.leave_queue"));
            plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_leave_message"));
            return true;
        } else if (args[0].equals("gui") && sender.hasPermission("deluxertpqueue.gui")) {
            menuManager.openGUI(player);
            player.sendMessage(plugin.getMessage("messages.gui_open"));
        } else if ((args[0].equalsIgnoreCase("admin") || args[0].equalsIgnoreCase("a")) && sender.hasPermission("deluxertpqueue.admin")) {
            if (args.length < 2) {
                sender.sendMessage(plugin.getMessage("messages.usage_admin"));
                return true;
            }

            String subCommand = args[1].toLowerCase();

            if (subCommand.equals("area")) {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessage("messages.queuearea_add_usage"));
                    return true;
                }

                String areaAction = args[2].toLowerCase();

                if (areaAction.equals("create")) {
                    if (args.length < 5) {
                        sender.sendMessage(plugin.getMessage("messages.queuearea_add_usage"));
                        return true;
                    }

                    String areaName = args[3];
                    int time = parseTimeToSeconds(args[4]);

                    if (plugin.getAreaManager().addAreaFromSelection(player, areaName, plugin.getSelectionManager(), time)) {
                        player.sendMessage(plugin.getMessage("messages.queuearea_set", areaName));
                    } else {
                        player.sendMessage(plugin.getMessage("messages.first_pos_select"));
                    }
                } else if (areaAction.equals("remove")) {
                    if (args.length < 4) {
                        player.sendMessage(plugin.getMessage("messages.queuearea_remove_usage"));
                        return true;
                    }

                    String areaName = args[3];
                    if (plugin.getAreaManager().getArea(areaName) == null) {
                        player.sendMessage(plugin.getMessage("messages.area_not_found", areaName));
                        return true;
                    }

                    plugin.getAreaManager().removeArea(areaName);
                    player.sendMessage(plugin.getMessage("messages.area_removed", areaName));
                } else if (areaAction.equals("teleport")) {
                    if (args.length < 4) {
                        player.sendMessage(plugin.getMessage("messages.queuearea_teleport_usage"));
                        return true;
                    }

                    String areaName = args[3];
                    if (plugin.getAreaManager().getArea(areaName) == null) {
                        player.sendMessage(plugin.getMessage("messages.area_not_found", areaName));
                        return true;
                    }

                    plugin.getAreaManager().teleportToArea(player, areaName);
                } else {
                    player.sendMessage(plugin.getMessage("messages.queuearea_add_usage"));
                }
            } else if (subCommand.equals("reload")) {
                plugin.reloadConfig();
                plugin.getMenuManager().reload();
                plugin.loadMessages();
                ShearsRegionSelectorListener.reloadSelectionMaterial();
                plugin.loadFields();
                player.sendMessage(plugin.getMessage("messages.config_reloaded"));
            } else if (subCommand.equals("pos1")) {
                plugin.getSelectionManager().setFirstPos(player, player.getLocation().getBlock().getLocation());
                player.sendMessage(plugin.getMessage("messages.first_pos_selected"));
            } else if (subCommand.equals("pos2")) {
                plugin.getSelectionManager().setSecondPos(player, player.getLocation().getBlock().getLocation());
                player.sendMessage(plugin.getMessage("messages.second_pos_selected"));
            } else if (subCommand.equals("clearselection")) {
                plugin.getSelectionManager().clearSelection(player);
                player.sendMessage(plugin.getMessage("messages.selection_cleared"));
            } else if (subCommand.equals("gui")) {
                //"&cUsage: /rtpqueue admin gui <name>"
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessage("messages.gui_usage"));
                    return true;
                }
                String subsubCommand = args[2].toLowerCase();
                Player player1 = Bukkit.getPlayer(subsubCommand);
                if (player1 == null) {
                    player.sendMessage(plugin.getMessage("messages.player_not_found", subsubCommand));
                    return true;
                } else if (!player1.isOnline()) {
                    player.sendMessage(plugin.getMessage("messages.player_not_online", subsubCommand));
                    return true;
                } else {
                    menuManager.openGUI(player1);
                    player1.sendMessage(plugin.getMessage("messages.gui_open"));
                    player.sendMessage(plugin.getMessage("messages.gui_opened_to_player"));
                }
            } else if (subCommand.equals("add")) {
                // /rtp admin add <player> <world>
                if (args.length < 4) {
                    player.sendMessage(plugin.getMessage("messages.add_player_usage"));
                    return true;
                }
                String subsubCommand = args[2].toLowerCase();
                Player player1 = Bukkit.getPlayer(subsubCommand);

                if (player1 == null) {
                    player.sendMessage(plugin.getMessage("messages.player_not_found", subsubCommand));
                    return true;
                } else if (!player1.isOnline()) {
                    player.sendMessage(plugin.getMessage("messages.player_not_online", subsubCommand));
                    return true;
                } else {
                    String inputWorld = args[3];
                    if (queue.containsKey(player1.getUniqueId())) {
                        player.sendMessage(plugin.getMessage("messages.player_already_queue", player1.getDisplayName()));
                        return true;
                    }
                    List<?> enabledWorlds = plugin.getConfig().getList("rtp_enabled_worlds");

                    if (enabledWorlds == null || !enabledWorlds.contains(inputWorld)) {
                        sender.sendMessage(plugin.getMessage("messages.world_not_enabled").replace("%world%", inputWorld));
                        return true;
                    } else if (Bukkit.getWorld(inputWorld) != null) {
                        worldName = inputWorld;
                    } else {
                        sender.sendMessage(plugin.getMessage("messages.world_not_found").replace("%world%", inputWorld));
                        return true;

                    }

                    if (queue.containsKey(playerId)) {
                        player.sendMessage(plugin.getMessage("messages.already_queue"));
                        return true;
                    }
                    queue.put(playerId, System.currentTimeMillis());
                    player.sendMessage(plugin.getMessage("messages.join_queue"));
                    plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_join_message"));
                    plugin.checkQueue(worldName);
                    player.sendMessage(plugin.getMessage("messages.player_added_to_queue", player.getDisplayName()));
                    return true;
                }
            } else if (subCommand.equals("remove")) {
                String subsubCommand = args[2].toLowerCase();
                Player player1 = Bukkit.getPlayer(subsubCommand);
                UUID playerId1;
                if (player1 == null) {
                    player.sendMessage(plugin.getMessage("messages.player_not_found", subsubCommand));
                    return true;
                } else if (!player1.isOnline()) {
                    player.sendMessage(plugin.getMessage("messages.player_not_online", subsubCommand));
                    return true;
                }
                playerId1 = player1.getUniqueId();
                if (!queue.containsKey(playerId1)) {
                    player.sendMessage(plugin.getMessage("messages.not_in_queue"));
                    return true;
                }
                queue.remove(playerId1);
                player.sendMessage(plugin.getMessage("messages.leave_queue"));
                plugin.sendActionBar(player, plugin.getMessage("messages.action_bar_leave_message"));
                player.sendMessage(plugin.getMessage("messages.player_removed_from_queue", player.getDisplayName()));
                return true;
            } else {
                player.sendMessage(plugin.getMessage("messages.usage_admin"));
            }
            return true;
        }

        return true;
    }

    public int parseTimeToSeconds(String input) {
        input = input.trim().toLowerCase();

        // If it's just a number, return it as seconds
        if (input.matches("\\d+")) {
            return Integer.parseInt(input);
        }

        int totalSeconds = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?")
                .matcher(input);

        if (matcher.matches()) {
            String hours = matcher.group(1);
            String minutes = matcher.group(2);
            String seconds = matcher.group(3);

            if (hours != null) totalSeconds += Integer.parseInt(hours) * 3600;
            if (minutes != null) totalSeconds += Integer.parseInt(minutes) * 60;
            if (seconds != null) totalSeconds += Integer.parseInt(seconds);
        }

        return totalSeconds;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return suggestions; // No tab completion for non-players
        }
        Player player = (Player) sender;

        // If no arguments, suggest the main subcommands
        if (args.length == 1) {
            List<String> mainCommands = Arrays.asList("join", "leave", "gui", "admin");
            for (String command : mainCommands) {
                if (command.startsWith(args[0].toLowerCase())) {
                    suggestions.add(command);
                }
            }
        }

        // If "admin" or "a" subcommand is used
        else if (args.length == 2 && (args[0].equalsIgnoreCase("admin") || args[0].equalsIgnoreCase("a"))) {
            List<String> adminSubcommands = Arrays.asList("area", "reload", "pos1", "pos2", "clearselection", "gui", "add", "remove");
            for (String subCommand : adminSubcommands) {
                if (subCommand.startsWith(args[1].toLowerCase())) {
                    suggestions.add(subCommand);
                }
            }
        }

        // If "admin area" subcommand is used, suggest "create", "remove", "teleport"
        else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("area")) {
            List<String> areaActions = Arrays.asList("create", "remove", "teleport");
            for (String action : areaActions) {
                if (action.startsWith(args[2].toLowerCase())) {
                    suggestions.add(action);
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("area") && !args[2].equalsIgnoreCase("add")) {
            Set<String> areaActions = plugin.getAreaManager().getAllAreaNames();
            for (String action : areaActions) {
                if (action.startsWith(args[3].toLowerCase())) {
                    suggestions.add(action);
                }
            }
        }

        // If "admin area create" or "admin area remove" is used, suggest area names
        else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("area") &&
                (args[2].equalsIgnoreCase("create") || args[2].equalsIgnoreCase("remove"))) {
            Set<String> areaNames = plugin.getAreaManager().getAllAreaNames(); // Get list of area names from your manager
            for (String areaName : areaNames) {
                if (areaName.toLowerCase().startsWith(args[3].toLowerCase())) {
                    suggestions.add(areaName);
                }
            }
        }

        // If "admin gui" or "admin add/remove" is used, suggest online player names
        else if (args.length == 3 && (args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("gui") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")))) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    suggestions.add(onlinePlayer.getName());
                }
            }
        }

        // If "admin add" or "admin remove" is used and an area is specified, suggest world names
        else if (args.length == 4 && (args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")))) {
            List<?> enabledWorlds = plugin.getConfig().getList("rtp_enabled_worlds");
            for (Object world : enabledWorlds) {
                if (world instanceof String) {
                    if (((String) world).toLowerCase().startsWith(args[3].toLowerCase())) {
                        suggestions.add((String) world);
                    }
                }
            }
        }

        return suggestions;
    }


}
