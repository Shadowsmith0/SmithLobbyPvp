package us.asooner.lobbypvp.commands;

import us.asooner.lobbypvp.LobbyPvPPlugin;
import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.PvpStateManager;
import us.asooner.lobbypvp.managers.WorldManager;
import us.asooner.lobbypvp.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyPvPCommand implements CommandExecutor {

    private final LobbyPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;
    private final MessageUtil messages;

    public LobbyPvPCommand(LobbyPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager,
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
        sender.sendMessage(MessageUtil.colorize("&b&lLobbyPvP &7v" + plugin.getDescription().getVersion()
                + " &8- &7by &fSmith the CEO of Asooner"));
        sender.sendMessage(MessageUtil.colorize("&e/" + label + " help &8- &7Show this help menu"));
        if (sender.hasPermission("lobbypvp.reload")) {
            sender.sendMessage(MessageUtil.colorize("&e/" + label + " reload &8- &7Reload the LobbyPvP configuration"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lobbypvp.reload")) {
            messages.send(sender, "no-permission");
            return;
        }

        plugin.reloadConfig();

        // Refresh every online player's kit so item changes (material/name/enchants) apply instantly.
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("lobbypvp.bypass")) continue;
            if (!worldManager.isActiveWorld(player.getWorld())) continue;

            pvpStateManager.removeFromPvp(player);
            itemManager.takeKit(player);
            itemManager.giveKit(player);
        }

        messages.send(sender, "reloaded");
    }
}
