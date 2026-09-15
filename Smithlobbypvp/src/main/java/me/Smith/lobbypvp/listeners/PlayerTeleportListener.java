package me.claude.lobbypvp.listeners;

import me.claude.lobbypvp.managers.ItemManager;
import me.claude.lobbypvp.managers.PvpStateManager;
import me.claude.lobbypvp.managers.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;

    public PlayerTeleportListener(ItemManager itemManager, WorldManager worldManager, PvpStateManager pvpStateManager) {
        this.itemManager = itemManager;
        this.worldManager = worldManager;
        this.pvpStateManager = pvpStateManager;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World from = event.getFrom().getWorld();
        World to = event.getTo() != null ? event.getTo().getWorld() : null;
        if (to == null || (from != null && from.equals(to))) return;

        if (player.hasPermission("lobbypvp.bypass")) return;

        if (worldManager.isActiveWorld(to)) {
            itemManager.giveKit(player);
        } else {
            // Leaving the active world: take the kit and clear their armed PvP state.
            itemManager.takeKit(player);
            pvpStateManager.removeFromPvp(player);
        }
    }
}
