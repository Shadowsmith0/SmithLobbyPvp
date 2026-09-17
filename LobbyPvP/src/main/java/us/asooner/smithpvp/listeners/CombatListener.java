package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final SmithPvPPlugin plugin;
    private final PvpStateManager pvpStateManager;
    private final WorldManager worldManager;

    public CombatListener(SmithPvPPlugin plugin, PvpStateManager pvpStateManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.pvpStateManager = pvpStateManager;
        this.worldManager = worldManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victimEntity = event.getEntity();

        if (!(damager instanceof Player attacker) || !(victimEntity instanceof Player victim)) return;
        if (!worldManager.isActiveWorld(attacker.getWorld())) return;

        if (attacker.hasPermission("smithpvp.bypass") || victim.hasPermission("smithpvp.bypass")) {
            event.setCancelled(true);
            return;
        }

        boolean bothArmed = pvpStateManager.isInPvp(attacker) && pvpStateManager.isInPvp(victim);

        if (bothArmed) {
            if (plugin.getConfig().getBoolean("compatibility.force-override-other-protections", true)) {
                // Both players have PvP armed: force the hit through, even if some other
                // plugin (region protection, anti-pvp zones, etc.) already cancelled it.
                // Since we run at MONITOR priority we execute last, after everyone else.
                event.setCancelled(false);
            }
            return;
        }

        event.setCancelled(true);
    }
}
