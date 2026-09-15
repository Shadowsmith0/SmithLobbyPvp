package us.asooner.lobbypvp.listeners;

import us.asooner.lobbypvp.LobbyPvPPlugin;
import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.PvpStateManager;
import us.asooner.lobbypvp.managers.WorldManager;
import us.asooner.lobbypvp.utils.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityListener implements Listener {

    private final LobbyPvPPlugin plugin;
    private final ItemManager itemManager;
    private final WorldManager worldManager;
    private final PvpStateManager pvpStateManager;
    private final MessageUtil messages;

    private final Map<UUID, Long> cooldownEnds = new HashMap<>();
    private final Map<UUID, BukkitTask> displayTasks = new HashMap<>();

    public AbilityListener(LobbyPvPPlugin plugin, ItemManager itemManager, WorldManager worldManager,
                            PvpStateManager pvpStateManager, MessageUtil messages) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.worldManager = worldManager;
        this.pvpStateManager = pvpStateManager;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("right-click-ability.enable", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (!itemManager.isWeapon(player.getInventory().getItemInMainHand())) return;
        if (!pvpStateManager.isInPvp(player)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long cooldownEnd = cooldownEnds.get(uuid);

        if (cooldownEnd != null && cooldownEnd > now) {
            long secondsLeft = (long) Math.ceil((cooldownEnd - now) / 1000.0);
            messages.send(player, "ability-on-cooldown", "%time%", String.valueOf(secondsLeft));
            return;
        }

        activateAbility(player);
    }

    private void activateAbility(Player player) {
        var cfg = plugin.getConfig();

        int speedDuration = cfg.getInt("right-click-ability.speed.duration", 5) * 20;
        int speedAmplifier = cfg.getInt("right-click-ability.speed.amplifier", 0);
        int strengthDuration = cfg.getInt("right-click-ability.strength.duration", 5) * 20;
        int strengthAmplifier = cfg.getInt("right-click-ability.strength.amplifier", 0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, speedAmplifier, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthDuration, strengthAmplifier, false, true));
        messages.send(player, "ability-activate");

        int cooldownSeconds = cfg.getInt("right-click-ability.cooldown", 15);
        UUID uuid = player.getUniqueId();
        cooldownEnds.put(uuid, System.currentTimeMillis() + cooldownSeconds * 1000L);

        if (cfg.getBoolean("right-click-ability.cooldown-display.enable", true)) {
            startCooldownDisplay(player, cooldownSeconds);
        }
    }

    private void startCooldownDisplay(Player player, int totalCooldownSeconds) {
        UUID uuid = player.getUniqueId();
        BukkitTask existing = displayTasks.remove(uuid);
        if (existing != null) existing.cancel();

        boolean useXpBar = "xp-bar".equalsIgnoreCase(
                plugin.getConfig().getString("right-click-ability.cooldown-display.mode", "actionbar"));

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Long cooldownEnd = cooldownEnds.get(uuid);
                if (cooldownEnd == null || !player.isOnline()) {
                    restoreExp(player, useXpBar);
                    cancel();
                    displayTasks.remove(uuid);
                    return;
                }

                long millisLeft = cooldownEnd - System.currentTimeMillis();
                if (millisLeft <= 0) {
                    restoreExp(player, useXpBar);
                    playReadySound(player);
                    cancel();
                    displayTasks.remove(uuid);
                    return;
                }

                long secondsLeft = (long) Math.ceil(millisLeft / 1000.0);

                if (useXpBar) {
                    float progress = 1f - Math.min(1f, Math.max(0f,
                            (float) millisLeft / (totalCooldownSeconds * 1000f)));
                    player.setExp(progress);
                    player.setLevel((int) secondsLeft);
                } else {
                    messages.actionBar(player, buildCooldownText(secondsLeft));
                }
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 5L);
        displayTasks.put(uuid, task);
    }

    private String buildCooldownText(long secondsLeft) {
        return "&eAbility ready in &6" + secondsLeft + "s";
    }

    private void restoreExp(Player player, boolean wasUsingXpBar) {
        if (wasUsingXpBar && player.isOnline()) {
            player.setExp(0f);
            player.setLevel(0);
        }
    }

    private void playReadySound(Player player) {
        if (!player.isOnline()) return;
        String soundName = plugin.getConfig().getString("right-click-ability.cooldown-display.ready-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid ready-sound in config: " + soundName);
        }
    }
}
