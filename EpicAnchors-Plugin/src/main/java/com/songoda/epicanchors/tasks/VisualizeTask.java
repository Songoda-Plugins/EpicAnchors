package com.songoda.epicanchors.tasks;

import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.epicanchors.EpicAnchors;
import com.songoda.epicanchors.api.Anchor;
import com.songoda.epicanchors.files.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VisualizeTask extends BukkitRunnable {
    private static final int TASK_INTERVAL = 30;

    private final EpicAnchors plugin;

    private final int radius = Bukkit.getServer().getViewDistance();

    public VisualizeTask(EpicAnchors plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        runTaskTimer(this.plugin, TASK_INTERVAL, TASK_INTERVAL);
    }

    @Override
    public void run() {
        HashMap<Chunk, Set<Player>> chunksToVisualize = new HashMap<>();
        Set<Chunk> loadedChunks = new HashSet<>();

        CompatibleParticleHandler.ParticleType particleType = CompatibleParticleHandler.ParticleType.getParticle(Settings.PARTICLE_VISUALIZER.getString());

        for (World world : Bukkit.getWorlds()) {
            if (!this.plugin.getAnchorManager().isReady(world)) continue;

            loadedChunks.clear();
            for (Anchor anchor : this.plugin.getAnchorManager().getAnchors(world)) {
                loadedChunks.add(anchor.getChunk());
            }

            if (!loadedChunks.isEmpty()) {
                for (Player p : world.getPlayers()) {
                    if (!this.plugin.getAnchorManager().hasChunksVisualized(p)) continue;

                    Location pLoc = p.getLocation();

                    // start and stop chunk coordinates
                    int cxi = (pLoc.getBlockX() >> 4) - this.radius;
                    int cxn = cxi + this.radius * 2;
                    int czi = (pLoc.getBlockZ() >> 4) - this.radius;
                    int czn = czi + this.radius * 2;

                    // loop through the chunks to find applicable ones
                    for (int cx = cxi; cx < cxn; ++cx) {
                        for (int cz = czi; cz < czn; ++cz) {
                            Chunk chunk = world.getChunkAt(cx, cz);

                            if (loadedChunks.contains(chunk)) {
                                chunksToVisualize.computeIfAbsent(chunk, key -> new HashSet<>())
                                        .add(p);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<Chunk, Set<Player>> entry : chunksToVisualize.entrySet()) {
            int maxY = entry.getKey().getWorld().getMaxHeight();

            for (Player p : entry.getValue()) {
                int startY = p.getLocation().getBlockY() + 2;

                if (startY <= 0) continue;

                // loop through the chunk
                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        if (Math.random() < .125) { // Don't spawn particles on each block
                            if (startY >= maxY) {
                                startY = maxY - 1;
                            }

                            Block block = entry.getKey().getBlock(x, startY, z);

                            for (int i = 0; i < 12; ++i) {
                                if (block.getType().isSolid()) break;

                                block = block.getRelative(BlockFace.DOWN);
                            }

                            if (!block.isEmpty() && !block.getRelative(BlockFace.UP).getType().isOccluding()) {
                                CompatibleParticleHandler.spawnParticles(particleType,
                                        block.getLocation().add(.5, 1.5, .5),
                                        0, 0, 0, 0, 1, p);
                            }
                        }
                    }
                }
            }
        }
    }
}
