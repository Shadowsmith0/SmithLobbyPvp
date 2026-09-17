package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final SmithPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;

    public PlayerRespawnListener(SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (player.hasPermission("smithpvp.bypass")) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && worldManager.isActiveWorld(player.getWorld())) itemManager.giveKit(player);
        }, 1L);
    }
}
