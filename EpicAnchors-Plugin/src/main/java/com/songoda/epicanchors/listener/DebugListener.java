package com.songoda.epicanchors.listener;

import com.songoda.epicanchors.EpicAnchors;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DebugListener implements Listener {
    private final EpicAnchors plugin;
    private final Logger logger;

    public DebugListener(EpicAnchors plugin) {
        this.plugin = plugin;

        this.logger = Logger.getLogger(plugin.getName() + "-DEBUG");
    }

    private void logDebug(String s) {
        LogRecord logRecord = new LogRecord(Level.INFO, s);
        logRecord.setMessage("[" + this.logger.getName() + "] " + logRecord.getMessage());

        this.logger.log(logRecord);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockPhysics(BlockPhysicsEvent e) {
        if (skipEvent(e.getBlock().getChunk())) return;

        logDebug("BlockPhysicsEvent (" + e.getBlock().getType() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockForm(BlockFormEvent e) {
        if (skipEvent(e.getBlock().getChunk())) return;

        logDebug("BlockFormEvent (" + e.getBlock().getType() + " -> " + e.getNewState().getType() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockFromTo(BlockFromToEvent e) {
        if (skipEvent(e.getBlock().getChunk())) return;

        logDebug("BlockFromToEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onFurnace(FurnaceSmeltEvent e) {
        if (skipEvent(e.getBlock().getChunk())) return;

        logDebug("FurnaceSmeltEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onSneak(PlayerToggleSneakEvent e) {
        Chunk chunk = e.getPlayer().getLocation().getChunk();

        if (e.getPlayer().isFlying() || skipEvent(chunk)) return;

        Map<String, Integer> count = new HashMap<>();

        if (e.isSneaking()) {
            e.getPlayer().sendMessage("§e§lEntities");
            for (Entity entity : chunk.getEntities()) {
                count.compute(entity.getType().name(), (key, value) -> value == null ? 1 : value + 1);
            }
        } else {
            e.getPlayer().sendMessage("§e§lTileEntities");
            for (BlockState blockState : chunk.getTileEntities()) {
                count.compute(blockState.getType().name(), (key, value) -> value == null ? 1 : value + 1);
            }
        }

        Map<String, Integer> sortedEntitiyCount = count.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (Map.Entry<String, Integer> entry : sortedEntitiyCount.entrySet()) {
            String entityName = WordUtils.capitalize(entry.getKey().toLowerCase(), new char[]{'_'}).replace("_", "");

            e.getPlayer().sendMessage("§a" + entityName + "§7:§r " + entry.getValue());
        }

        e.getPlayer().sendMessage("");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onSpawner(SpawnerSpawnEvent e) {
        if (skipEvent(e.getSpawner().getBlock().getChunk())) return;

        logDebug("SpawnerSpawnEvent (" + e.getEntity().getType().name() + ")");
    }

    @EventHandler
    private void onCreatureSpawn(CreatureSpawnEvent e) {
        if (skipEvent(e.getLocation().getChunk())) return;

        logDebug("CreatureSpawnEvent (" + e.getEntity().getType().name() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockGrow(BlockGrowEvent e) {
        if (skipEvent(e.getBlock().getChunk())) return;

        logDebug("BlockGrowEvent (" + e.getBlock().getType() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onInvItemMove(InventoryMoveItemEvent e) {
        if (e.getSource().getHolder() == null) return;

        Location loc = null;

        try {
            loc = (Location) e.getSource().getHolder().getClass().getDeclaredMethod("getLocation").invoke(e.getSource().getHolder());
        } catch (Exception ex) {
            try {
                loc = (Location) e.getSource().getClass().getDeclaredMethod("getLocation").invoke(e.getSource());
            } catch (Exception ex2) {
                logDebug("InventoryMoveItemEvent (Potentially in a chunk without Anchor [Not supported at current server version])");
            }
        }

        if (loc == null || skipEvent(loc.getChunk())) return;

        logDebug("InventoryMoveItemEvent (" + e.getSource().getType() + " -> " + e.getDestination().getType() + ")");
    }

    private boolean skipEvent(Chunk chunk) {
        return !this.plugin.getAnchorManager().isReady(chunk.getWorld()) || !this.plugin.getAnchorManager().hasAnchor(chunk);
    }
}
