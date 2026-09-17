package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final SmithPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;

    public PlayerJoinListener(SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (player.hasPermission("smithpvp.bypass")) return;

        if (plugin.getConfig().getBoolean("general.heal-on-join", true)) {
            var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                player.setHealth(maxHealthAttr.getValue());
            }
        }

        itemManager.giveKit(player);
    }
}
