package us.asooner.smithpvp;

import us.asooner.smithpvp.commands.SmithPvPCommand;
import us.asooner.smithpvp.listeners.*;
import us.asooner.smithpvp.managers.ItemManager;
import us.asooner.smithpvp.managers.PvpStateManager;
import us.asooner.smithpvp.managers.WorldManager;
import us.asooner.smithpvp.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SmithPvPPlugin extends JavaPlugin {

    private ItemManager itemManager;
    private WorldManager worldManager;
    private PvpStateManager pvpStateManager;
    private MessageUtil messageUtil;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.messageUtil = new MessageUtil(getConfig());
        this.worldManager = new WorldManager(this);
        this.itemManager = new ItemManager(this, worldManager);
        this.pvpStateManager = new PvpStateManager(this, itemManager, worldManager, messageUtil);

        registerListeners();
        registerCommand();
        startKitWatchdog();

        // Give the kit to anyone already online (useful after a /reload of the server).
        for (Player player : getServer().getOnlinePlayers()) {
            if (worldManager.isActiveWorld(player.getWorld()) && !player.hasPermission("smithpvp.bypass")) {
                itemManager.giveKit(player);
            }
        }

        getLogger().info("=======================================");
        getLogger().info(" Smithpvp v" + getDescription().getVersion() + " has been ENABLED!");
        getLogger().info(" Author: Smith the CEO of Asooner");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (worldManager.isActiveWorld(player.getWorld())) itemManager.takeKit(player);
        }

        getLogger().info("=======================================");
        getLogger().info(" Smithpvp v" + getDescription().getVersion() + " has been DISABLED!");
        getLogger().info(" Author: Smith the CEO of Asooner");
        getLogger().info("=======================================");
    }

    private void startKitWatchdog() {
        if (!getConfig().getBoolean("general.kit-watchdog.enabled", true)) return;
        long interval = getConfig().getLong("general.kit-watchdog.interval-ticks", 40);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.hasPermission("smithpvp.bypass")) continue;
                if (!worldManager.isActiveWorld(player.getWorld())) continue;

                itemManager.ensureKitIntact(player, pvpStateManager.isInPvp(player));
            }
        }, interval, interval);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(this, itemManager, worldManager), this);
        pm.registerEvents(new PlayerRespawnListener(this, itemManager, worldManager), this);
        pm.registerEvents(new PlayerQuitListener(pvpStateManager), this);
        pm.registerEvents(new PlayerTeleportListener(this, itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new ItemHeldListener(itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new KitProtectionListener(itemManager, worldManager), this);
        pm.registerEvents(new CombatListener(this, pvpStateManager, worldManager), this);
        pm.registerEvents(new DeathListener(this, pvpStateManager, worldManager, itemManager, messageUtil), this);
        pm.registerEvents(new HungerListener(worldManager), this);
        pm.registerEvents(new AbilityListener(this, itemManager, worldManager, pvpStateManager, messageUtil), this);
    }

    private void registerCommand() {
        var command = getCommand("smithpvp");
        if (command != null) {
            command.setExecutor(new SmithPvPCommand(this, itemManager, worldManager, pvpStateManager, messageUtil));
        }
    }
}
