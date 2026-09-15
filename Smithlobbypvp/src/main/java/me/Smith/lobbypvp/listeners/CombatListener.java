package me.claude.lobbypvp.listeners;

import me.claude.lobbypvp.managers.PvpStateManager;
import me.claude.lobbypvp.managers.WorldManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final PvpStateManager pvpStateManager;
    private final WorldManager worldManager;

    public CombatListener(PvpStateManager pvpStateManager, WorldManager worldManager) {
        this.pvpStateManager = pvpStateManager;
        this.worldManager = worldManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victimEntity = event.getEntity();

        if (!(damager instanceof Player attacker) || !(victimEntity instanceof Player victim)) return;
        if (!worldManager.isActiveWorld(attacker.getWorld())) return;

        if (attacker.hasPermission("lobbypvp.bypass") || victim.hasPermission("lobbypvp.bypass")) {
            event.setCancelled(true);
            return;
        }

        if (!pvpStateManager.isInPvp(attacker) || !pvpStateManager.isInPvp(victim)) {
            event.setCancelled(true);
        }
    }
}
