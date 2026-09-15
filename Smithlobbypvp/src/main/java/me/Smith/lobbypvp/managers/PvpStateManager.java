package me.claude.lobbypvp.managers;

import me.claude.lobbypvp.LobbyPvPPlugin;
import me.claude.lobbypvp.utils.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players currently have PvP "armed" and drives the
 * countdown that happens when a player picks up / puts away the sword.
 */
public class PvpStateManager {

    private final LobbyPvPPlugin plugin;
    private final ItemManager itemManager;
    private final MessageUtil messages;

    private final Set<UUID> playersInPvp = new HashSet<>();
    private final Map<UUID, BukkitTask> activeTimers = new HashMap<>();

    public PvpStateManager(LobbyPvPPlugin plugin, ItemManager itemManager, MessageUtil messages) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.messages = messages;
    }

    public boolean isInPvp(Player player) {
        return playersInPvp.contains(player.getUniqueId());
    }

    public void removeFromPvp(Player player) {
        playersInPvp.remove(player.getUniqueId());
        cancelActiveTimer(player.getUniqueId());
    }

    private void cancelActiveTimer(UUID uuid) {
        BukkitTask task = activeTimers.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Call this whenever the player's held-item slot changes. Figures out
     * whether they just grabbed the sword, just let go of it, or the change
     * is irrelevant, and (re)starts the appropriate countdown.
     *
     * A short "debounce" grace period (held-item-debounce-ticks) runs first so that
     * quickly scrolling past the sword's slot doesn't fire off a countdown by accident -
     * we only actually start counting down if the new state is still true a few ticks later.
     */
    public void handleHeldItemChange(Player player, boolean wasHoldingSword, boolean isHoldingSwordNow) {
        if (wasHoldingSword == isHoldingSwordNow) return;

        UUID uuid = player.getUniqueId();
        cancelActiveTimer(uuid);

        int debounceTicks = plugin.getConfig().getInt("held-item-debounce-ticks", 4);
        if (debounceTicks <= 0) {
            startCountdown(player, isHoldingSwordNow);
            return;
        }

        BukkitTask debounceTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            boolean stillMatches = itemManager.isWeapon(player.getInventory().getItemInMainHand()) == isHoldingSwordNow;
            if (stillMatches) {
                startCountdown(player, isHoldingSwordNow);
            }
        }, debounceTicks);

        activeTimers.put(uuid, debounceTask);
    }

    private void startCountdown(Player player, boolean enabling) {
        int seconds = plugin.getConfig().getInt(enabling ? "enable-cooldown" : "disable-cooldown", 3);

        if (seconds <= 0) {
            finish(player, enabling);
            return;
        }

        UUID uuid = player.getUniqueId();
        BukkitRunnable runnable = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeTimers.remove(uuid);
                    return;
                }

                if (remaining <= 0) {
                    finish(player, enabling);
                    cancel();
                    activeTimers.remove(uuid);
                    return;
                }

                String key = enabling ? "pvp-enabling" : "pvp-disabling";
                String text = messages.raw(key, "%time%", String.valueOf(remaining));
                String mode = plugin.getConfig().getString("countdown-message-display.mode", "chat");
                messages.displayByMode(player, text, mode);
                playCountdownSound(player);
                remaining--;
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
        activeTimers.put(uuid, task);
    }

    private void finish(Player player, boolean enabling) {
        boolean stillHoldingSword = itemManager.isWeapon(player.getInventory().getItemInMainHand());

        // If the player switched away again before the countdown ended, don't flip the state.
        if (enabling != stillHoldingSword) {
            return;
        }

        if (enabling) {
            playersInPvp.add(player.getUniqueId());
            messages.send(player, "pvp-enabled");
        } else {
            playersInPvp.remove(player.getUniqueId());
            messages.send(player, "pvp-disabled");
        }
    }

    private void playCountdownSound(Player player) {
        if (!plugin.getConfig().getBoolean("pvp-activation-deactivation-countdown-sound.enable", true)) return;
        String soundName = plugin.getConfig().getString("pvp-activation-deactivation-countdown-sound.sound", "BLOCK_WOODEN_BUTTON_CLICK_ON");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }
}
