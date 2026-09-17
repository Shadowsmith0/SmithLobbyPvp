package us.asooner.smithpvp.commands;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import us.asooner.smithpvp.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SmithPvPCommand implements CommandExecutor {

    private final SmithPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;
    private final MessageUtil messages;

    public SmithPvPCommand(SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager,
                            PvpStateManager pvpStateManager, MessageUtil messages) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
        this.pvpStateManager = pvpStateManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
            return true;
        }

        messages.send(sender, "usage");
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(MessageUtil.colorize("&b&lSmithpvp &7v" + plugin.getDescription().getVersion()
                + " &8- &7by &fSmith the CEO of Asooner"));
        sender.sendMessage(MessageUtil.colorize("&e/" + label + " help &8- &7Show this help menu"));
        if (sender.hasPermission("smithpvp.reload")) {
            sender.sendMessage(MessageUtil.colorize("&e/" + label + " reload &8- &7Reload the Smithpvp configuration"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("smithpvp.reload")) {
            messages.send(sender, "no-permission");
            return;
        }

        plugin.reloadConfig();

        messages.send(sender, "reloaded");
    }
}
