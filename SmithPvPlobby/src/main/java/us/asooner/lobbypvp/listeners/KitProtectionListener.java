package us.asooner.lobbypvp.listeners;

import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.WorldManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class KitProtectionListener implements Listener {

    private final ItemManager itemManager;
    private final WorldManager worldManager;

    public KitProtectionListener(ItemManager itemManager, WorldManager worldManager) {
        this.itemManager = itemManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        if (itemManager.isKitItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) return;
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        if (itemManager.isKitItem(event.getCurrentItem()) || itemManager.isKitItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) return;
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        if (itemManager.isKitItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;

        if (itemManager.isKitItem(event.getMainHandItem()) || itemManager.isKitItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}
