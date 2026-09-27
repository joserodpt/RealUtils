package joserodpt.realutils.dialog;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.function.Function;

/**
 * How the Paper dialog backend turns text into components on a Paper too old for sprites:
 * {@code §} colours, hex ones included. UniDialog's own default reads only the sixteen named
 * colours, which turned a plugin's hex colours into stray characters.
 *
 * <p>Kept apart from {@link PaperText}, which refers to the sprite classes those older versions
 * do not have.</p>
 */
final class PaperLegacyText implements Function<String, Component> {

    /** {@code §x§r§r§g§g§b§b}, the form Bukkit's hex colours take. */
    static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    @Override
    public Component apply(final String text) {
        return SERIALIZER.deserialize(text);
    }
}
