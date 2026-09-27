package joserodpt.realutils.gui;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.RealUtils;
import joserodpt.realutils.input.PlayerInput;
import joserodpt.realutils.item.Items;
import joserodpt.realutils.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A searchable, paged chest of materials to pick one from. The player's pick - or null, if they
 * close it - reaches the callback a few ticks after the chest closes.
 *
 * <p>Its words are the plugin's own, from {@link #labels}. {@link RealUtils#setup} registers
 * {@link #getListener()}.</p>
 */
public class MaterialPickerGUI {

    public enum MaterialLists {
        ALL_MATERIALS, ONLY_ITEMS, ONLY_BLOCKS;

        public List<Material> materials() {
            switch (this) {
                case ONLY_ITEMS:
                    return Arrays.stream(Material.values()).filter(m -> m != Material.AIR && m.isItem()).toList();
                case ONLY_BLOCKS:
                    return Arrays.stream(Material.values()).filter(m -> m != Material.AIR && m.isSolid() && m.isBlock() && m.isItem()).toList();
                default:
                    return Arrays.stream(Material.values()).filter(m -> m != Material.AIR && m.isItem() && m.isBlock()).toList();
            }
        }
    }

    private static final int PAGE_SIZE = 28;
    private static final int SEARCH = 4;
    private static final int CLOSE = 49;
    private static final int[] BORDER = {45, 46, 47, 48, 49, 50, 51, 52, 53, 36, 44, 9, 17};

    private static final Map<UUID, MaterialPickerGUI> inventories = new HashMap<>();
    private static Supplier<Labels> labels = Labels::new;

    public static final ItemStack placeholder = Items.createItem(Material.BLACK_STAINED_GLASS_PANE, 1, "");

    private final UUID uuid;
    private final Labels words;
    private final Map<Integer, Material> display = new HashMap<>();
    private final List<Material> allowed;
    private final Pagination<Material> pages;
    private final Inventory inv;
    private final String title;
    private final MaterialRunnable materialRunnable;
    private int pageNumber = 0;

    /** Where the words come from, read for every picker opened so a reloaded language applies. */
    public static void labels(final Supplier<Labels> supplier) {
        labels = supplier == null ? Labels::new : supplier;
    }

    public MaterialPickerGUI(final Player pl, final String title, final MaterialLists list, final MaterialRunnable materialRunnable) {
        this(pl, title, list.materials(), materialRunnable);
    }

    public MaterialPickerGUI(final Player pl, final String title, final List<Material> allowed, final MaterialRunnable materialRunnable) {
        this(pl, title, allowed, materialRunnable, null);
    }

    /** @param search only the materials whose name contains this, or null for all of them */
    public MaterialPickerGUI(final Player pl, final String title, final List<Material> allowed, final MaterialRunnable materialRunnable, final String search) {
        this.uuid = pl.getUniqueId();
        this.title = title;
        this.materialRunnable = materialRunnable;
        this.allowed = allowed;
        this.words = labels.get();
        this.inv = Bukkit.getServer().createInventory(null, 54, Text.color(title));

        final String needle = search == null ? null : search.toLowerCase();
        this.pages = new Pagination<>(PAGE_SIZE, needle == null ? allowed
                : allowed.stream().filter(m -> m.name().toLowerCase().contains(needle)).toList());
        //an empty search result has no pages at all
        this.fillChest(this.pages.exists(0) ? this.pages.getPage(0) : Collections.emptyList());
        this.register();
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClick(final InventoryClickEvent e) {
                if (!(e.getWhoClicked() instanceof Player)) {
                    return;
                }
                final MaterialPickerGUI current = inventories.get(e.getWhoClicked().getUniqueId());
                if (current == null || !current.inv.equals(e.getInventory())) {
                    return;
                }

                e.setCancelled(true);
                if (e.getCurrentItem() == null) {
                    return;
                }
                final Player p = (Player) e.getWhoClicked();

                switch (e.getRawSlot()) {
                    case SEARCH:
                        new PlayerInput(p, true, input -> new MaterialPickerGUI(p, current.title, current.allowed,
                                current.materialRunnable, input).openInventory(p), input -> current.exit(p));
                        return;
                    case CLOSE:
                        current.exit(p);
                        return;
                    case 26:
                    case 35:
                        current.turn(p, 1);
                        return;
                    case 18:
                    case 27:
                        current.turn(p, -1);
                        return;
                    default:
                        break;
                }

                final Material picked = current.display.get(e.getRawSlot());
                if (picked != null) {
                    //closed a tick later: Bukkit doesn't support closing the inventory from inside its own click event
                    Bukkit.getScheduler().runTask(RealUtils.plugin(), () -> p.closeInventory());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(RealUtils.plugin(), () -> current.materialRunnable.selectedMaterial(picked), 3);
                }
            }

            @EventHandler
            public void onDrag(final InventoryDragEvent e) {
                final MaterialPickerGUI current = inventories.get(e.getWhoClicked().getUniqueId());
                //dragging over this GUI's slots would drop the dragged items into it
                if (current != null && current.inv.equals(e.getInventory())) {
                    e.setCancelled(true);
                }
            }

            @EventHandler
            public void onClose(final InventoryCloseEvent e) {
                final MaterialPickerGUI current = inventories.get(e.getPlayer().getUniqueId());
                if (current != null && current.inv.equals(e.getInventory())) {
                    inventories.remove(current.uuid);
                }
            }
        };
    }

    /** Closes every picker, for a reload: their callbacks hold objects that are about to be replaced. */
    public static void closeAll() {
        for (final MaterialPickerGUI picker : inventories.values().toArray(new MaterialPickerGUI[0])) {
            final Player p = Bukkit.getPlayer(picker.uuid);
            if (p != null && picker.inv.equals(p.getOpenInventory().getTopInventory())) {
                p.closeInventory();
            }
        }
        inventories.clear();
    }

    private void turn(final Player p, final int by) {
        if (this.pages.exists(this.pageNumber + by)) {
            this.pageNumber += by;
            this.fillChest(this.pages.getPage(this.pageNumber));
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 50, 50);
        }
    }

    private void fillChest(final List<Material> items) {
        this.inv.clear();
        this.display.clear();

        for (int i = 0; i < 9; ++i) {
            this.inv.setItem(i, placeholder);
        }
        for (final int slot : BORDER) {
            this.inv.setItem(slot, placeholder);
        }
        this.inv.setItem(SEARCH, Items.createItem(Material.OAK_SIGN, 1, this.words.searchName, this.words.searchLore));
        final ItemStack back = Items.createItem(Material.YELLOW_STAINED_GLASS, 1, this.words.previousName, this.words.previousLore);
        final ItemStack next = Items.createItem(Material.GREEN_STAINED_GLASS, 1, this.words.nextName, this.words.nextLore);
        this.inv.setItem(18, back);
        this.inv.setItem(27, back);
        this.inv.setItem(26, next);
        this.inv.setItem(35, next);

        int taken = 0;
        for (int slot = 0; slot < this.inv.getSize() && taken < items.size(); ++slot) {
            if (this.inv.getItem(slot) == null) {
                final Material m = items.get(taken++);
                this.inv.setItem(slot, Items.createItem(m, 1, this.words.pickName.apply(m), this.words.pickLore));
                this.display.put(slot, m);
            }
        }

        this.inv.setItem(CLOSE, Items.createItem(Material.ACACIA_DOOR, 1, this.words.closeName, this.words.closeLore));
    }

    public void openInventory(final Player target) {
        if (!this.inv.equals(target.getOpenInventory().getTopInventory())) {
            target.openInventory(this.inv);
        }
    }

    private void exit(final Player p) {
        //a tick later, since this is called from the click event
        Bukkit.getScheduler().runTask(RealUtils.plugin(), () -> p.closeInventory());
        Bukkit.getScheduler().scheduleSyncDelayedTask(RealUtils.plugin(), () -> this.materialRunnable.selectedMaterial(null), 3);
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private void register() {
        inventories.put(this.uuid, this);
    }

    @FunctionalInterface
    public interface MaterialRunnable {
        void selectedMaterial(Material m);
    }

    /** The picker's words, in English unless the plugin sets its own through {@link #labels}. */
    public static final class Labels {
        public String nextName = "&aNext";
        public List<String> nextLore = Collections.emptyList();
        public String previousName = "&eBack";
        public List<String> previousLore = Collections.emptyList();
        public String closeName = "&cClose";
        public List<String> closeLore = Collections.emptyList();
        public String searchName = "&9Search";
        public List<String> searchLore = Collections.emptyList();
        public Function<Material, String> pickName = m -> "&f" + Text.beautifyMaterialName(m);
        public List<String> pickLore = Collections.emptyList();
    }
}
