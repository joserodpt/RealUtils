package joserodpt.realutils.text;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Colours and sends text the same way everywhere in a plugin.
 *
 * <p>How text is coloured and what goes in front of a message are the plugin's own, set once with
 * {@link #colorizer} and {@link #prefix}. Everything in RealUtils that shows text - GUI titles, item
 * names, dialogs - colours it through here.</p>
 */
public final class Text {

    private static final UnaryOperator<String> AMPERSAND = text -> ChatColor.translateAlternateColorCodes('&', text);

    private static UnaryOperator<String> colorizer = AMPERSAND;
    private static Supplier<String> prefix = () -> "";

    private Text() {
    }

    /**
     * How text is coloured. {@code &} codes unless set; {@code ForestColorAPI::colorize} adds hex
     * colours and gradients on 1.16 and up.
     */
    public static void colorizer(final UnaryOperator<String> colorizer) {
        Text.colorizer = colorizer == null ? AMPERSAND : colorizer;
    }

    /**
     * What {@link #send} puts in front of every message, read each time so a reloaded config
     * applies. Include any separator, such as {@code "&f"} or {@code " &r"}, in what it returns.
     */
    public static void prefix(final Supplier<String> prefix) {
        Text.prefix = prefix == null ? () -> "" : prefix;
    }

    public static String color(final String text) {
        return text == null || text.isEmpty() ? "" : colorizer.apply(text);
    }

    public static List<String> color(final Collection<?> lines) {
        final List<String> colored = new ArrayList<>();
        if (lines != null) {
            for (final Object line : lines) {
                colored.add(color(line == null ? null : line.toString()));
            }
        }
        return colored;
    }

    public static String strip(final String text) {
        return text == null ? "" : ChatColor.stripColor(text);
    }

    /** The prefix, coloured. */
    public static String getPrefix() {
        return color(prefix.get());
    }

    /** The message with the plugin's prefix in front. A missing message sends just the prefix. */
    public static void send(final CommandSender sender, final String message) {
        final String before = prefix.get();
        sender.sendMessage(color((before == null ? "" : before) + (message == null ? "" : message)));
    }

    /** The message on its own, without the prefix. */
    public static void sendRaw(final CommandSender sender, final String message) {
        sender.sendMessage(color(message));
    }

    /** Each line on its own, without the prefix. */
    public static void sendList(final CommandSender sender, final List<String> lines) {
        lines.forEach(line -> sender.sendMessage(color(line)));
    }

    /** DIAMOND_ORE as "Diamond Ore": a raw enum name in a GUI reads like a config key. */
    public static String beautifyEnumName(final String name) {
        if (name == null || name.isEmpty()) {
            return "Unknown";
        }
        final StringBuilder out = new StringBuilder();
        for (final String part : name.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return out.toString();
    }

    public static String beautifyMaterialName(final Material material) {
        return beautifyEnumName(material == null ? null : material.name());
    }

    /** 1500 as "1.5k", up to Q for 10^15. */
    public static String formatNumber(double number) {
        final String[] suffixes = {"", "k", "M", "B", "T", "Q"};
        int index = 0;
        while (number >= 1_000 && index < suffixes.length - 1) {
            number /= 1_000;
            ++index;
        }
        return new DecimalFormat("#.#").format(number) + suffixes[index];
    }
}
