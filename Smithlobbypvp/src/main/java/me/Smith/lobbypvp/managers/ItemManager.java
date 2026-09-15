package me.claude.lobbypvp.managers;

import me.claude.lobbypvp.LobbyPvPPlugin;
import me.claude.lobbypvp.utils.AjParkourHook;
import me.claude.lobbypvp.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Builds every piece of the PvP kit straight from config.yml and knows how
 * to give/take the whole set, and how to recognise a kit item later on
 * (drop protection, slot-change detection, etc.) via a PersistentDataContainer tag.
 */
public class ItemManager {

    public static final NamespacedKey KIT_TAG = new NamespacedKey("lobbypvp", "kit_piece");

    private final LobbyPvPPlugin plugin;
    private final AjParkourHook ajParkourHook;

    public ItemManager(LobbyPvPPlugin plugin) {
        this.plugin = plugin;
        this.ajParkourHook = new AjParkourHook(plugin.getLogger());
    }

    /** True if we should leave this player's inventory alone right now (e.g. mid-parkour). */
    private boolean isManagedByAnotherPlugin(Player player) {
        return plugin.getConfig().getBoolean("ajparkour-compatibility.enable", true)
                && ajParkourHook.isInParkour(player);
    }

    // ---------------------------------------------------------------
    // Building items
    // ---------------------------------------------------------------

    private ItemStack build(String path, String tag) {
        var cfg = plugin.getConfig();
        String materialName = cfg.getString("items." + path + ".material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + materialName + "' for items." + path + ", defaulting to STONE.");
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = cfg.getString("items." + path + ".name", "");
            if (!name.isEmpty()) {
                meta.setDisplayName(MessageUtil.colorize(name));
            }
            meta.setUnbreakable(cfg.getBoolean("items." + path + ".unbreakable", true));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(KIT_TAG, PersistentDataType.STRING, tag);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack buildWeapon() {
        ItemStack sword = build("weapon", "weapon");
        int sharpness = plugin.getConfig().getInt("items.weapon.sharpness", 0);
        if (sharpness > 0) {
            sword.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
        }
        return sword;
    }

    private ItemStack buildArmorPiece(String path, String tag, Enchantment protectionEnchant) {
        ItemStack piece = build(path, tag);
        int protection = plugin.getConfig().getInt("items." + path + ".protection", 0);
        if (protection > 0) {
            piece.addUnsafeEnchantment(protectionEnchant, protection);
        }
        return piece;
    }

    public ItemStack buildHelmet() { return buildArmorPiece("helmet", "helmet", Enchantment.PROTECTION); }
    public ItemStack buildChestplate() { return buildArmorPiece("chestplate", "chestplate", Enchantment.PROTECTION); }
    public ItemStack buildLeggings() { return buildArmorPiece("leggings", "leggings", Enchantment.PROTECTION); }
    public ItemStack buildBoots() { return buildArmorPiece("boots", "boots", Enchantment.PROTECTION); }

    public Material getWeaponMaterial() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("items.weapon.material", "DIAMOND_SWORD"));
        return m != null ? m : Material.DIAMOND_SWORD;
    }

    // ---------------------------------------------------------------
    // Recognising kit items
    // ---------------------------------------------------------------

    public boolean isKitItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(KIT_TAG, PersistentDataType.STRING);
    }

    public boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && "weapon".equals(meta.getPersistentDataContainer().get(KIT_TAG, PersistentDataType.STRING));
    }

    // ---------------------------------------------------------------
    // Giving / taking the whole set
    // ---------------------------------------------------------------

    public void giveKit(Player player) {
        if (isManagedByAnotherPlugin(player)) return;

        var cfg = plugin.getConfig();
        Runnable apply = () -> {
            if (!player.isOnline() || isManagedByAnotherPlugin(player)) return;
            PlayerInventory inv = player.getInventory();
            inv.setItem(cfg.getInt("items.weapon.slot", 0), buildWeapon());

            if (cfg.getBoolean("items.helmet.enable", true)) inv.setHelmet(buildHelmet());
            if (cfg.getBoolean("items.chestplate.enable", true)) inv.setChestplate(buildChestplate());
            if (cfg.getBoolean("items.leggings.enable", true)) inv.setLeggings(buildLeggings());
            if (cfg.getBoolean("items.boots.enable", true)) inv.setBoots(buildBoots());
        };

        if (cfg.getBoolean("delay-before-giving-sword", false)) {
            long delay = cfg.getLong("give-sword-delay-ticks", 10);
            plugin.getServer().getScheduler().runTaskLater(plugin, apply, delay);
        } else {
            apply.run();
        }
    }

    public void takeKit(Player player) {
        if (isManagedByAnotherPlugin(player)) return;

        PlayerInventory inv = player.getInventory();
        removeIfKitItem(inv, inv.getHelmet(), inv::setHelmet);
        removeIfKitItem(inv, inv.getChestplate(), inv::setChestplate);
        removeIfKitItem(inv, inv.getLeggings(), inv::setLeggings);
        removeIfKitItem(inv, inv.getBoots(), inv::setBoots);

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isKitItem(contents[i])) {
                inv.setItem(i, null);
            }
        }
    }

    private void removeIfKitItem(PlayerInventory inv, ItemStack current, java.util.function.Consumer<ItemStack> setter) {
        if (isKitItem(current)) {
            setter.accept(null);
        }
    }
}
