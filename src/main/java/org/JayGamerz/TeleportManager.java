package org.JayGamerz;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class TeleportManager {
    private final DeluxeRTPQueue plugin;
    private final Random random;
    private final List<Material> allowedGroundBlocks;
    private final int maxAttempts;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    // Executor service to offload heavy computations.
    private final ExecutorService executorService;

    public TeleportManager(DeluxeRTPQueue plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.allowedGroundBlocks = plugin.getConfigManager().getSafeMaterials() != null
                ? plugin.getConfigManager().getSafeMaterials()
                : new ArrayList<>();
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        this.minX = plugin.getConfigManager().getMinX();
        this.maxX = plugin.getConfigManager().getMaxX();
        this.minZ = plugin.getConfigManager().getMinZ();
        this.maxZ = plugin.getConfigManager().getMaxZ();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // Returns true if the material is one of our safe air types.
    private boolean isSafe(Material material) {
        try {
            return material == Material.valueOf("CAVE_AIR") || material == Material.valueOf("VOID_AIR");
        } catch (Exception e) {
            return false;
        }
    }

    // Returns true if the material is AIR or one of our safe air types.
    private boolean isAir(Material material) {
        return material == Material.AIR || isSafe(material);
    }

    /**
     * Asynchronously finds valid teleport locations.
     * The result (an array of two Location objects) is passed to the given callback.
     * This method offloads heavy computation to another thread while scheduling safe world queries
     * on the main server thread.
     *
     * @param world    the world in which to search for valid locations
     * @param callback a Consumer that receives the result (or null if no valid locations were found)
     */
    public void findValidLocationsAsync(World world, Consumer<Location[]> callback) {
        executorService.submit(() -> {
            Location[] result = null;
            int xRange = maxX - minX;
            int zRange = maxZ - minZ;
            if (xRange <= 0 || zRange <= 0) {
                plugin.getLogger().warning("Invalid teleport boundaries. Check min/max values.");
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                return;
            }

            for (int i = 0; i < maxAttempts; i++) {
                int x1 = minX + random.nextInt(xRange + 1);
                int z1 = minZ + random.nextInt(zRange + 1);

                Location loc1 = getValidSurfaceSync(world, x1, z1);
                if (loc1 == null) continue;

                List<int[]> deltas = new ArrayList<>(plugin.deltas != null ? plugin.deltas : Arrays.asList(
                        new int[]{0, 1}, new int[]{1, 0}, new int[]{0, -1}, new int[]{-1, 0}
                ));
                Collections.shuffle(deltas);

                for (int[] delta : deltas) {
                    int x2 = x1 + delta[0];
                    int z2 = z1 + delta[1];
                    if (x2 < minX || x2 > maxX || z2 < minZ || z2 > maxZ) {
                        continue;
                    }

                    Location loc2 = getValidSurfaceSync(world, x2, z2);
                    if (loc2 != null && isValidDistance(loc1, loc2)) {
                        result = new Location[]{
                                loc1.clone().add(0.5, 0, 0.5),
                                loc2.clone().add(0.5, 0, 0.5)
                        };
                        break;
                    }
                }
                if (result != null) break;
            }
            // Pass result back on the main thread.
            Location[] finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }

    /**
     * Synchronously finds valid teleport locations.
     * <strong>Note:</strong> This method must NOT be called from the main thread
     * because it blocks waiting for scheduled tasks to complete.
     *
     * @param world the world in which to search for valid locations
     * @return an array of two Location objects if found, or null otherwise
     * @throws IllegalStateException if called on the main thread.
     */
    public Location[] findValidLocations(World world) {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("findValidLocations() cannot be called from the main thread. Use findValidLocationsAsync() instead.");
        }
        CompletableFuture<Location[]> future = new CompletableFuture<>();
        findValidLocationsAsync(world, future::complete);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error in finding valid locations: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a valid surface location at the specified x and z coordinates.
     * Because world interactions must run on the main thread, this schedules a task and waits for the result.
     *
     * @param world the world to check
     * @param x     the x-coordinate
     * @param z     the z-coordinate
     * @return a valid Location if found, or null otherwise
     */
    private Location getValidSurfaceSync(World world, int x, int z) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        // Schedule world queries on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            int y = world.getHighestBlockYAt(x, z);
            Block surfaceBlock = world.getBlockAt(x, y, z);
            Block aboveBlock = world.getBlockAt(x, y + 1, z);
            Block aboveAboveBlock = world.getBlockAt(x, y + 2, z);

            if (isSafeBlock(surfaceBlock) && isAir(aboveBlock.getType()) && isAir(aboveAboveBlock.getType())) {
                if (hasSufficientAdjacentSolidBlocks(world, x, y, z)) {
                    future.complete(new Location(world, x, y + 1, z));
                    return;
                }
            }
            future.complete(null);
        });
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error retrieving valid surface: " + e.getMessage());
            return null;
        }
    }

    // Returns true if the block is of an allowed type and not a dangerous block.
    private boolean isSafeBlock(Block block) {
        Material type = block.getType();
        return allowedGroundBlocks.contains(type)
                && type != Material.LAVA
                && type != Material.WATER
                && type != Material.FIRE;
    }

    // Checks that the distance between two locations is within the desired range.
    private boolean isValidDistance(Location loc1, Location loc2) {
        double distance = loc1.distance(loc2);
        return distance >= 3 && distance <= 4;
    }

    // Checks that at least two adjacent blocks (north, south, east, or west) are solid.
    private boolean hasSufficientAdjacentSolidBlocks(World world, int x, int y, int z) {
        int adjacentSolidCount = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            Block adjacent = world.getBlockAt(x + dir[0], y, z + dir[1]);
            if (adjacent.getType().isSolid()) {
                adjacentSolidCount++;
            }
        }
        return adjacentSolidCount >= 2;
    }

    // Call this method on plugin disable to shut down the executor service.
    public void shutdown() {
        executorService.shutdown();
    }
}
