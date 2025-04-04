package com.songoda.epicanchors.tasks;

import com.songoda.core.nms.Nms;
import com.songoda.core.nms.UnsupportedServerVersionException;
import com.songoda.epicanchors.EpicAnchors;
import com.songoda.epicanchors.api.Anchor;
import com.songoda.epicanchors.api.AnchorManager;
import com.songoda.epicanchors.utils.Utils;
import com.songoda.epicanchors.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * More information about what types of game ticks there are and what does what: https://minecraft.fandom.com/wiki/Tick
 */
public class AnchorTask extends BukkitRunnable {
    private static final int TASK_INTERVAL = 3;

    private final EpicAnchors plugin;
    private final AnchorManager anchorManager;

    private boolean randomTicksFailed;
    private boolean spawnerTicksFailed;

    public AnchorTask(EpicAnchors plugin) {
        this.plugin = plugin;
        this.anchorManager = plugin.getAnchorManager();
    }

    public void startTask() {
        runTaskTimer(this.plugin, TASK_INTERVAL, TASK_INTERVAL);
    }

    @Override
    public void run() {
        try {
            for (World world : Bukkit.getWorlds()) {
                if (!this.anchorManager.isReady(world)) return;

                int randomTicksToDo = WorldUtils.getRandomTickSpeed(world) * TASK_INTERVAL;

                Set<Chunk> alreadyTicked = new HashSet<>();
                Anchor[] anchorsInWorld = this.anchorManager.getAnchors(world);
                List<Anchor> toUpdateHolo = new ArrayList<>(anchorsInWorld.length);

                List<Chunk> chunksWithPlayers = getChunksWithPlayers(world);
                for (Anchor anchor : anchorsInWorld) {
                    Chunk chunk = anchor.getChunk();

                    if (alreadyTicked.add(chunk)) {
                        // Having a chunk loaded takes care of entities and weather (https://minecraft.fandom.com/wiki/Tick#Chunk_tick)
                        loadChunk(chunk);

                        executeTicksForInactiveSpawners(chunk);

                        if (!chunksWithPlayers.contains(chunk)) {
                            executeRandomTick(chunk, randomTicksToDo);
                        }
                    }

                    if (updateAnchorTimeLeftAndCheckIfHologramsNeedsUpdate(anchor)) {
                        toUpdateHolo.add(anchor);
                    }
                }

                this.anchorManager.updateHolograms(toUpdateHolo);
            }
        } catch (Exception ex) {
            Utils.logException(this.plugin, ex);
        }
    }

    private List<Chunk> getChunksWithPlayers(World world) {
        List<Player> playersInWorld = world.getPlayers();

        List<Chunk> chunksWithPlayers = new ArrayList<>(playersInWorld.size());
        for (Player p : playersInWorld) {
            chunksWithPlayers.add(p.getLocation().getChunk());
        }
        return chunksWithPlayers;
    }

    private void loadChunk(Chunk chunk) {
        if (chunk.isLoaded()) {
            // Loading an already loaded chunk still fires the ChunkLoadEvent and might have a huge
            // impact on performance if other plugins do not expect that either...
            return;
        }

        WorldUtils.loadAnchoredChunk(chunk, this.plugin);
    }

    private void executeTicksForInactiveSpawners(Chunk chunk) {
        if (this.spawnerTicksFailed) {
            return;
        }

        try {
            Nms.getImplementations().getWorld().tickInactiveSpawners(chunk, TASK_INTERVAL);
        } catch (UnsupportedServerVersionException | ReflectiveOperationException | IncompatibleClassChangeError ex) {
            this.plugin.getLogger().log(Level.SEVERE, ex,
                    () -> "Failed to do spawner ticks on this server implementation(/version) - " +
                            "Skipping further spawner ticks.");

            this.spawnerTicksFailed = true;
        }
    }

    private void executeRandomTick(Chunk chunk, int randomTicks) {
        if (this.randomTicksFailed) {
            return;
        }

        try {
            Nms.getImplementations().getWorld().randomTickChunk(chunk, randomTicks);
        } catch (UnsupportedServerVersionException | ReflectiveOperationException | IncompatibleClassChangeError ex) {
            this.plugin.getLogger().log(Level.SEVERE, ex,
                    () -> "Failed to do random ticks on this server implementation(/version) - " +
                            "Skipping further random ticks.");

            this.randomTicksFailed = true;
        }
    }

    private boolean updateAnchorTimeLeftAndCheckIfHologramsNeedsUpdate(Anchor anchor) {
        // TODO: Only update hologram if a player is nearby
        //       Simplify player location to chunks to potentially group players
        //       Use the server view distance to calculate minimum distance to count as not-nearby

        if (!anchor.isInfinite()) {
            int ticksLeft = anchor.removeTicksLeft(TASK_INTERVAL);

            if (ticksLeft == 0) {
                this.anchorManager.destroyAnchor(anchor);
            } else {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }
}
