package com.songoda.epicanchors.commands.sub;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.epicanchors.EpicAnchors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ShowCommand extends AbstractCommand {
    private final EpicAnchors plugin;

    public ShowCommand(EpicAnchors plugin) {
        super(CommandType.PLAYER_ONLY, false, "show");

        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Command must be called as a player");

            return ReturnType.FAILURE;
        }

        if (args.length != 0) return ReturnType.SYNTAX_ERROR;

        boolean visualize = this.plugin.getAnchorManager().toggleChunkVisualized((Player) sender);

        this.plugin.getLocale().getMessage("command.show." + (visualize ? "start" : "stop"))
                .sendPrefixedMessage(sender);

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionNode() {
        return "EpicAnchors.cmd.show";
    }

    @Override
    public String getSyntax() {
        return "show";
    }

    @Override
    public String getDescription() {
        return "Visualize anchors around you";
    }
}
