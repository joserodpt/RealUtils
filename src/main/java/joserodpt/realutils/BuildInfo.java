package joserodpt.realutils;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/**
 * When a plugin's jar was built, as Maven stamped it into a {@code build.properties} at the root of
 * the jar:
 *
 * <pre>{@code
 * # src/main/resources/build.properties, with resource filtering on
 * build.time=${build.time}
 * realutils.version=${realutils.version}
 *
 * <!-- pom.xml; maven.build.timestamp is always UTC, and can't be filtered in directly -->
 * <maven.build.timestamp.format>yyyy-MM-dd'T'HH:mm:ss'Z'</maven.build.timestamp.format>
 * <build.time>${maven.build.timestamp}</build.time>
 * }</pre>
 */
public final class BuildInfo {

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private BuildInfo() {
    }

    /**
     * The build time as {@code day/month/year hour:minute:second}, in the server's time zone, or
     * {@code "unknown"} for a jar built without the stamp.
     */
    public static String time(final Plugin plugin) {
        final Instant built = instant(plugin);
        return built == null ? "unknown" : SHOWN.withZone(ZoneId.systemDefault()).format(built);
    }

    /** The build time, or null for a jar built without the stamp. */
    public static Instant instant(final Plugin plugin) {
        final String stamp = read(plugin, "build.time");
        try {
            return stamp == null ? null : Instant.parse(stamp);
        } catch (final DateTimeParseException e) {
            return null;
        }
    }

    /** The RealUtils version the jar was built with, or {@code "unknown"}. */
    public static String realUtilsVersion(final Plugin plugin) {
        final String version = read(plugin, "realutils.version");
        return version == null ? "unknown" : version;
    }

    private static String read(final Plugin plugin, final String key) {
        try (InputStream in = plugin.getResource("build.properties")) {
            if (in == null) {
                return null;
            }
            final Properties properties = new Properties();
            properties.load(in);
            final String value = properties.getProperty(key);
            //still the placeholder where the jar was built without resource filtering
            return value == null || value.contains("${") ? null : value.trim();
        } catch (final IOException e) {
            return null;
        }
    }
}
