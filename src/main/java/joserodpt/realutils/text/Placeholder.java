package joserodpt.realutils.text;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import java.util.Locale;

/**
 * A token a {@link LanguageLine} may contain. Meant for enums, whose constants supply
 * {@link #name()}: {@code MAXPLAYERS} is written {@code %maxplayers%}.
 */
public interface Placeholder {

    String name();

    default String getToken() {
        return "%" + this.name().toLowerCase(Locale.ROOT) + "%";
    }
}
