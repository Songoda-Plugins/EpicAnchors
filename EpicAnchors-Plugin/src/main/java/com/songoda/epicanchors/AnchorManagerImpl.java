package com.songoda.epicanchors;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.hooks.HologramManager;
import com.songoda.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.songoda.core.utils.TextUtils;
import com.songoda.core.utils.TimeUtils;
import com.songoda.epicanchors.api.Anchor;
import com.songoda.epicanchors.api.AnchorAccessCheck;
import com.songoda.epicanchors.api.AnchorManager;
import com.songoda.epicanchors.files.AnchorsDataManager;
import com.songoda.epicanchors.files.Settings;
import com.songoda.epicanchors.utils.Callback;
import com.songoda.epicanchors.utils.UpdateCallback;
import com.songoda.epicanchors.utils.Utils;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AnchorManagerImpl implements AnchorManager {
    private static final String ERR_WORLD_NOT_READY = "EpicAnchors has not finished initializing that world yet";
    private static final String HOLOGRAM_PREFIX = "Anchor_";

    private final SongodaPlugin plugin;
    private final AnchorsDataManager dataManager;

    private final Map<World, Set<Anchor>> anchors = new HashMap<>(3);
    private final Set<Player> visualizedChunk = new HashSet<>();
    private final List<AnchorAccessCheck> accessChecks = new LinkedList<>();

    private boolean ready;

    public AnchorManagerImpl(SongodaPlugin plugin, AnchorsDataManager dataManager) {
        this.plugin = Objects.requireNonNull(plugin);
        this.dataManager = Objects.requireNonNull(dataManager);
    }

    protected void saveAll() {
        for (Set<Anchor> anchorSet : this.anchors.values()) {
            this.dataManager.updateAnchors(anchorSet, null);
        }
    }

    protected void deInitAll() {
        for (World world : this.anchors.keySet().toArray(new World[0])) {
            deInitAnchors(world);
        }
    }

    protected void initAnchorsAsync(@NotNull World world, @Nullable UpdateCallback callback) {
        if (this.anchors.containsKey(world)) {
            if (callback != null) {
                callback.accept(null);
            }

            return;
        }

        long start = System.nanoTime();

        this.dataManager.getAnchors(world, (ex, result) -> {
            if (ex == null) {
                this.anchors.computeIfAbsent(world, key -> new HashSet<>());

                for (Anchor anchor : result) {
                    ((AnchorImpl) anchor).init(this.plugin);

                    this.anchors.computeIfAbsent(anchor.getWorld(), key -> new HashSet<>())
                            .add(anchor);
                }

                long end = System.nanoTime();
                this.plugin.getLogger().info("Initialized " + this.anchors.get(world).size() + " anchors in world '" + world.getName() + "' " +
                        "(" + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms)");

                if (callback != null) {
                    callback.accept(null);
                }
            } else {
                if (callback != null) {
                    callback.accept(ex);
                } else {
                    Utils.logException(this.plugin, ex, "H2");
                }
            }
        });
    }

    protected void deInitAnchors(@NotNull World world) {
        Set<Anchor> tmpAnchors = this.anchors.remove(world);

        if (tmpAnchors != null) {
            this.dataManager.updateAnchors(tmpAnchors, null);

            for (Anchor anchor : tmpAnchors) {
                ((AnchorImpl) anchor).deInit(this.plugin);
            }
        }
    }

    protected void setReady() {
        this.ready = true;

        Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveAll, 20L * 60 * 5, 20L * 60 * 5);
    }

    @Override
    public boolean isReady(World world) {
        return this.ready && this.anchors.containsKey(world);
    }

    /* Getter */

    @Override
    public Anchor[] getAnchors(@NotNull World world) {
        Set<Anchor> set = this.anchors.get(world);

        if (set != null) {
            return set.toArray(new Anchor[0]);
        }

        return new Anchor[0];
    }

    @Override
    public @Nullable Anchor getAnchor(@NotNull Block block) {
        if (!isReady(block.getWorld())) {
            throw new IllegalStateException(ERR_WORLD_NOT_READY);
        }

        Location bLoc = block.getLocation();
        Set<Anchor> set = this.anchors.get(block.getWorld());

        if (set != null) {
            for (Anchor anchor : set) {
                if (anchor.getLocation().equals(bLoc)) {
                    return anchor;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isAnchor(@NotNull Block block) {
        return getAnchor(block) != null;
    }

    @Override
    public boolean hasAnchor(@NotNull Chunk chunk) {
        if (!isReady(chunk.getWorld())) {
            throw new IllegalStateException(ERR_WORLD_NOT_READY);
        }

        Set<Anchor> set = this.anchors.get(chunk.getWorld());

        if (set != null) {
            for (Anchor anchor : set) {
                if (anchor.getChunk().equals(chunk)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @SuppressWarnings("unused")
    public List<Anchor> searchAnchors(Location center, double searchRadius) {
        return searchAnchors(center, searchRadius, false);
    }

    @Override
    public List<Anchor> searchAnchors(Location center, double searchRadius, boolean ignoreHeight) {
        List<Anchor> result = new ArrayList<>();

        if (ignoreHeight) {
            center = center.clone();
            center.setY(0);
        }

        for (Anchor anchor : getAnchors(center.getWorld())) {
            Location loc = anchor.getLocation();

            if (ignoreHeight) {
                loc.setY(0);
            }

            if (center.distance(loc) <= searchRadius) {
                result.add(anchor);
            }
        }

        return result;
    }

    /* Create 'n Destroy */

    @Override
    public void createAnchor(@NotNull Location loc, @NotNull UUID owner, int ticks, @Nullable Callback<Anchor> callback) {
        if (!isReady(loc.getWorld())) {
            throw new IllegalStateException(ERR_WORLD_NOT_READY);
        }

        this.dataManager.insertAnchorAsync(loc, Objects.requireNonNull(owner), ticks, (ex, anchor) -> {
            if (ex != null) {
                if (callback != null) {
                    callback.accept(ex, null);
                } else {
                    Utils.logException(this.plugin, ex, "H2");
                }
            } else {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    Block block = loc.getBlock();
                    block.setType(Settings.MATERIAL.getMaterial().parseMaterial());

                    this.anchors.computeIfAbsent(anchor.getWorld(), key -> new HashSet<>())
                            .add(anchor);

                    updateHologram(anchor);

                    if (callback != null) {
                        callback.accept(null, anchor);
                    }
                });
            }
        });
    }

    @Override
    public void destroyAnchor(@NotNull Anchor anchor) {
        destroyAnchor(anchor, false);
    }

    @Override
    public void destroyAnchor(@NotNull Anchor anchor, boolean forceSkipItemDrop) {
        if (!isReady(anchor.getWorld())) {
            throw new IllegalStateException(ERR_WORLD_NOT_READY);
        }

        for (Set<Anchor> value : this.anchors.values()) {
            value.remove(anchor);
        }

        removeHologram(anchor);

        Location anchorLoc = anchor.getLocation();
        Block anchorBlock = anchorLoc.getBlock();
        Material anchorMaterial = anchorBlock.getType();

        if (anchorBlock.getType() == Settings.MATERIAL.getMaterial().parseMaterial()) {
            anchorBlock.setType(Material.AIR);
        }

        // Drop anchor as an item
        if (!forceSkipItemDrop &&
                Settings.ALLOW_ANCHOR_BREAKING.getBoolean() &&
                (anchor.isInfinite() || anchor.getTicksLeft() >= 20)) {
            anchor.getWorld().dropItemNaturally(anchorLoc, createAnchorItem(anchor.getTicksLeft(), anchorMaterial));
        }

        // Particles & Sound
        XSound.ENTITY_GENERIC_EXPLODE.play(anchorLoc, 10, 10);
        CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.getParticle(Settings.PARTICLE_DESTROY.getString()),
                anchor.getLocation().add(.5, .5, .5), 100, .5, .5, .5);

        ((AnchorImpl) anchor).deInit(this.plugin);
        this.dataManager.deleteAnchorAsync(anchor);
    }

    /* Anchor access */

    @Override
    public void registerAccessCheck(AnchorAccessCheck accessCheck) {
        if (!this.accessChecks.contains(accessCheck)) {
            // Adding at the start of the list makes sure the default check is
            this.accessChecks.add(accessCheck);
        }
    }

    @Override
    public boolean unregisterAccessCheck(AnchorAccessCheck accessCheck) {
        return this.accessChecks.remove(accessCheck);
    }

    @Override
    public boolean hasAccess(@NotNull Anchor anchor, @NotNull OfflinePlayer p) {
        return hasAccess(anchor, p.getUniqueId());
    }

    @Override
    public boolean hasAccess(@NotNull Anchor anchor, @NotNull UUID uuid) {
        if (anchor.isLegacy() || anchor.getOwner().equals(uuid)) return true;

        for (AnchorAccessCheck accessCheck : this.accessChecks) {
            if (accessCheck.check(anchor, uuid)) {
                return true;
            }
        }

        return false;
    }

    /* Anchor item */

    @Override
    public ItemStack createAnchorItem(int ticks) {
        return createAnchorItem(ticks, Settings.MATERIAL.getMaterial());
    }

    @Override
    public ItemStack createAnchorItem(int ticks, Material material) {
        return createAnchorItem(ticks, CompatibleMaterial.getMaterial(material).get());
    }

    @Override
    public ItemStack createAnchorItem(int ticks, XMaterial material) {
        if (ticks <= 0 && ticks != -1) throw new IllegalArgumentException();

        ItemStack item = material.parseItem();
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(formatAnchorText(ticks, false));
        meta.setLore(TextUtils.formatText(Settings.LORE.getString().split("\r?\n")));
        item.setItemMeta(meta);

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger(NBT_TICKS_KEY, ticks);

        return nbtItem.getItem();
    }

    /* Chunk visualization */

    @Override
    public boolean toggleChunkVisualized(Player p) {
        boolean visualize = !hasChunksVisualized(p);

        setChunksVisualized(p, visualize);

        return visualize;
    }

    @Override
    public void setChunksVisualized(Player p, boolean visualize) {
        if (visualize) {
            this.visualizedChunk.add(p);
        } else {
            this.visualizedChunk.remove(p);
        }
    }

    @Override
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasChunksVisualized(Player p) {
        return this.visualizedChunk.contains(p);
    }

    /* Holograms */

    @Override
    public void updateHolograms(List<Anchor> anchors) {
        // are holograms enabled?
        if (!Settings.HOLOGRAMS.getBoolean() || !HologramManager.getManager().isEnabled()) return;

        Map<String, List<String>> hologramData = new HashMap<>(anchors.size());

        for (Anchor anchor : anchors) {
            List<String> lines = Collections.singletonList(formatAnchorText(anchor.getTicksLeft(), true));

            if (!HologramManager.isHologramLoaded(HOLOGRAM_PREFIX + anchor.getDbId())) {
                HologramManager.createHologram(HOLOGRAM_PREFIX + anchor.getDbId(), anchor.getLocation(), lines);
                continue;
            }

            hologramData.put(HOLOGRAM_PREFIX + anchor.getDbId(), lines);
        }

        // Create the holograms
        HologramManager.bulkUpdateHolograms(hologramData);
    }

    private void updateHologram(Anchor anchor) {
        updateHolograms(Collections.singletonList(anchor));
    }

    private String formatAnchorText(int ticks, boolean shorten) {
        String remaining;

        if (ticks < 0) {
            remaining = this.plugin.getLocale().getMessage("Infinite").toText();
        } else {
            long millis = (ticks / 20L) * 1000L;

            remaining = TimeUtils.makeReadable(millis);

            if (shorten && millis > 60 * 5 * 1000 /* 5 minutes */ &&
                    remaining.charAt(remaining.length() - 1) == 's') {
                int i = remaining.lastIndexOf(' ');

                remaining = remaining.substring(0, i);
            }

            if (remaining.isEmpty()) {
                remaining = "0s";
            }
        }

        return TextUtils.formatText(Settings.NAME_TAG.getString().replace("{REMAINING}", remaining));
    }

    public static int getTicksFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }

        NBTItem nbtItem = new NBTItem(item);

        if (nbtItem.hasTag(NBT_TICKS_KEY)) {
            return nbtItem.getInteger(NBT_TICKS_KEY);
        }

        // Legacy code (pre v2) to stay cross-version compatible
        if (Settings.MATERIAL.getMaterial().parseMaterial() == item.getType()) {
            if (nbtItem.hasTag("ticks")) {
                int result = nbtItem.getInteger("ticks");

                return result == -99 ? -1 : result;
            }

            // Tries to get the ticks remaining from hidden text
            if (item.hasItemMeta() &&
                    item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains(":")) {
                try {
                    int result = Integer.parseInt(item.getItemMeta().getDisplayName().replace("§", "").split(":")[0]);

                    return result == -99 ? -1 : result;
                } catch (NumberFormatException ignore) {
                }
            }
        }

        return 0;
    }

    private static void removeHologram(Anchor anchor) {
        HologramManager.removeHologram(HOLOGRAM_PREFIX + anchor.getDbId());
    }
}
