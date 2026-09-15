package us.asooner.lobbypvp;

import us.asooner.lobbypvp.commands.LobbyPvPCommand;
import us.asooner.lobbypvp.listeners.*;
import us.asooner.lobbypvp.managers.ItemManager;
import us.asooner.lobbypvp.managers.PvpStateManager;
import us.asooner.lobbypvp.managers.WorldManager;
import us.asooner.lobbypvp.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LobbyPvPPlugin extends JavaPlugin {

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
        this.itemManager = new ItemManager(this);
        this.worldManager = new WorldManager(this);
        this.pvpStateManager = new PvpStateManager(this, itemManager, messageUtil);

        registerListeners();
        registerCommand();
        startKitWatchdog();

        // Give the kit to anyone already online (useful after a /reload of the server).
        for (Player player : getServer().getOnlinePlayers()) {
            if (worldManager.isActiveWorld(player.getWorld()) && !player.hasPermission("lobbypvp.bypass")) {
                itemManager.giveKit(player);
            }
        }

        getLogger().info("=======================================");
        getLogger().info(" LobbyPvP v" + getDescription().getVersion() + " has been ENABLED!");
        getLogger().info(" Author: Smith the CEO of Asooner");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            itemManager.takeKit(player);
        }

        getLogger().info("=======================================");
        getLogger().info(" LobbyPvP v" + getDescription().getVersion() + " has been DISABLED!");
        getLogger().info(" Author: Smith the CEO of Asooner");
        getLogger().info("=======================================");
    }

    private void startKitWatchdog() {
        if (!getConfig().getBoolean("kit-watchdog.enable", true)) return;
        long interval = getConfig().getLong("kit-watchdog.interval-ticks", 40);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.hasPermission("lobbypvp.bypass")) continue;
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
        pm.registerEvents(new PlayerTeleportListener(itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new ItemHeldListener(itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new KitProtectionListener(itemManager, worldManager), this);
        pm.registerEvents(new CombatListener(this, pvpStateManager, worldManager), this);
        pm.registerEvents(new DeathListener(this, pvpStateManager, worldManager, itemManager, messageUtil), this);
        pm.registerEvents(new HungerListener(worldManager), this);
        pm.registerEvents(new AbilityListener(this, itemManager, worldManager, pvpStateManager, messageUtil), this);
    }

    private void registerCommand() {
        var command = getCommand("lobbypvp");
        if (command != null) {
            command.setExecutor(new LobbyPvPCommand(this, itemManager, worldManager, pvpStateManager, messageUtil));
        }
    }
}
