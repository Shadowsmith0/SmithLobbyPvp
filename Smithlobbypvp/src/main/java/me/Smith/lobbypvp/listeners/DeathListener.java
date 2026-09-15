package me.claude.lobbypvp.listeners;

import me.claude.lobbypvp.LobbyPvPPlugin;
import me.claude.lobbypvp.managers.ItemManager;
import me.claude.lobbypvp.managers.PvpStateManager;
import me.claude.lobbypvp.managers.WorldManager;
import me.claude.lobbypvp.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeathListener implements Listener {

    private final LobbyPvPPlugin plugin;
    private final PvpStateManager pvpStateManager;
    private final WorldManager worldManager;
    private final ItemManager itemManager;
    private final MessageUtil messages;

    public DeathListener(LobbyPvPPlugin plugin, PvpStateManager pvpStateManager, WorldManager worldManager,
                          ItemManager itemManager, MessageUtil messages) {
        this.plugin = plugin;
        this.pvpStateManager = pvpStateManager;
        this.worldManager = worldManager;
        this.itemManager = itemManager;
        this.messages = messages;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!worldManager.isActiveWorld(victim.getWorld())) return;
        if (!pvpStateManager.isInPvp(victim)) return;

        var cfg = plugin.getConfig();

        if (cfg.getBoolean("clear-drops-on-death", true)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // Blank the vanilla death message; we send our own (or none, if disabled).
        event.setDeathMessage(null);

        Player killer = victim.getKiller();
        if (killer != null) {
            String msg = messages.raw("kill-message", "%victim%", victim.getName(), "%killer%", killer.getName());
            if (!msg.isEmpty()) {
                plugin.getServer().broadcastMessage(msg);
            }

            applyKillEffects(killer);
        }

        if (cfg.getBoolean("lightning-effect-on-kill.enable", true)) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
        }

        pvpStateManager.removeFromPvp(victim);
        itemManager.takeKit(victim);
    }

    private void applyKillEffects(Player killer) {
        var cfg = plugin.getConfig();

        if (cfg.getBoolean("speed-strength-on-kill.enable", true)) {
            int speedDuration = cfg.getInt("right-click-ability.speed.duration", 5) * 20;
            int speedAmplifier = cfg.getInt("right-click-ability.speed.amplifier", 0);
            int strengthDuration = cfg.getInt("right-click-ability.strength.duration", 5) * 20;
            int strengthAmplifier = cfg.getInt("right-click-ability.strength.amplifier", 0);

            killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, speedAmplifier, false, true));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthDuration, strengthAmplifier, false, true));
        }

        if (cfg.getBoolean("regeneration-on-kill.enable", true)) {
            int duration = cfg.getInt("regeneration-on-kill.duration", 5) * 20;
            int amplifier = cfg.getInt("regeneration-on-kill.amplifier", 2);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier, false, true));
        }
    }
}
