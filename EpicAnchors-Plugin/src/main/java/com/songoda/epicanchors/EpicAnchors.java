package com.songoda.epicanchors;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.configuration.Config;
import com.songoda.core.dependency.Dependency;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.hooks.HologramManager;
import com.songoda.epicanchors.api.AnchorManager;
import com.songoda.epicanchors.commands.EpicAnchorsCommand;
import com.songoda.epicanchors.commands.sub.GiveCommand;
import com.songoda.epicanchors.commands.sub.ReloadCommand;
import com.songoda.epicanchors.commands.sub.SettingsCommand;
import com.songoda.epicanchors.commands.sub.ShowCommand;
import com.songoda.epicanchors.files.AnchorsDataManager;
import com.songoda.epicanchors.files.Settings;
import com.songoda.epicanchors.files.migration.LegacyYamlAnchorsMigrator;
import com.songoda.epicanchors.files.migration._1_InitialMigration;
import com.songoda.epicanchors.listener.AnchorListener;
import com.songoda.epicanchors.listener.BlockListener;
import com.songoda.epicanchors.listener.WorldListener;
import com.songoda.epicanchors.tasks.AnchorTask;
import com.songoda.epicanchors.tasks.VisualizeTask;
import com.songoda.epicanchors.utils.ThreadSync;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class EpicAnchors extends SongodaPlugin {
    private GuiManager guiManager;
    private AnchorManagerImpl anchorManager;

    private AnchorsDataManager dataManager;

    @Override
    public void onPluginLoad() {
    }

    @Override
    public void onPluginEnable() {
        SongodaCore.registerPlugin(this, 31, XMaterial.END_PORTAL_FRAME);

        initializeDataManager();

        this.anchorManager = new AnchorManagerImpl(this, this.dataManager);
        EpicAnchorsApi.initApi(this.anchorManager);

        // Economy [1/2]
        EconomyManager.load();

        // Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUAGE.getString(), false);

        // Economy [2/2]
        EconomyManager.getManager().setPreferredHook(Settings.ECONOMY_PLUGIN.getString());

        // Holograms
        HologramManager.load(this);

        // Event Listener
        this.guiManager = new GuiManager(this);
        this.guiManager.init();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new WorldListener(
                        world -> this.anchorManager.initAnchorsAsync(world, null),
                        world -> this.anchorManager.deInitAnchors(world)),
                this);
        pluginManager.registerEvents(new AnchorListener(this), this);
        pluginManager.registerEvents(new BlockListener(this.anchorManager), this);

        // Commands
        CommandManager commandManager = new CommandManager(this);
        commandManager.addCommand(new EpicAnchorsCommand(this, commandManager))
                .addSubCommands(
                        new GiveCommand(this),
                        new ReloadCommand(this),
                        new SettingsCommand(this, this.guiManager),
                        new ShowCommand(this)
                );
    }

    @Override
    public void onPluginDisable() {
        // Save all Anchors
        if (this.dataManager != null) {
            this.anchorManager.deInitAll();

            this.dataManager.close();
        }

        // Remove all holograms
        HologramManager.removeAllHolograms();
    }

    @Override
    public void onDataLoad() {
        new Thread(() -> {
            ThreadSync tSync = new ThreadSync();

            for (World w : Bukkit.getWorlds()) {
                this.anchorManager.initAnchorsAsync(w, (ex) -> {
                    if (ex != null) {
                        this.getLogger().log(Level.SEVERE, ex, () -> "Failed to initialize world '" + w.getName() + "'");
                    }

                    tSync.release();
                });

                tSync.waitForRelease();
                tSync.reset();
            }

            this.anchorManager.setReady();

            // Start tasks
            new AnchorTask(this).startTask();
            new VisualizeTask(this).startTask();
        }).start();
    }

    @Override
    public void onConfigReload() {
        this.setLocale(Settings.LANGUAGE.getString(), true);
    }

    @Override
    public List<Config> getExtraConfig() {
        return Collections.emptyList();
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public AnchorManager getAnchorManager() {
        return this.anchorManager;
    }

    @Override
    public Config getDatabaseConfig() {
        Config staticDatabaseConfig = new Config();
        staticDatabaseConfig.set("Connection Settings.Type", "H2");
        staticDatabaseConfig.set("Connection Settings.Pool Size", 1);
        return staticDatabaseConfig;
    }

    private void initializeDataManager() {
        super.initDatabase(new _1_InitialMigration());

        this.dataManager = new AnchorsDataManager(this);
        LegacyYamlAnchorsMigrator.migrateLegacyData(this, this.dataManager);
    }
}
