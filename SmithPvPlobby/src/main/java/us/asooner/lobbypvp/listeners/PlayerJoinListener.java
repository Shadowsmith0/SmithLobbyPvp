package us.asooner.lobbypvp.listeners;

import us.asooner.lobbypvp.LobbyPvPPlugin;
import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final LobbyPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;

    public PlayerJoinListener(LobbyPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (player.hasPermission("lobbypvp.bypass")) return;

        if (plugin.getConfig().getBoolean("heal-on-join", true)) {
            var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                player.setHealth(maxHealthAttr.getValue());
            }
        }

        itemManager.giveKit(player);
    }
}
