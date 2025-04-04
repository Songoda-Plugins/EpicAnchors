package com.songoda.epicanchors.commands.sub;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.core.configuration.editor.PluginConfigGui;
import com.songoda.core.gui.GuiManager;
import com.songoda.epicanchors.EpicAnchors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SettingsCommand extends AbstractCommand {
    private final EpicAnchors instance;
    private final GuiManager guiManager;

    public SettingsCommand(EpicAnchors instance, GuiManager manager) {
        super(CommandType.PLAYER_ONLY, false, "settings");

        this.instance = instance;
        this.guiManager = manager;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        this.guiManager.showGUI((Player) sender, new PluginConfigGui(this.instance));

        return AbstractCommand.ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionNode() {
        return "EpicAnchors.cmd.settings";
    }

    @Override
    public String getSyntax() {
        return "/ea settings";
    }

    @Override
    public String getDescription() {
        return "Edit the EpicAnchors Settings.";
    }
}
