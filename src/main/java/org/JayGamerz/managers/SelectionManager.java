package org.JayGamerz.managers;

import org.JayGamerz.DeluxeRTPQueue;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, Location> firstPos = new HashMap<> ( );
    private final Map<UUID, Location> secondPos = new HashMap<> ( );
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<> ( );

    public void setFirstPos(Player player, Location loc) {
        firstPos.put (player.getUniqueId ( ), loc);
        updateParticles (player);
    }

    public void setSecondPos(Player player, Location loc) {
        secondPos.put (player.getUniqueId ( ), loc);
        updateParticles (player);
    }

    public Location getFirst(Player player) {
        return firstPos.get (player.getUniqueId ( ));
    }

    public Location getSecond(Player player) {
        return secondPos.get (player.getUniqueId ( ));
    }

    public void updateParticles(Player player) {
        BukkitTask oldTask = particleTasks.get (player.getUniqueId ( ));
        if ( oldTask != null ) oldTask.cancel ( );

        Location pos1 = getFirst (player);
        Location pos2 = getSecond (player);
        if ( pos1 == null || pos2 == null ) return;

        BukkitTask task = Bukkit.getScheduler ( ).runTaskTimer (DeluxeRTPQueue.getPlugin ( ), () -> {
            drawCuboidParticles (player, pos1, pos2);
        }, 0L, 10L);

        particleTasks.put (player.getUniqueId ( ), task);
    }


    public void drawCuboidParticles(Player player, Location pos1, Location pos2) {
        World world = pos1.getWorld ( );
        double minX = Math.min (pos1.getX ( ), pos2.getX ( ));
        double maxX = Math.max (pos1.getX ( ), pos2.getX ( ));
        double minY = Math.min (pos1.getY ( ), pos2.getY ( ));
        double maxY = Math.max (pos1.getY ( ), pos2.getY ( ));
        double minZ = Math.min (pos1.getZ ( ), pos2.getZ ( ));
        double maxZ = Math.max (pos1.getZ ( ), pos2.getZ ( ));

        // Base hue cycles every 10 seconds:
        float baseHue = (System.currentTimeMillis ( ) % 10000L) / 10000f;

        for (double x = minX; x <= maxX; x++) {
            for (double y = minY; y <= maxY; y++) {
                for (double z = minZ; z <= maxZ; z++) {
                    // only on the edges (vertical edges + top/bottom faces)
                    boolean edgeXZ = (x == minX || x == maxX || z == minZ || z == maxZ);
                    boolean edgeY = (y == minY || y == maxY);
                    if ( edgeXZ && edgeY ) {
                        // small offset based on position to spread the rainbow
                        double totalSpan = (maxX - minX) + (maxY - minY) + (maxZ - minZ);
                        double posOffset = ((x - minX) + (y - minY) + (z - minZ)) / totalSpan;
                        float hue = (baseHue + (float) posOffset) % 1f;

                        // convert HSB to RGB
                        int rgb = java.awt.Color.HSBtoRGB (hue, 1f, 1f);
                        Color bukkitColor = Color.fromRGB ((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

                        try {
                            Particle.DustOptions dust = new Particle.DustOptions (bukkitColor, 1.0f);
                            world.spawnParticle (
                                    Particle.REDSTONE,
                                    x + 0.5, y + 0.5, z + 0.5,
                                    1,
                                    0, 0, 0,
                                    dust
                            );
                        } catch (Throwable ignored) { // <-- catch Throwable instead of Exception
                            spawnRedstoneParticle (world, x, y, z);
                        }

                    }
                }
            }
        }
    }

    public void spawnRedstoneParticle(World world, double x, double y, double z) {
        Location loc = new Location (world, x + 0.5, y + 0.5, z + 0.5);
        world.playEffect (loc, Effect.valueOf ("COLOURED_DUST"), 0); // 0 is data, ignored for REDSTONE
    }

    public void clearSelection(Player player) {
        firstPos.remove (player.getUniqueId ( ));
        secondPos.remove (player.getUniqueId ( ));
        BukkitTask task = particleTasks.remove (player.getUniqueId ( ));
        if ( task != null ) task.cancel ( );
    }
}
