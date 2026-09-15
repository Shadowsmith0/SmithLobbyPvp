package me.claude.lobbypvp.commands;

import me.claude.lobbypvp.LobbyPvPPlugin;
import me.claude.lobbypvp.managers.ItemManager;
import me.claude.lobbypvp.managers.PvpStateManager;
import me.claude.lobbypvp.managers.WorldManager;
import me.claude.lobbypvp.utils.MessageUtil;
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
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, "usage");
            return true;
        }

        if (!sender.hasPermission("lobbypvp.reload")) {
            messages.send(sender, "no-permission");
            return true;
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
        return true;
    }
}
