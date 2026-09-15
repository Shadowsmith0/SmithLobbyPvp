package us.asooner.lobbypvp.listeners;

import us.asooner.lobbypvp.LobbyPvPPlugin;
import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.PvpStateManager;
import us.asooner.lobbypvp.managers.WorldManager;
import us.asooner.lobbypvp.utils.MessageUtil;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathListener implements Listener {

    private final LobbyPvPPlugin plugin;
    private final PvpStateManager pvpStateManager;
    private final WorldManager worldManager;
    private final ItemManager itemManager;
    private final MessageUtil messages;

    // Simple in-memory kill counter for this server session (resets on restart).
    private final Map<UUID, Integer> sessionKills = new HashMap<>();

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
            int kills = sessionKills.merge(killer.getUniqueId(), 1, Integer::sum);
            String msg = messages.raw("kill-message",
                    "%victim%", victim.getName(),
                    "%killer%", killer.getName(),
                    "%kills%", String.valueOf(kills));
            if (!msg.isEmpty()) {
                plugin.getServer().broadcastMessage(msg);
            }

            applyKillEffects(killer);
        }

        if (cfg.getBoolean("lightning-effect-on-kill.enable", true)) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
        }

        spawnKillParticles(victim);

        pvpStateManager.removeFromPvp(victim);
        itemManager.takeKit(victim);
    }

    private void spawnKillParticles(Player victim) {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("kill-particles.enable", true)) return;

        String particleName = cfg.getString("kill-particles.particle", "FLAME");
        int count = cfg.getInt("kill-particles.count", 30);
        try {
            Particle particle = Particle.valueOf(particleName);
            victim.getWorld().spawnParticle(particle, victim.getLocation().add(0, 1, 0), count, 0.4, 0.6, 0.4, 0.02);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid kill-particles.particle in config: " + particleName);
        }
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
