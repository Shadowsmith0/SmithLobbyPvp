package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final us.asooner.smithpvp.SmithPvPPlugin plugin;

    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;

    public PlayerTeleportListener(us.asooner.smithpvp.SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager, PvpStateManager pvpStateManager) {
        this.plugin = plugin;
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

        if (player.hasPermission("smithpvp.bypass")) return;

        boolean fromActive = from != null && worldManager.isActiveWorld(from);
        boolean toActive = worldManager.isActiveWorld(to);

        if (fromActive && !toActive) {
            // Event fires before the world switch: remove only Smithpvp-tagged kit from
            // the source inventory. Once the destination inventory is loaded, we never touch it.
            itemManager.takeKit(player);
            pvpStateManager.removeFromPvp(player);
            return;
        }

        if (!fromActive && toActive) {
            // Wait one tick so Multiverse-Inventories can finish loading the destination inventory.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && worldManager.isActiveWorld(player.getWorld())) itemManager.giveKit(player);
            });
        }
    }
}
