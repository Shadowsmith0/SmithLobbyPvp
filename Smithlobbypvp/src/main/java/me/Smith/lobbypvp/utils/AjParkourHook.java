package me.claude.lobbypvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Soft-integration with the ajParkour plugin (https://wiki.ajg0702.us/ajParkour/).
 * We never compile against ajParkour's jar - instead we reach for its public
 * "us.ajg0702.parkour.game.Manager#getInstance()/inParkour(Player)" API via
 * reflection only if the plugin is actually installed and enabled. If it's not
 * present (or anything about the reflection fails), we simply behave as if no
 * player is ever "in parkour", so nothing changes for servers that don't use it.
 */
public class AjParkourHook {

    private static final String PLUGIN_NAME = "ajParkour";
    private static final String MANAGER_CLASS = "us.ajg0702.parkour.game.Manager";

    private final Logger logger;
    private boolean resolved = false;
    private boolean available = false;
    private Method getInstanceMethod;
    private Method inParkourMethod;

    public AjParkourHook(Logger logger) {
        this.logger = logger;
    }

    private void resolve() {
        if (resolved) return;
        resolved = true;

        if (Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) == null
                || !Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        try {
            Class<?> managerClass = Class.forName(MANAGER_CLASS);
            getInstanceMethod = managerClass.getMethod("getInstance");
            inParkourMethod = managerClass.getMethod("inParkour", Player.class);
            available = true;
        } catch (ReflectiveOperationException ex) {
            logger.log(Level.WARNING, "Detected ajParkour but couldn't hook into its API (maybe an unsupported version). " +
                    "LobbyPvP will not pause kit-giving during parkour sessions.", ex);
            available = false;
        }
    }

    /** True if the player is currently inside an active ajParkour session. */
    public boolean isInParkour(Player player) {
        resolve();
        if (!available) return false;

        try {
            Object manager = getInstanceMethod.invoke(null);
            Object result = inParkourMethod.invoke(manager, player);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
