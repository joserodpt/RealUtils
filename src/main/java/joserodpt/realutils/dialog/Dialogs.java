package joserodpt.realutils.dialog;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Where a plugin turns dialogs on: Minecraft's dialog screens (1.21.6 and up), shown through
 * UniDialog on Paper and Spigot, in place of chat prompts and inventory menus.
 *
 * <p>Call {@link #setup} once the plugin is enabled and {@link #shutdown} when it is disabled.
 * Everything else - {@link DialogForm}, {@link DialogMenu}, {@link SettingsDialog} and
 * {@link #confirm} - shows nothing unless {@link #isSupported()}, so every screen keeps its chat or
 * inventory version for servers without dialogs.</p>
 *
 * <p>UniDialog is built for Java 21, while the plugins still load on servers much older than that.
 * So nothing touches a UniDialog class until the server is known to have dialogs.</p>
 */
public final class Dialogs {

    /** Whether the server has dialogs and they are set up. Nothing touches {@link DialogInput} otherwise. */
    private static boolean available = false;
    private static Plugin plugin;
    private static BooleanSupplier enabled = () -> true;
    /** {@link Text#color} unless set, so dialogs colour the same as the rest of the plugin. */
    private static UnaryOperator<String> colorizer = Text::color;

    private static String confirmLabel = "&aConfirm";
    private static String cancelLabel = "&cCancel";
    private static String closeLabel = "&cClose";
    private static String backLabel = "&7Back";
    private static String saveLabel = "&aSave";
    private static String previousPageLabel = "&6Previous page";
    private static String nextPageLabel = "&aNext page";

    private Dialogs() {
    }

    /**
     * Uses dialogs from here on if the server has them.
     *
     * @param enabled read every time a dialog would open - usually the plugin's {@code useDialogs}
     *                setting - so turning it off applies without a restart
     */
    public static void setup(final Plugin plugin, final BooleanSupplier enabled) {
        shutdown();
        Dialogs.plugin = plugin;
        Dialogs.enabled = enabled == null ? () -> true : enabled;
        //dialogs came with 1.21.6, which already needs Java 21
        if (Runtime.version().feature() < 21) {
            return;
        }
        try {
            available = DialogInput.setup(plugin);
        } catch (final Throwable e) {
            plugin.getLogger().log(Level.WARNING, "Dialogs are not available, so the chat and inventory menus are used instead.", e);
        }
    }

    public static void shutdown() {
        if (available) {
            available = false;
            DialogInput.shutdown();
        }
    }

    /** Whether dialogs can be shown right now: the server has them, and the plugin has not turned them off. */
    public static boolean isSupported() {
        if (!available) {
            return false;
        }
        try {
            return enabled.getAsBoolean();
        } catch (final RuntimeException e) {
            //a config that is not loaded yet, or being reloaded: dialogs stay on, as they default to
            return true;
        }
    }

    /** Takes whatever dialog is on this player's screen away, and forgets its callbacks. */
    public static void close(final UUID uuid) {
        if (available) {
            DialogInput.close(uuid);
        }
    }

    /**
     * How {@code &} colour codes become colours in every dialog. By default Bukkit's own; a plugin
     * with hex colours passes its own colouring here.
     */
    public static void colorizer(final UnaryOperator<String> colorizer) {
        Dialogs.colorizer = colorizer == null ? Text::color : colorizer;
    }

    /** The buttons dialogs show when a screen does not name its own, usually from the plugin's language file. */
    public static void labels(final String confirm, final String cancel, final String close, final String back, final String save) {
        if (confirm != null) {
            confirmLabel = confirm;
        }
        if (cancel != null) {
            cancelLabel = cancel;
        }
        if (close != null) {
            closeLabel = close;
        }
        if (back != null) {
            backLabel = back;
        }
        if (save != null) {
            saveLabel = save;
        }
    }

    /**
     * A yes-or-no question, such as before something that cannot be undone. Escape counts as no.
     *
     * @return false if dialogs are not supported, in which case nothing is shown and nothing is
     * run: the caller asks its own way, or goes ahead as it did before
     */
    public static boolean confirm(final Player p, final String title, final String question,
                                  final String yes, final String no, final Runnable confirmed, final Runnable declined) {
        return new DialogForm(title, question)
                .buttons(yes == null ? confirmLabel : yes, no == null ? cancelLabel : no)
                //Escape tells the server nothing, and a question has to be answered one way or the other
                .closeWithEscape(false)
                .open(p, answers -> confirmed.run(), declined == null ? () -> { } : declined,
                        //could not be shown after all: nothing was asked, so nothing is done
                        declined == null ? () -> { } : declined);
    }

    /** Colours text the way dialogs do, with the plugin's {@link #colorizer}. */
    public static String colorText(final String text) {
        return color(text);
    }

    static Plugin plugin() {
        return plugin;
    }

    /** The page turns of a {@link PagedDialogMenu} that does not name its own. */
    public static void pageLabels(final String previous, final String next) {
        if (previous != null) {
            previousPageLabel = previous;
        }
        if (next != null) {
            nextPageLabel = next;
        }
    }

    static String color(final String text) {
        return text == null || text.isEmpty() ? "" : colorizer.apply(text);
    }

    static String confirmLabel() {
        return color(confirmLabel);
    }

    static String cancelLabel() {
        return color(cancelLabel);
    }

    static String closeLabel() {
        return color(closeLabel);
    }

    static String backLabel() {
        return color(backLabel);
    }

    static String saveLabel() {
        return color(saveLabel);
    }

    static String previousPageLabel() {
        return previousPageLabel;
    }

    static String nextPageLabel() {
        return nextPageLabel;
    }
}
