package com.songoda.epicanchors.listener;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.function.Consumer;

public class WorldListener implements Listener {
    private final Consumer<World> initAnchorsInWorld;
    private final Consumer<World> deInitAnchorsInWorld;

    public WorldListener(Consumer<World> initAnchorsInWorld, Consumer<World> deInitAnchorsInWorld) {
        this.initAnchorsInWorld = initAnchorsInWorld;
        this.deInitAnchorsInWorld = deInitAnchorsInWorld;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldLoad(WorldLoadEvent e) {
        this.initAnchorsInWorld.accept(e.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldUnload(WorldUnloadEvent e) {
        this.deInitAnchorsInWorld.accept(e.getWorld());
    }
}
