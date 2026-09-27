package joserodpt.realutils.dialog;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The config a {@link SettingsDialog} reads and writes, by path.
 *
 * <p>Built from three plain functions rather than tied to a config library, so any plugin's config
 * fits: a BoostedYAML {@code YamlDocument} ({@code doc::get}, {@code doc::set},
 * {@code MyConfig::save}) or a Bukkit {@code FileConfiguration} alike, whatever each plugin
 * shades and relocates.</p>
 */
public final class SettingsStore {

    private final Function<String, Object> getter;
    private final BiConsumer<String, Object> setter;
    private final Runnable saver;

    private SettingsStore(final Function<String, Object> getter, final BiConsumer<String, Object> setter, final Runnable saver) {
        this.getter = getter;
        this.setter = setter;
        this.saver = saver;
    }

    /**
     * @param get   the value at a path, or null if there is none
     * @param set   writes a value at a path
     * @param save  writes the config out to disk
     */
    public static SettingsStore of(final Function<String, Object> get, final BiConsumer<String, Object> set, final Runnable save) {
        return new SettingsStore(get, set, save);
    }

    Object get(final String path) {
        return this.getter.apply(path);
    }

    boolean getBoolean(final String path, final boolean fallback) {
        final Object value = this.get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }

    double getDouble(final String path, final double fallback) {
        final Object value = this.get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    String getString(final String path, final String fallback) {
        final Object value = this.get(path);
        return value == null ? fallback : value.toString();
    }

    List<String> getStringList(final String path) {
        final Object value = this.get(path);
        final List<String> list = new ArrayList<>();
        if (value instanceof Collection) {
            for (final Object entry : (Collection<?>) value) {
                list.add(String.valueOf(entry));
            }
        } else if (value != null && !value.toString().isEmpty()) {
            list.add(value.toString());
        }
        return list;
    }

    void set(final String path, final Object value) {
        this.setter.accept(path, value);
    }

    void save() {
        this.saver.run();
    }
}
