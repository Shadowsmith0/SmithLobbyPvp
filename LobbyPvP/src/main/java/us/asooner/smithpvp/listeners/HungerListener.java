package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerListener implements Listener {

    private final WorldManager worldManager;

    public HungerListener(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onHungerLoss(FoodLevelChangeEvent event) {
        HumanEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        event.setCancelled(true);
        player.setFoodLevel(20);
    }
}
