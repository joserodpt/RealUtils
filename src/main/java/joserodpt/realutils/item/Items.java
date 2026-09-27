package joserodpt.realutils.item;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Items for GUIs and messages, with names and lore coloured by {@link Text}. */
public final class Items {

    /**
     * Enchanted only to make an item glow. Looked up by key, which every version since 1.13 has,
     * where the field was renamed from LUCK to LUCK_OF_THE_SEA in 1.20.5.
     */
    private static final Enchantment GLOW = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));

    private static UnaryOperator<Material> materialMapper = m -> m;

    private Items() {
    }

    /**
     * Picks the item to show for a material that isn't one - a crop block, say - before the
     * built-in fallbacks. Return the material unchanged when there is nothing better.
     */
    public static void materialMapper(final UnaryOperator<Material> mapper) {
        materialMapper = mapper == null ? m -> m : mapper;
    }

    public static ItemStack createItem(final Material m, final int amount, final String name) {
        return createItem(m, amount, name, null);
    }

    /** @param lore null to leave the item without any */
    public static ItemStack createItem(final Material m, final int amount, final String name, final List<String> lore) {
        final ItemStack item = new ItemStack(asItem(m), amount);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(Text.color(name));
            }
            if (lore != null) {
                meta.setLore(lore(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** An item that glows, for whatever is currently selected. */
    public static ItemStack createItemLoreEnchanted(final Material m, final int amount, final String name, final List<String> lore) {
        final ItemStack item = createItem(m, amount, name, lore);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null && GLOW != null) {
            meta.addEnchant(GLOW, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createHead(final OfflinePlayer owner, final int amount, final String name, final List<String> lore) {
        final ItemStack item = createItem(Material.PLAYER_HEAD, amount, name, lore);
        final SkullMeta skull = (SkullMeta) item.getItemMeta();
        if (skull != null) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    /** Sets the name and lore of this item itself, and hands it back. */
    public static ItemStack changeItemStack(final String name, final List<String> lore, final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(lore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Like {@link #changeItemStack}, and also hides what a potion or tipped arrow would list. */
    public static ItemStack renameItem(final ItemStack item, final String name, final List<String> lore) {
        final ItemStack renamed = changeItemStack(name, lore, item);
        final ItemMeta meta = renamed.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            renamed.setItemMeta(meta);
        }
        return renamed;
    }

    /** A copy of the item with these lines added under its lore, after a blank one. */
    public static ItemStack addLore(final ItemStack item, final List<String> lines) {
        if (item == null) {
            return null;
        }
        final ItemStack copy = item.clone();
        final ItemMeta meta = copy.hasItemMeta() ? copy.getItemMeta() : Bukkit.getItemFactory().getItemMeta(copy.getType());
        if (meta == null) {
            return copy;
        }
        final List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§9");
        lore.addAll(lore(lines));
        meta.setLore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Lore lines coloured, each grey unless it opens with a colour of its own. Minecraft shows lore
     * in purple italics otherwise, and a line opening with only a format code such as {@code &l}
     * gets the grey in front of it, so it stays grey and bold.
     */
    private static List<String> lore(final List<String> lines) {
        final List<String> coloured = new ArrayList<>();
        for (final String line : Text.color(lines)) {
            coloured.add(line == null || line.isEmpty() || opensWithColour(line) ? line : "§7" + line);
        }
        return coloured;
    }

    /** Whether the codes a line opens with include a colour: 0-9, a-f, or a hex {@code §x}. */
    private static boolean opensWithColour(final String line) {
        for (int i = 0; i + 1 < line.length() && line.charAt(i) == '§'; i += 2) {
            if ("0123456789abcdefx".indexOf(Character.toLowerCase(line.charAt(i + 1))) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Every flag that hides tooltip detail, for GUI buttons. Read from the server so new ones apply too. */
    public static void hideAttributes(final ItemStack item) {
        final ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
    }

    /**
     * A material that can be held. Water and lava become their buckets, anything else that isn't
     * an item - a crop, a wall sign - becomes stone, since an ItemStack of it shows nothing.
     */
    private static Material asItem(final Material m) {
        final Material mapped = m == null ? Material.STONE : materialMapper.apply(m);
        if (mapped == Material.WATER) {
            return Material.WATER_BUCKET;
        }
        if (mapped == Material.LAVA) {
            return Material.LAVA_BUCKET;
        }
        return mapped.isItem() ? mapped : Material.STONE;
    }
}
