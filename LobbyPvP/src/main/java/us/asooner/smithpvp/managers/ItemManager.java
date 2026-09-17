package us.asooner.smithpvp.managers;

import us.asooner.smithpvp.SmithPvPPlugin;
import us.asooner.smithpvp.utils.AjParkourHook;
import us.asooner.smithpvp.utils.MessageUtil;
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

    public static final NamespacedKey KIT_TAG = new NamespacedKey("smithpvp", "kit_piece");

    private final SmithPvPPlugin plugin;
    private final AjParkourHook ajParkourHook;
    private final WorldManager worldManager;

    public ItemManager(SmithPvPPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.ajParkourHook = new AjParkourHook(plugin.getLogger());
    }

    /** True if we should leave this player's inventory alone right now (e.g. mid-parkour). */
    private boolean isManagedByAnotherPlugin(Player player) {
        return plugin.getConfig().getBoolean("compatibility.ajparkour.enabled", true)
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

            if (cfg.getBoolean("items.branding-lore.enabled", true)) {
                String loreLine = cfg.getString("items.branding-lore.text", "");
                if (!loreLine.isEmpty()) {
                    meta.setLore(java.util.List.of(MessageUtil.colorize(loreLine)));
                }
            }

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
        giveWeapon(player);
        // Armor belongs to the PvP state. Never equip it just because the player joined/respawned.
        if (!plugin.getConfig().getBoolean("pvp.armor.follows-pvp-state", true)) {
            giveArmor(player);
        }
    }

    /** Gives only the Smithpvp weapon, respecting the configured delay and destination world. */
    public void giveWeapon(Player player) {
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isManagedByAnotherPlugin(player)) return;

        Runnable apply = () -> {
            if (!player.isOnline() || !worldManager.isActiveWorld(player.getWorld()) || isManagedByAnotherPlugin(player)) return;
            PlayerInventory inv = player.getInventory();
            int weaponSlot = Math.max(0, Math.min(8, plugin.getConfig().getInt("items.weapon.slot", 4)));
            ItemStack current = inv.getItem(weaponSlot);
            // Never overwrite a normal player item.
            if (current == null || isWeapon(current)) inv.setItem(weaponSlot, buildWeapon());
        };

        if (plugin.getConfig().getBoolean("general.delayed-sword-give.enabled", true)) {
            long delay = Math.max(0L, plugin.getConfig().getLong("general.delayed-sword-give.delay-ticks", 10));
            plugin.getServer().getScheduler().runTaskLater(plugin, apply, delay);
        } else {
            apply.run();
        }
    }

    /** Equips just the armor pieces (used when re-enabling PvP without re-handing the sword). */
    public void giveArmor(Player player) {
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isManagedByAnotherPlugin(player)) return;

        var cfg = plugin.getConfig();
        PlayerInventory inv = player.getInventory();
        if (cfg.getBoolean("items.helmet.enabled", true)) inv.setHelmet(buildHelmet());
        if (cfg.getBoolean("items.chestplate.enabled", true)) inv.setChestplate(buildChestplate());
        if (cfg.getBoolean("items.leggings.enabled", true)) inv.setLeggings(buildLeggings());
        if (cfg.getBoolean("items.boots.enabled", true)) inv.setBoots(buildBoots());

        playArmorSound(player, "equip");
    }

    /** Strips just the armor pieces, leaving the sword where it is (used when disabling PvP). */
    public void takeArmor(Player player) {
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isManagedByAnotherPlugin(player)) return;

        PlayerInventory inv = player.getInventory();
        removeIfKitItem(inv, inv.getHelmet(), inv::setHelmet);
        removeIfKitItem(inv, inv.getChestplate(), inv::setChestplate);
        removeIfKitItem(inv, inv.getLeggings(), inv::setLeggings);
        removeIfKitItem(inv, inv.getBoots(), inv::setBoots);
        playArmorSound(player, "unequip");
    }

    private void playArmorSound(Player player, String action) {
        var cfg = plugin.getConfig();
        String base = "pvp.armor.sounds." + action;
        if (!cfg.getBoolean(base + ".enabled", true)) return;
        String soundName = cfg.getString(base + ".type", "ITEM_ARMOR_EQUIP_DIAMOND");
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1f, 1f);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid armor sound in config: " + soundName);
        }
    }

    public void takeKit(Player player) {
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isManagedByAnotherPlugin(player)) return;

        takeArmor(player);

        PlayerInventory inv = player.getInventory();
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

    // ---------------------------------------------------------------
    // Self-healing: quietly re-adds any kit piece that went missing
    // (e.g. a client/inventory desync, or another plugin briefly touching gear)
    // without ever touching pieces that are already there.
    // ---------------------------------------------------------------

    public void ensureKitIntact(Player player, boolean shouldHaveArmor) {
        if (!worldManager.isActiveWorld(player.getWorld())) return;
        if (isManagedByAnotherPlugin(player)) return;

        var cfg = plugin.getConfig();
        PlayerInventory inv = player.getInventory();

        if (shouldHaveArmor) {
            if (cfg.getBoolean("items.helmet.enabled", true) && inv.getHelmet() == null) {
                inv.setHelmet(buildHelmet());
            }
            if (cfg.getBoolean("items.chestplate.enabled", true) && inv.getChestplate() == null) {
                inv.setChestplate(buildChestplate());
            }
            if (cfg.getBoolean("items.leggings.enabled", true) && inv.getLeggings() == null) {
                inv.setLeggings(buildLeggings());
            }
            if (cfg.getBoolean("items.boots.enabled", true) && inv.getBoots() == null) {
                inv.setBoots(buildBoots());
            }
        }

        int weaponSlot = Math.max(0, Math.min(8, cfg.getInt("items.weapon.slot", 4)));
        if (inv.getItem(weaponSlot) == null) {
            inv.setItem(weaponSlot, buildWeapon());
        }
    }
}
