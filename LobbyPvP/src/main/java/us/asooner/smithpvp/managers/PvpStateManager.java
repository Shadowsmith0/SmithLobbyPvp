package us.asooner.smithpvp.managers;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.utils.MessageUtil;
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

    private final SmithPvPPlugin plugin;
    private final ItemManager itemManager;
    private final MessageUtil messages;
    private final WorldManager worldManager;

    private final Set<UUID> playersInPvp = new HashSet<>();
    private final Map<UUID, BukkitTask> activeTimers = new HashMap<>();

    public PvpStateManager(SmithPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager, MessageUtil messages) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
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
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isHoldingSwordNow && isInPvp(player)) return;
        if (!isHoldingSwordNow && !isInPvp(player)) { cancelActiveTimer(player.getUniqueId()); return; }

        UUID uuid = player.getUniqueId();
        cancelActiveTimer(uuid);

        int debounceTicks = plugin.getConfig().getInt("held-item-debounce-ticks", 4);
        if (debounceTicks <= 0) {
            startCountdown(player, isHoldingSwordNow);
            return;
        }

        BukkitTask debounceTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !worldManager.isActiveWorld(player.getWorld())) return;
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
                if (!player.isOnline() || !worldManager.isActiveWorld(player.getWorld())) {
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
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (enabling != stillHoldingSword) {
            return;
        }

        boolean armorFollowsToggle = plugin.getConfig().getBoolean("armor-follows-pvp-toggle", true);

        if (enabling) {
            playersInPvp.add(player.getUniqueId());
            if (armorFollowsToggle) itemManager.giveArmor(player);
            sendStateMessage(player, "pvp-enabled");
        } else {
            playersInPvp.remove(player.getUniqueId());
            if (armorFollowsToggle) itemManager.takeArmor(player);
            heal(player);
            sendStateMessage(player, "pvp-disabled");
        }
    }

    private void heal(Player player) {
        var max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (max != null) player.setHealth(max.getValue());
    }

    private void sendStateMessage(Player player, String key) {
        String text = messages.raw(key);
        String mode = plugin.getConfig().getString("state-message-display.mode", "actionbar");
        messages.displayByMode(player, text, mode);
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
