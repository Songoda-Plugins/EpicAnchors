package com.songoda.epicanchors.api;

import com.songoda.epicanchors.utils.Callback;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface AnchorManager {
    String NBT_TICKS_KEY = "EpicAnchors_Ticks".toLowerCase();

    boolean isReady(World world);

    Anchor[] getAnchors(@NotNull World world);

    @Nullable Anchor getAnchor(@NotNull Block block);

    boolean isAnchor(@NotNull Block block);

    boolean hasAnchor(@NotNull Chunk chunk);

    List<Anchor> searchAnchors(Location center, double searchRadius);

    List<Anchor> searchAnchors(Location center, double searchRadius, boolean ignoreHeight);

    /**
     * Creates a new anchor at a given location
     *
     * @param loc   The block location for the anchor
     * @param ticks The amount of ticks the anchor lives or -1 for infinite
     */
    void createAnchor(@NotNull Location loc, @NotNull UUID owner, int ticks, @Nullable Callback<Anchor> callback);

    void destroyAnchor(@NotNull Anchor anchor);

    void destroyAnchor(@NotNull Anchor anchor, boolean forceSkipItemDrop);

    void registerAccessCheck(AnchorAccessCheck accessCheck);

    /**
     * @param accessCheck The {@link AnchorAccessCheck} to remove
     * @return true if the {@link AnchorAccessCheck} has been found and removed, false otherwise
     */
    boolean unregisterAccessCheck(AnchorAccessCheck accessCheck);

    /**
     * @deprecated Use {@link #hasAccess(Anchor, UUID)} instead
     */
    @Deprecated
    boolean hasAccess(@NotNull Anchor anchor, @NotNull OfflinePlayer p);

    /**
     * Checks if a player has access to an Anchor. By default, only the owner has access to an Anchor.
     * <br>
     * Other plugins can grant access to other players (e.g. friends).
     * <br>
     * Legacy anchors without an owner automatically grant access to all players.
     *
     * @return true if the player may access the Anchor, false otherwise
     * @see #registerAccessCheck(AnchorAccessCheck)
     */
    boolean hasAccess(@NotNull Anchor anchor, @NotNull UUID uuid);


    ItemStack createAnchorItem(int ticks);

    ItemStack createAnchorItem(int ticks, Material material);

    ItemStack createAnchorItem(int ticks, XMaterial material);

    boolean toggleChunkVisualized(Player p);

    void setChunksVisualized(Player p, boolean visualize);

    boolean hasChunksVisualized(Player p);

    void updateHolograms(List<Anchor> anchors);
}
