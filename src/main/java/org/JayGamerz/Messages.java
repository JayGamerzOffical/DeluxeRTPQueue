package org.JayGamerz;

import org.bukkit.configuration.ConfigurationSection;

public class Messages {
    private final String prefix;
    private final String joinQueue;
    private final String alrQueue;
    private final String leaveQueue;
    private final String configReloaded;
    private final String teleportSuccess;
    private final String actionBarJoin;
    private final String actionBarLeave;
    private final String actionBarTeleport;
    private final String queueFull;
    private final String teleportCountdown;
    private final String queueAnnouncement;

    public Messages(ConfigurationSection section) {
        prefix = section.getString("prefix", "&e[RTP Queue] &7 >>  ");
        joinQueue = section.getString("join_queue", "&aJoined queue!");
        alrQueue = section.getString("already_queue", "&aYou are already in the queue!");
        leaveQueue = section.getString("leave_queue", "&cLeft queue!");
        configReloaded = section.getString("config_reloaded", "&eConfig reloaded!");
        teleportSuccess = section.getString("teleport_success", "&6Teleported!");
        actionBarJoin = section.getString("action_bar_join_message", "&aIn Queue");
        actionBarLeave = section.getString("action_bar_leave_message", "&cLeft Queue");
        actionBarTeleport = section.getString("action_bar_teleport_message", "&6Teleported!");
        queueFull = section.getString("queue_full", "&cQueue is full!");
        teleportCountdown = section.getString("teleport_countdown", "&eTeleporting in %time% seconds");
        queueAnnouncement = section.getString("queue_announcement", "&e%player% is waiting for a partner to RTP! Click to join!");
    }

    public String getPrefix() {
        return prefix;
    }

    public String getJoinQueue() {
        return prefix+joinQueue;
    }

    public String getAlrQueue() {
        return alrQueue;
    }

    public String getLeaveQueue() {
        return leaveQueue;
    }

    public String getConfigReloaded() {
        return configReloaded;
    }

    public String getTeleportSuccess() {
        return teleportSuccess;
    }

    public String getActionBarJoin() {
        return prefix + actionBarJoin;
    }

    public String getActionBarLeave() {
        return prefix+ actionBarLeave;
    }

    public String getActionBarTeleport() {
        return actionBarTeleport;
    }

    public String getQueueFull() {
        return prefix + queueFull;
    }

    public String getTeleportCountdown() {
        return prefix+ teleportCountdown;
    }

    public String getQueueAnnouncement() {
        return prefix+ queueAnnouncement;
    }
}