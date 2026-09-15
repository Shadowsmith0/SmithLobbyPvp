package me.claude.lobbypvp.listeners;

import me.claude.lobbypvp.LobbyPvPPlugin;
import me.claude.lobbypvp.managers.ItemManager;
import me.claude.lobbypvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final LobbyPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;

    public PlayerRespawnListener(LobbyPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (player.hasPermission("lobbypvp.bypass")) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> itemManager.giveKit(player), 1L);
    }
}
