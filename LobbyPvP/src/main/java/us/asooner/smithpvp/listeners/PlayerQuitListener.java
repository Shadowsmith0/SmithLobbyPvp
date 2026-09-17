package us.asooner.smithpvp.listeners;

import us.asooner.smithpvp.managers.PvpStateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final PvpStateManager pvpStateManager;

    public PlayerQuitListener(PvpStateManager pvpStateManager) {
        this.pvpStateManager = pvpStateManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        pvpStateManager.removeFromPvp(player);
    }
}
