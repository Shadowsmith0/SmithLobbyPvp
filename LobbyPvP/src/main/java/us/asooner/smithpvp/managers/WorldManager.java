package us.asooner.smithpvp.managers;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class WorldManager {
    private final Plugin plugin;
    public WorldManager(Plugin plugin) { this.plugin = plugin; }

    public boolean isActiveWorld(World world) {
        if (world == null) return false;
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.getBoolean("worlds.whitelist-mode", false)) {
            return cfg.getStringList("worlds.enabled-worlds").contains(world.getName());
        }
        return !cfg.getStringList("worlds.disabled-worlds").contains(world.getName());
    }
}
