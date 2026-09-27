package joserodpt.realutils.config;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * One YAML file in a plugin's folder, through BoostedYAML: the file every {@code R*Config} class
 * used to open for itself.
 *
 * <pre>{@code
 * config = YamlConfig.of(plugin, "config.yml").versioned("Version").ignoring("2", "Locations").load();
 * config.file().getString("Strings.Prefix");
 * }</pre>
 *
 * A file that can't be read is logged, and {@link #file()} stays null, as it always did.
 */
public final class YamlConfig {

    private final JavaPlugin plugin;
    private final String name;
    private YamlDocument document;

    private YamlConfig(final JavaPlugin plugin, final String name) {
        this.plugin = plugin;
        this.name = name;
    }

    /** {@code name} in the plugin's folder, with the bundled resource of that name as its defaults. */
    public static Builder of(final JavaPlugin plugin, final String name) {
        return new Builder(plugin, new File(plugin.getDataFolder(), name), name);
    }

    /**
     * @param resource the bundled defaults, or null for a file that has none, such as one the plugin
     *                 writes itself
     */
    public static Builder of(final JavaPlugin plugin, final File file, final String resource) {
        return new Builder(plugin, file, resource);
    }

    public YamlDocument file() {
        return this.document;
    }

    public void save() {
        try {
            this.document.save();
        } catch (final IOException | NullPointerException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save " + this.name + "!", e);
        }
    }

    public void reload() {
        try {
            this.document.reload();
        } catch (final IOException | NullPointerException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't reload " + this.name + "!", e);
        }
    }

    public static final class Builder {
        private final JavaPlugin plugin;
        private final File file;
        private final String resource;
        private final GeneralSettings.Builder general = GeneralSettings.builder();
        private final LoaderSettings.Builder loader = LoaderSettings.builder();
        private final UpdaterSettings.Builder updater = UpdaterSettings.builder();

        private Builder(final JavaPlugin plugin, final File file, final String resource) {
            this.plugin = plugin;
            this.file = file;
            this.resource = resource;
        }

        /**
         * Adds whatever the bundled file has and the saved one lacks, whenever the number under
         * {@code key} goes up.
         */
        public Builder versioned(final String key) {
            this.loader.setAutoUpdate(true);
            this.updater.setVersioning(new BasicVersioning(key));
            return this;
        }

        /**
         * Leaves these routes as the server owner wrote them when updating from {@code version},
         * for sections they fill in themselves.
         */
        public Builder ignoring(final String version, final String... routes) {
            for (final String route : routes) {
                this.updater.addIgnoredRoute(version, route, '.');
            }
            return this;
        }

        /** Whether a missing value reads from the bundled defaults, as it does unless turned off. */
        public Builder useDefaults(final boolean useDefaults) {
            this.general.setUseDefaults(useDefaults);
            return this;
        }

        public Builder maxCollectionAliases(final int aliases) {
            this.loader.setMaxCollectionAliases(aliases);
            return this;
        }

        public YamlConfig load() {
            final YamlConfig config = new YamlConfig(this.plugin, this.file.getName());
            try {
                final InputStream defaults = this.resource == null ? null : this.plugin.getResource(this.resource);
                config.document = defaults == null
                        ? YamlDocument.create(this.file, this.general.build(), this.loader.build(), DumperSettings.DEFAULT, this.updater.build())
                        : YamlDocument.create(this.file, defaults, this.general.build(), this.loader.build(), DumperSettings.DEFAULT, this.updater.build());
            } catch (final IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't setup " + this.file.getName() + "!", e);
            }
            return config;
        }
    }
}
