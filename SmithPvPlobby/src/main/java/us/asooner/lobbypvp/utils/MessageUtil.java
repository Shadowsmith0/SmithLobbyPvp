package us.asooner.lobbypvp.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Small helper around the "lang" section of the config: reads a message,
 * colourizes it, fills in placeholders and sends it (or skips it entirely
 * if the admin left the value blank).
 */
public class MessageUtil {

    private final FileConfiguration config;

    public MessageUtil(FileConfiguration config) {
        this.config = config;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String prefix() {
        return colorize(config.getString("lang.prefix", ""));
    }

    /** Raw (colourized) message for a lang key, without the prefix, "" if disabled. */
    public String raw(String key) {
        String value = config.getString("lang." + key, "");
        if (value == null || value.isEmpty()) return "";
        return colorize(value);
    }

    /** Same as {@link #raw(String)} but replaces %placeholder% tokens first. */
    public String raw(String key, String... placeholdersAndValues) {
        String value = config.getString("lang." + key, "");
        if (value == null || value.isEmpty()) return "";
        for (int i = 0; i + 1 < placeholdersAndValues.length; i += 2) {
            value = value.replace(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        return colorize(value);
    }

    /** Sends the message (with the configured prefix) unless the value is blank. */
    public void send(CommandSender target, String key, String... placeholdersAndValues) {
        String msg = raw(key, placeholdersAndValues);
        if (msg.isEmpty()) return;
        target.sendMessage(prefix() + msg);
    }

    /** Sends the message WITHOUT the prefix (useful for broadcast/death messages). */
    public void sendNoPrefix(CommandSender target, String key, String... placeholdersAndValues) {
        String msg = raw(key, placeholdersAndValues);
        if (msg.isEmpty()) return;
        target.sendMessage(msg);
    }

    public void actionBar(Player player, String text) {
        if (text == null || text.isEmpty()) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(colorize(text)));
    }

    /**
     * Sends an already-colourized message through chat, the action bar, or as a title,
     * depending on the given mode ("chat" / "actionbar" / "title"). Falls back to chat
     * for any unrecognised value.
     */
    public void displayByMode(Player player, String coloredText, String mode) {
        if (coloredText == null || coloredText.isEmpty()) return;

        String normalized = mode == null ? "chat" : mode.toLowerCase();
        switch (normalized) {
            case "actionbar" -> actionBar(player, coloredText);
            case "title" -> player.sendTitle(coloredText, "", 0, 30, 5);
            default -> player.sendMessage(prefix() + coloredText);
        }
    }
}
