package us.asooner.lobbypvp.managers;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Decides whether LobbyPvP should be active in a given world, based on
 * either a blacklist ("disabled-worlds") or, if enabled, a strict whitelist.
 */
public class WorldManager {

    private final Plugin plugin;

    public WorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActiveWorld(World world) {
        if (world == null) return false;
        FileConfiguration cfg = plugin.getConfig();

        boolean whitelistMode = cfg.getBoolean("worlds-white-list-mode.enable", false);
        if (whitelistMode) {
            return cfg.getStringList("worlds-white-list-mode.white-list-worlds").contains(world.getName());
        }

        return !cfg.getStringList("disabled-worlds").contains(world.getName());
    }
}
