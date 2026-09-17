package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemHeldListener implements Listener {

    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;

    public ItemHeldListener(ItemManager itemManager, WorldManager worldManager, PvpStateManager pvpStateManager) {
        this.itemManager = itemManager;
        this.worldManager = worldManager;
        this.pvpStateManager = pvpStateManager;
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        PlayerInventory inv = player.getInventory();
        ItemStack previous = inv.getItem(event.getPreviousSlot());
        ItemStack next = inv.getItem(event.getNewSlot());

        boolean wasHoldingSword = itemManager.isWeapon(previous);
        boolean isHoldingSwordNow = itemManager.isWeapon(next);

        pvpStateManager.handleHeldItemChange(player, wasHoldingSword, isHoldingSwordNow);
    }
}
