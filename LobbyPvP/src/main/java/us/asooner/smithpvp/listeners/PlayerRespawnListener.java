package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {
    private final SmithPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;

    public PlayerRespawnListener(SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager, PvpStateManager pvpStateManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
        this.pvpStateManager = pvpStateManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        pvpStateManager.removeFromPvp(player);
        if (!worldManager.isActiveWorld(event.getRespawnLocation().getWorld())) return;
        if (player.hasPermission("smithpvp.bypass")) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !worldManager.isActiveWorld(player.getWorld())) return;
            itemManager.takeArmor(player); // death must never leave Smithpvp armor equipped
            itemManager.giveWeapon(player);
            int slot = Math.max(0, Math.min(8, plugin.getConfig().getInt("pvp.post-death-slot", 0)));
            player.getInventory().setHeldItemSlot(slot);
        });
    }
}
