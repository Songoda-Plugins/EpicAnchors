package com.songoda.epicanchors;

import com.songoda.epicanchors.api.Anchor;
import com.songoda.epicanchors.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class AnchorImpl implements Anchor {
    private final int dbId;

    private final UUID owner;

    private final Location location;
    private int ticksLeft;

    public AnchorImpl(int dbId, @Nullable UUID owner, @NotNull Location location, int ticksLeft) {
        if (dbId <= 0) throw new IllegalArgumentException("Invalid value for dbId");
        if (ticksLeft <= 0 && ticksLeft != -1) throw new IllegalArgumentException("Invalid value for ticksLeft");

        Objects.requireNonNull(location.getWorld());    // Sanity check

        this.dbId = dbId;

        this.owner = owner;

        this.location = location;
        this.ticksLeft = ticksLeft;
    }

    /**
     * <b></b>This method is automatically synchronized with the server's main thread using
     * {@link org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable)}</b>
     *
     * @see Bukkit#isPrimaryThread()
     * @see org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable)
     */
    protected void init(Plugin plugin) {
        if (Bukkit.isPrimaryThread()) {
            WorldUtils.loadAnchoredChunk(getChunk(), plugin);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> init(plugin));
        }
    }

    /**
     * <b></b>This method is automatically synchronized with the server's main thread using
     * {@link org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable)}</b>
     *
     * @see Bukkit#isPrimaryThread()
     * @see org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable)
     */
    protected void deInit(Plugin plugin) {
        // TODO: Document that holograms are not removed or add boolean flag to remove them

        if (Bukkit.isPrimaryThread()) {
            WorldUtils.unloadAnchoredChunk(getChunk(), plugin);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> deInit(plugin));
        }
    }

    @Override
    public int getDbId() {
        return this.dbId;
    }

    @Override
    public UUID getOwner() {
        return this.owner;
    }

    @Override
    public boolean isLegacy() {
        return this.owner == null;
    }

    @Override
    public @NotNull Location getLocation() {
        return this.location.clone();
    }

    @Override
    public @NotNull World getWorld() {
        return this.location.getWorld();
    }

    @Override
    public @NotNull Chunk getChunk() {
        return this.location.getChunk();
    }

    @Override
    public int getTicksLeft() {
        return this.ticksLeft;
    }

    @Override
    @SuppressWarnings("unused")
    public void setTicksLeft(int ticksLeft) {
        if (ticksLeft < 0) throw new IllegalArgumentException("Invalid value for ticksLeft");

        this.ticksLeft = ticksLeft;
    }

    @Override
    @SuppressWarnings("UnusedReturnValue")
    public int addTicksLeft(int ticks) {
        if (!isInfinite()) {
            this.ticksLeft += ticks;
        }

        return this.ticksLeft;
    }

    @Override
    public int removeTicksLeft(int ticks) {
        if (!isInfinite()) {
            this.ticksLeft -= ticks;

            if (this.ticksLeft < 0) {
                this.ticksLeft = 0;
            }
        }

        return this.ticksLeft;
    }

    @Override
    public boolean isInfinite() {
        return this.ticksLeft == -1;
    }
}
