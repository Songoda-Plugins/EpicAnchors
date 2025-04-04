package com.songoda.epicanchors.files.migration;

import com.songoda.core.configuration.Config;
import com.songoda.core.configuration.ConfigSection;
import com.songoda.epicanchors.files.AnchorsDataManager;
import com.songoda.epicanchors.utils.ThreadSync;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class LegacyYamlAnchorsMigrator {
    public static void migrateLegacyData(Plugin plugin, AnchorsDataManager dataManager) {
        long start = System.nanoTime();

        AtomicBoolean abortMigration = new AtomicBoolean(false);
        int migratedAnchors = 0;

        Config legacyData = new Config(plugin, "data.yml");

        if (legacyData.getFile().exists()) {
            ThreadSync thSync = new ThreadSync();

            legacyData.load();

            ConfigSection cfgSection = legacyData.getConfigurationSection("Anchors");

            if (cfgSection == null) return;

            List<LegacyAnchorEntry> anchorQueue = new ArrayList<>();

            for (String locationStr : cfgSection.getKeys(false)) {
                int ticksLeft = cfgSection.getInt(locationStr + ".ticksLeft");
                String[] locArgs = deserializeLegacyLocation(locationStr);

                if (ticksLeft == -99) {
                    ticksLeft = -1;
                }

                if (locArgs.length == 0) {
                    abortMigration.set(true);

                    plugin.getLogger().warning(() -> "Error migrating anchor '" + locationStr + "': invalid format - expected 'worldName:X:Y:Z'");
                    break;
                }

                String worldName = locArgs[0];
                int x = Location.locToBlock(Double.parseDouble(locArgs[1]));
                int y = Location.locToBlock(Double.parseDouble(locArgs[2]));
                int z = Location.locToBlock(Double.parseDouble(locArgs[3]));

                int finalTicksLeft = ticksLeft;
                dataManager.exists(worldName, x, y, z, (ex, anchorExists) -> {
                    if (ex == null) {
                        if (anchorExists) {
                            cfgSection.set(locationStr, null);
                        } else {
                            anchorQueue.add(new LegacyAnchorEntry(worldName, x, y, z, finalTicksLeft));
                        }
                    } else {
                        abortMigration.set(true);

                        plugin.getLogger().log(Level.SEVERE, ex, () -> "Error migrating Anchor '" + locationStr + "' from '" +
                                legacyData.getFile().getName() + "'");
                    }

                    thSync.release();
                });

                thSync.waitForRelease();
                thSync.reset();

                if (abortMigration.get()) break;

                ++migratedAnchors;
            }

            if (!abortMigration.get()) {
                int finalMigratedAnchors = migratedAnchors;
                dataManager.migrateAnchor(anchorQueue, ex -> {
                    long end = System.nanoTime();

                    if (ex == null) {
                        try {
                            Files.deleteIfExists(legacyData.getFile().toPath());
                        } catch (IOException err) {
                            plugin.getLogger().warning("Could not delete '" + legacyData.getFile().getName() + "' after data migration: " + err.getMessage());
                        }

                        plugin.getLogger().info("Successfully migrated " + finalMigratedAnchors + " Anchors from '" +
                                legacyData.getFile().getName() + "' (" + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms)");
                    } else {
                        legacyData.save();
                    }
                });
            }
        }
    }

    private static String[] deserializeLegacyLocation(String str) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        }

        str = str
                .replace("w:", "")
                .replace("x:", ":")
                .replace("y:", ":")
                .replace("z:", ":")
                .replace("/", ".");

        String[] args = str.split(":");

        return args.length == 4 ? args : new String[0];
    }

    public static class LegacyAnchorEntry {
        public final String worldName;
        public final int x;
        public final int y;
        public final int z;

        public final int ticksLeft;

        public LegacyAnchorEntry(String worldName, int x, int y, int z, int ticksLeft) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticksLeft = ticksLeft;
        }
    }
}
