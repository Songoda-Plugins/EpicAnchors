package com.songoda.epicanchors.api;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Anchor {
    int getDbId();

    UUID getOwner();

    boolean isLegacy();

    @NotNull Location getLocation();

    @NotNull World getWorld();

    @NotNull Chunk getChunk();

    int getTicksLeft();

    @SuppressWarnings("unused")
    void setTicksLeft(int ticksLeft);

    @SuppressWarnings("UnusedReturnValue")
    int addTicksLeft(int ticks);

    int removeTicksLeft(int ticks);

    boolean isInfinite();
}
