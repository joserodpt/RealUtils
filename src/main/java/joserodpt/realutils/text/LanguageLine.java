package joserodpt.realutils.text;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.command.CommandSender;

/**
 * A line a plugin says, kept in its language file. Each plugin's {@code TranslatableLine} enum
 * implements this and only has to say where its lines live:
 *
 * <pre>{@code
 * public enum TranslatableLine implements LanguageLine {
 *     REGION_DELETED("Region.Deleted");
 *     ...
 *     public String getPath() { return this.configPath; }
 *     public YamlDocument getLanguageFile() { return RRLanguage.file(); }
 * }
 *
 * TranslatableLine.REGION_DELETED.with(NAME, region.getDisplayName()).send(p);
 * }</pre>
 *
 * <p>Placeholders are filled on a new {@link LanguageMessage} each time, never on the constant,
 * which every caller shares.</p>
 */
public interface LanguageLine {

    /** The route of this line in the language file. */
    String getPath();

    /** The file the line is read from, or null while it isn't loaded. */
    YamlDocument getLanguageFile();

    /** Starts a message from this line with one placeholder filled; chain more with {@link LanguageMessage#with}. */
    default LanguageMessage with(final Placeholder placeholder, final Object value) {
        return new LanguageMessage(this).with(placeholder, value);
    }

    /** The line with no placeholders filled, coloured. */
    default String get() {
        return new LanguageMessage(this).get();
    }

    default void send(final CommandSender sender) {
        new LanguageMessage(this).send(sender);
    }
}
