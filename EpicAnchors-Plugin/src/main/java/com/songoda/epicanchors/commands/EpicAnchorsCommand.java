package com.songoda.epicanchors.commands;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.core.commands.CommandManager;
import com.songoda.epicanchors.EpicAnchors;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class EpicAnchorsCommand extends AbstractCommand {
    private final EpicAnchors plugin;
    private final CommandManager commandManager;

    public EpicAnchorsCommand(EpicAnchors plugin, CommandManager commandManager) {
        super(CommandType.CONSOLE_OK, false, "EpicAnchors");

        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        sender.sendMessage("");
        this.plugin.getLocale().newMessage("&7Version " + this.plugin.getDescription().getVersion()
                + " Created with <3 by &5&l&oCraftaro").sendPrefixedMessage(sender);

        for (AbstractCommand cmd : this.commandManager.getAllCommands()) {
            if (cmd.getPermissionNode() == null || sender.hasPermission(cmd.getPermissionNode())) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8 - &a" + cmd.getSyntax() + "&7 - " + cmd.getDescription()));
            }
        }
        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionNode() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "/EpicAnchors";
    }

    @Override
    public String getDescription() {
        return "Displays this page.";
    }
}
