package joserodpt.realutils;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.gui.GUIBuilder;
import joserodpt.realutils.gui.MaterialPickerGUI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * The plugin this copy of RealUtils belongs to. Each plugin shades and relocates its own copy, so
 * this is one plugin, never several.
 *
 * <p>Call {@link #setup} first thing in {@code onEnable}: it registers the listeners behind
 * {@link GUIBuilder} and {@link MaterialPickerGUI}, and everything that schedules a task or logs
 * goes through the plugin given here.</p>
 */
public final class RealUtils {

    private static JavaPlugin plugin;

    private RealUtils() {
    }

    public static void setup(final JavaPlugin owner) {
        plugin = owner;
        Bukkit.getPluginManager().registerEvents(GUIBuilder.getListener(), owner);
        Bukkit.getPluginManager().registerEvents(MaterialPickerGUI.getListener(), owner);
    }

    public static JavaPlugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("RealUtils.setup(plugin) has not been called");
        }
        return plugin;
    }

    /** The plugin's logger, or the server's before {@link #setup}. */
    public static Logger logger() {
        return plugin == null ? Bukkit.getLogger() : plugin.getLogger();
    }
}
