package com.songoda.epicanchors.commands.sub;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.epicanchors.EpicAnchors;
import com.songoda.epicanchors.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GiveCommand extends AbstractCommand {
    private final EpicAnchors plugin;

    public GiveCommand(EpicAnchors plugin) {
        super(CommandType.CONSOLE_OK, false, "give");

        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length != 2) return ReturnType.SYNTAX_ERROR;

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null &&
                !args[0].trim().equalsIgnoreCase("all") &&
                !args[0].trim().equalsIgnoreCase("@a")) {
            this.plugin.getLocale().newMessage("&cThat is not a player...").sendPrefixedMessage(sender);

            return ReturnType.SYNTAX_ERROR;
        }

        ItemStack itemStack;

        if (Utils.isInt(args[1]) && Integer.parseInt(args[1]) > 0) {
            itemStack = this.plugin.getAnchorManager().createAnchorItem(Integer.parseInt(args[1]) * 20 * 60 * 60);
        } else if (args[1].equalsIgnoreCase("infinite")) {
            itemStack = this.plugin.getAnchorManager().createAnchorItem(-1);
        } else {
            this.plugin.getLocale().newMessage("&cYou can only use positive whole numbers...").sendPrefixedMessage(sender);

            return ReturnType.FAILURE;
        }

        if (target != null) {
            target.getInventory().addItem(itemStack);
            this.plugin.getLocale().getMessage("command.give.success").sendPrefixedMessage(target);
        } else {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.getInventory().addItem(itemStack);
                this.plugin.getLocale().getMessage("command.give.success").sendPrefixedMessage(online);
            }
        }

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        if (args.length == 1) {
            Set<String> players = new HashSet<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }

            players.add("all");

            if ("@a".startsWith(args[0].toLowerCase())) {
                players.add("@a");
            }

            return Utils.getMatches(args[0], players, true);
        } else if (args.length == 2) {
            List<String> result = new ArrayList<>();

            result.add("infinite");

            if (args[1].isEmpty()) {
                for (int i = 1; i <= 5; ++i) {
                    result.add(String.valueOf(i));
                }
            } else if (Utils.isInt(args[1]) && args[1].charAt(0) != '-') {
                result.add(args[1] + "0");
                result.add(args[1] + "00");
                result.add(args[1] + "000");
            }

            return Utils.getMatches(args[1], result, true);
        }

        return Collections.emptyList();
    }

    @Override
    public String getPermissionNode() {
        return "EpicAnchors.cmd.give";
    }

    @Override
    public String getSyntax() {
        return "/ea give <player/all> <amount in hours / infinite>";
    }

    @Override
    public String getDescription() {
        return "Gives an operator the ability to spawn a ChunkAnchor of his or her choice.";
    }
}
