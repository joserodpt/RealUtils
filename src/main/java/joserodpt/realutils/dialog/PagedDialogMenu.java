package joserodpt.realutils.dialog;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.gui.Pagination;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link DialogMenu} of a list too long for one: a page of it at a time, with previous and next
 * buttons and the page it is on, {@code (2/5)}, after the title.
 *
 * <pre>{@code
 * new PagedDialogMenu<>("Plugins", "Click one to see its permissions.", plugins)
 *         .button("&eSearch", null, this::askSearch)
 *         .entries(ExternalPlugin::getDisplayName, null, (plugin, page) -> openPermissions(plugin))
 *         .open(p, 0, this::back, this::openChest);
 * }</pre>
 *
 * <p>Buttons added with {@link #button} are on every page, before the entries, and each takes a
 * place an entry would have had: a page holds {@link DialogMenu#MAX_OPTIONS} buttons in all, two of
 * them always kept for the page turns. The list is read once, when the menu is made, so a click that
 * changes it should build a new menu, opened at the page it hands over.</p>
 */
public final class PagedDialogMenu<T> {

    /** What an entry does when clicked, told which page it was on so the list can be reopened there. */
    @FunctionalInterface
    public interface EntryAction<T> {
        void chosen(T entry, int page);
    }

    private static final class Button {
        final String label;
        final String tooltip;
        final Runnable chosen;

        private Button(final String label, final String tooltip, final Runnable chosen) {
            this.label = label;
            this.tooltip = tooltip;
            this.chosen = chosen;
        }
    }

    private final String title;
    private final String description;
    private final List<T> entries;
    private final List<Button> buttons = new ArrayList<>();
    private Function<T, String> label = String::valueOf;
    private Function<T, String> tooltip = entry -> null;
    private EntryAction<T> chosen = (entry, page) -> { };
    private String close;
    private String previous;
    private String next;
    private int columns = 1;
    private Material icon;

    /** @param entries copied, so the menu keeps paging through the list as it was */
    public PagedDialogMenu(final String title, final String description, final List<T> entries) {
        this.title = title;
        this.description = description;
        this.entries = new ArrayList<>(entries);
    }

    /** How many entries fit on a page beside this many buttons of the menu's own. */
    public static int pageSize(final int buttons) {
        return Math.max(1, DialogMenu.MAX_OPTIONS - 2 - buttons);
    }

    /**
     * A button on every page, before the entries.
     *
     * @throws IllegalStateException when there would be no room left for a single entry
     */
    public PagedDialogMenu<T> button(final String label, final String tooltip, final Runnable chosen) {
        if (this.buttons.size() + 1 > DialogMenu.MAX_OPTIONS - 3) {
            throw new IllegalStateException("A paged dialog menu holds at most " + (DialogMenu.MAX_OPTIONS - 3) + " buttons of its own.");
        }
        this.buttons.add(new Button(label, tooltip, chosen));
        return this;
    }

    /** How each entry is shown, and what clicking it does. {@code tooltip} may give null for none. */
    public PagedDialogMenu<T> entries(final Function<T, String> label, final Function<T, String> tooltip, final EntryAction<T> chosen) {
        this.label = label;
        this.tooltip = tooltip == null ? entry -> null : tooltip;
        this.chosen = chosen;
        return this;
    }

    /** The close button, which runs {@code closed} in {@link #open}. The dialogs' default one unless set. */
    public PagedDialogMenu<T> close(final String close) {
        this.close = close;
        return this;
    }

    /** The page turns, in place of those set with {@link Dialogs#pageLabels}. */
    public PagedDialogMenu<T> pageButtons(final String previous, final String next) {
        this.previous = previous;
        this.next = next;
        return this;
    }

    public PagedDialogMenu<T> columns(final int columns) {
        this.columns = columns;
        return this;
    }

    /** Paper only, as on {@link DialogMenu#icon}. */
    public PagedDialogMenu<T> icon(final Material icon) {
        this.icon = icon;
        return this;
    }

    /** Entries on each page, after this menu's own buttons and the page turns. */
    public int pageSize() {
        return pageSize(this.buttons.size());
    }

    /** The page an entry is on, or 0 when it isn't listed. */
    public int pageOf(final T entry) {
        return Math.max(0, this.entries.indexOf(entry)) / this.pageSize();
    }

    /**
     * Shows one page, as {@link DialogMenu#open} does. A page past the end, as a removal can leave
     * behind, shows the last one instead.
     *
     * @return false if dialogs are not supported, in which case nothing is shown and nothing is run
     */
    public boolean open(final Player p, final int page, final Runnable closed, final Runnable failed) {
        final Pagination<T> pages = new Pagination<>(this.pageSize(), this.entries);
        final int shown = pages.exists(page) ? page : Math.max(0, pages.totalPages() - 1);

        final DialogMenu menu = new DialogMenu(
                this.title + " &7(" + (shown + 1) + "/" + Math.max(1, pages.totalPages()) + ")", this.description)
                .columns(this.columns);
        if (this.icon != null) {
            menu.icon(this.icon);
        }

        for (final Button button : this.buttons) {
            menu.option(button.label, button.tooltip, button.chosen);
        }
        if (!pages.isEmpty()) {
            for (final T entry : pages.getPage(shown)) {
                menu.option(this.label.apply(entry), this.tooltip.apply(entry), () -> this.chosen.chosen(entry, shown));
            }
        }
        if (pages.exists(shown - 1)) {
            menu.option(this.previous == null ? Dialogs.previousPageLabel() : this.previous, null,
                    () -> this.open(p, shown - 1, closed, failed));
        }
        if (pages.exists(shown + 1)) {
            menu.option(this.next == null ? Dialogs.nextPageLabel() : this.next, null,
                    () -> this.open(p, shown + 1, closed, failed));
        }

        if (this.close != null) {
            menu.close(this.close);
        }
        return menu.open(p, closed, failed);
    }
}
