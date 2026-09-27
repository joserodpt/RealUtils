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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One {@link LanguageLine} with its placeholders filled in. A new one per message and never shared,
 * so nothing set here can leak into the next.
 */
public final class LanguageMessage {

    private final LanguageLine line;
    private final Map<Placeholder, String> values = new LinkedHashMap<>();

    LanguageMessage(final LanguageLine line) {
        this.line = line;
    }

    /** Fills a placeholder. Setting the same one again replaces its value. */
    public LanguageMessage with(final Placeholder placeholder, final Object value) {
        this.values.put(placeholder, String.valueOf(value));
        return this;
    }

    /** The line with its placeholders replaced, then coloured, so a value may carry colour codes of its own. */
    public String get() {
        final YamlDocument file = this.line.getLanguageFile();
        String s = file == null ? null : file.getString(this.line.getPath());
        if (s == null) {
            //a route missing from a hand-edited or older language file, said plainly rather than
            //printed to the player as the word "null"
            return Text.color("&cMissing language entry: " + this.line.getPath());
        }
        for (final Map.Entry<Placeholder, String> entry : this.values.entrySet()) {
            s = s.replace(entry.getKey().getToken(), entry.getValue());
        }
        return Text.color(s);
    }

    /** The line with the plugin's prefix in front. */
    public void send(final CommandSender sender) {
        Text.send(sender, this.get());
    }
}
