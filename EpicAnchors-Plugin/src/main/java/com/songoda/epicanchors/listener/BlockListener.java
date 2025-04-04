package com.songoda.epicanchors.listener;

import com.songoda.epicanchors.api.AnchorManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class BlockListener implements Listener {
    private final AnchorManager manager;

    public BlockListener(AnchorManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockBurn(BlockBurnEvent e) {
        if (!this.manager.isReady(e.getBlock().getWorld())) return;

        if (this.manager.isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockPiston(BlockPistonExtendEvent e) {
        if (!this.manager.isReady(e.getBlock().getWorld())) return;

        if (this.manager.isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockPiston(BlockPistonRetractEvent e) {
        if (!this.manager.isReady(e.getBlock().getWorld())) return;

        if (this.manager.isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockPhysics(BlockPhysicsEvent e) {
        if (!this.manager.isReady(e.getBlock().getWorld())) return;

        if (this.manager.isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockPiston(LeavesDecayEvent e) {
        if (!this.manager.isReady(e.getBlock().getWorld())) return;

        if (this.manager.isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreation(PortalCreateEvent e) {
        if (!this.manager.isReady(e.getWorld())) return;

        for (Block block : e.getBlocks()) {
            if (this.manager.isAnchor(block)) {
                e.setCancelled(true);
                break;
            }
        }
    }
}
