package me.claude.lobbypvp;

import me.claude.lobbypvp.commands.LobbyPvPCommand;
import me.claude.lobbypvp.listeners.*;
import me.claude.lobbypvp.managers.ItemManager;
import me.claude.lobbypvp.managers.PvpStateManager;
import me.claude.lobbypvp.managers.WorldManager;
import me.claude.lobbypvp.utils.MessageUtil;
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

        // Give the kit to anyone already online (useful after a /reload of the server).
        for (Player player : getServer().getOnlinePlayers()) {
            if (worldManager.isActiveWorld(player.getWorld()) && !player.hasPermission("lobbypvp.bypass")) {
                itemManager.giveKit(player);
            }
        }

        getLogger().info("=======================================");
        getLogger().info(" LobbyPvP v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info(" Author: Smith (CEO-Asooner)");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            itemManager.takeKit(player);
        }
        getLogger().info("LobbyPvP has been disabled!");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(this, itemManager, worldManager), this);
        pm.registerEvents(new PlayerRespawnListener(this, itemManager, worldManager), this);
        pm.registerEvents(new PlayerQuitListener(pvpStateManager), this);
        pm.registerEvents(new PlayerTeleportListener(itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new ItemHeldListener(itemManager, worldManager, pvpStateManager), this);
        pm.registerEvents(new KitProtectionListener(itemManager, worldManager), this);
        pm.registerEvents(new CombatListener(pvpStateManager, worldManager), this);
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
