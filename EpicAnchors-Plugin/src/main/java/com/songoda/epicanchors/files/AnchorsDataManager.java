package com.songoda.epicanchors.files;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.database.DatabaseConnector;
import com.songoda.epicanchors.AnchorImpl;
import com.songoda.epicanchors.api.Anchor;
import com.songoda.epicanchors.files.migration.LegacyYamlAnchorsMigrator;
import com.songoda.epicanchors.utils.Callback;
import com.songoda.epicanchors.utils.UpdateCallback;
import com.songoda.epicanchors.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnchorsDataManager {
    private final ExecutorService thread = Executors.newSingleThreadExecutor();
    private final SongodaPlugin plugin;

    public AnchorsDataManager(SongodaPlugin plugin) {
        this.plugin = plugin;
    }

    public void close() {
        if (this.thread.isShutdown()) {
            return;
        }

        this.thread.shutdown();
        try {
            if (!this.thread.awaitTermination(60, TimeUnit.SECONDS)) {
                // Try stopping the thread forcefully (there is basically no hope left for the data)
                this.thread.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Utils.logException(this.plugin, ex);
        }

        this.plugin.getDataManager().shutdown();
    }

    public void exists(@NotNull String worldName, int x, int y, int z, @NotNull Callback<Boolean> callback) {
        getDatabaseConnector().connect((con) -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM " + getAnchorTable() + " WHERE world_name =? AND x =? AND y =? AND z=?;")) {
                ps.setString(1, worldName);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);

                ResultSet rs = ps.executeQuery();

                callback.accept(null, rs.next());
            } catch (Exception ex) {
                resolveCallback(callback, ex);
            }
        });
    }

    public void getAnchors(@Nullable World world, @NotNull Callback<List<Anchor>> callback) {
        List<Anchor> result = new ArrayList<>();

        getDatabaseConnector().connect((con) -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM " + getAnchorTable() + (world != null ? " WHERE world_name =?" : "") + ";")) {
                if (world != null) {
                    ps.setString(1, world.getName());
                }

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    result.add(extractAnchor(rs));
                }

                callback.accept(null, result);
            } catch (Exception ex) {
                resolveCallback(callback, ex);
            }
        });
    }

    public void insertAnchorAsync(Location loc, UUID owner, int ticks, Callback<Anchor> callback) {
        this.thread.execute(() -> insertAnchor(loc, owner, ticks, callback));
    }

    public void insertAnchor(Location loc, UUID owner, int ticks, Callback<Anchor> callback) {
        getDatabaseConnector().connect((con) -> {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + getAnchorTable() + "(owner, world_name,x,y,z, ticks_left) VALUES (?,?,?,?,?, ?);"); // Future SQLite version might support 'RETURNING *'
                 PreparedStatement psFetch = con.prepareStatement("SELECT * FROM " + getAnchorTable() + " WHERE world_name =? AND x =? AND y =? AND z=?;")
            ) {
                ps.setString(1, owner != null ? owner.toString() : null);

                ps.setString(2, Objects.requireNonNull(loc.getWorld()).getName());
                psFetch.setString(1, Objects.requireNonNull(loc.getWorld()).getName());

                ps.setInt(3, loc.getBlockX());
                psFetch.setInt(2, loc.getBlockX());

                ps.setInt(4, loc.getBlockY());
                psFetch.setInt(3, loc.getBlockY());

                ps.setInt(5, loc.getBlockZ());
                psFetch.setInt(4, loc.getBlockZ());

                ps.setInt(6, ticks);

                ps.executeUpdate();

                if (callback != null) {
                    ResultSet rs = psFetch.executeQuery();
                    rs.next();

                    callback.accept(null, extractAnchor(rs));
                }
            } catch (Exception ex) {
                resolveCallback(callback, ex);
            }
        });
    }

    public void migrateAnchor(List<LegacyYamlAnchorsMigrator.LegacyAnchorEntry> anchorEntries, UpdateCallback callback) {
        getDatabaseConnector().connect((con) -> {
            con.setAutoCommit(false);

            SQLException err = null;

            try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + getAnchorTable() + "(world_name,x,y,z, ticks_left) VALUES (?,?,?,?, ?);")) {
                for (LegacyYamlAnchorsMigrator.LegacyAnchorEntry entry : anchorEntries) {
                    ps.setString(1, entry.worldName);
                    ps.setInt(2, entry.x);
                    ps.setInt(3, entry.y);
                    ps.setInt(4, entry.z);

                    ps.setInt(5, entry.ticksLeft);

                    ps.addBatch();
                }

                int[] batchRes = ps.executeBatch();

                for (int i : batchRes) {
                    if (i < 0 && i != Statement.SUCCESS_NO_INFO) {
                        throw new AssertionError("Batch-INSERT failed for at least one statement with code " + i);
                    }
                }
            } catch (SQLException ex) {
                err = ex;
            }

            if (err == null) {
                con.commit();

                resolveUpdateCallback(callback, null);
            } else {
                con.rollback();

                resolveUpdateCallback(callback, err);
            }

            con.setAutoCommit(true);
        });
    }

    public void updateAnchors(Collection<Anchor> anchors, UpdateCallback callback) {
        getDatabaseConnector().connect((con) -> {
            con.setAutoCommit(false);

            SQLException err = null;

            for (Anchor anchor : anchors) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE " + getAnchorTable() + " SET ticks_left =? WHERE id =?;")) {
                    ps.setInt(1, anchor.getTicksLeft());
                    ps.setInt(2, anchor.getDbId());

                    ps.executeUpdate();
                } catch (SQLException ex) {
                    err = ex;
                    break;
                }
            }

            if (err == null) {
                con.commit();

                resolveUpdateCallback(callback, null);
            } else {
                con.rollback();

                resolveUpdateCallback(callback, err);
            }

            con.setAutoCommit(true);
        });
    }

    public void deleteAnchorAsync(Anchor anchor) {
        deleteAnchorAsync(anchor, null);
    }

    public void deleteAnchorAsync(Anchor anchor, UpdateCallback callback) {
        this.thread.execute(() ->
                getDatabaseConnector().connect((con) -> {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + getAnchorTable() + " WHERE id =?;")) {
                        ps.setInt(1, anchor.getDbId());

                        ps.executeUpdate();

                        resolveUpdateCallback(callback, null);
                    } catch (Exception ex) {
                        resolveUpdateCallback(callback, ex);
                    }
                })
        );
    }

    public String getAnchorTable() {
        return getAnchorTable(this.plugin.getDataManager().getTablePrefix());
    }

    public static String getAnchorTable(String prefix) {
        return prefix + "anchors";
    }

    private DatabaseConnector getDatabaseConnector() {
        return this.plugin.getDataManager().getDatabaseConnector();
    }

    private Anchor extractAnchor(ResultSet rs) throws SQLException {
        String ownerStr = rs.getString("owner");

        return new AnchorImpl(rs.getInt("id"),
                ownerStr != null ? UUID.fromString(ownerStr) : null,
                new Location(Bukkit.getWorld(rs.getString("world_name")),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")),
                rs.getInt("ticks_left"));
    }

    private void resolveUpdateCallback(@Nullable UpdateCallback callback, @Nullable Exception ex) {
        if (callback != null) {
            callback.accept(ex);
        } else if (ex != null) {
            Utils.logException(this.plugin, ex, "H2");
        }
    }

    private void resolveCallback(@Nullable Callback<?> callback, @NotNull Exception ex) {
        if (callback != null) {
            callback.accept(ex, null);
        } else {
            Utils.logException(this.plugin, ex, "H2");
        }
    }
}
