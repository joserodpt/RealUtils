package joserodpt.realutils.dialog;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A plugin's config as dialogs: a menu of categories, each opening a form of its settings.
 * Saving writes only what changed and brings the menu back; Back returns to it without saving.
 *
 * <pre>{@code
 * final SettingsDialog settings = new SettingsDialog("&9MyPlugin &8| &fSettings").icon(Material.COMMAND_BLOCK);
 * settings.category("&eGeneral", "&7Prefix and messages")
 *         .text("MyPlugin.Prefix", "Plugin prefix", 64)
 *         .toggle("MyPlugin.Debug", "Debug messages");
 * settings.category("&aTimings", "&7How long things take")
 *         .slider("MyPlugin.Countdown", "Countdown seconds", 5, 120, 5).note("after a restart");
 * settings.open(player, SettingsStore.of(config::get, config::set, MyConfig::save), () -> new SettingsGUI(player).open());
 * }</pre>
 *
 * <p>Labels are shown as given, with {@code &} colours; settings without a colour of their own
 * are drawn in yellow.</p>
 */
public final class SettingsDialog {

    private final String title;
    private final List<Category> categories = new ArrayList<>();
    private Material icon;
    private String description = "";
    private BiConsumer<Player, Category> saved = (p, category) -> { };

    public SettingsDialog(final String title) {
        this.title = title;
    }

    /** Shown above the categories. Paper only, as items are in every dialog. */
    public SettingsDialog icon(final Material icon) {
        this.icon = icon;
        return this;
    }

    /** Text above the categories, or none when empty. */
    public SettingsDialog description(final String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    /** Run after a category is saved and the config written, for a message or to reload what reads it. */
    public SettingsDialog onSave(final BiConsumer<Player, Category> saved) {
        this.saved = saved == null ? (p, category) -> { } : saved;
        return this;
    }

    /** A new category, which the settings added to it next belong to. */
    public Category category(final String name, final String tooltip) {
        final Category category = new Category(name, tooltip);
        this.categories.add(category);
        return category;
    }

    /**
     * Shows the menu, or the only category straight away if there is just one.
     *
     * @param fallback run if dialogs are not supported, or the menu could not be shown: the
     *                 plugin's inventory editor, or a message saying where the config is
     */
    public void open(final Player p, final SettingsStore store, final Runnable fallback) {
        if (this.categories.size() == 1) {
            this.openCategory(p, store, this.categories.get(0), false, fallback);
            return;
        }

        final DialogMenu menu = new DialogMenu(this.title, this.description);
        if (this.icon != null) {
            menu.icon(this.icon);
        }
        for (final Category category : this.categories) {
            menu.option(category.name, category.tooltip, () -> this.openCategory(p, store, category, true, fallback));
        }
        if (!menu.open(p, () -> { }, fallback)) {
            fallback.run();
        }
    }

    private void openCategory(final Player p, final SettingsStore store, final Category category,
                              final boolean fromMenu, final Runnable fallback) {
        final DialogForm form = new DialogForm(this.title + " &8> " + category.name, category.description);
        for (final Setting setting : category.settings) {
            setting.add(form, store, p);
        }
        form.buttons(Dialogs.saveLabel(), fromMenu ? Dialogs.backLabel() : Dialogs.cancelLabel());

        final Runnable back = fromMenu ? () -> this.open(p, store, fallback) : () -> { };
        final boolean shown = form.open(p, answers -> {
            boolean changed = false;
            for (final Setting setting : category.settings) {
                changed |= setting.save(answers, store, p);
            }
            if (changed) {
                store.save();
            }
            this.saved.accept(p, category);
            back.run();
        }, back, fallback);

        if (!shown) {
            fallback.run();
        }
    }

    /** One page of settings. */
    public static final class Category {
        private final String name;
        private final String tooltip;
        private final List<Setting> settings = new ArrayList<>();
        private String description = "";

        private Category(final String name, final String tooltip) {
            this.name = name;
            this.tooltip = tooltip;
        }

        public String getName() {
            return this.name;
        }

        /** Text above this category's settings. */
        public Category description(final String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        public Category toggle(final String path, final String label) {
            return this.add(new Toggle(path, label));
        }

        /** A whole number between {@code min} and {@code max}. */
        public Category slider(final String path, final String label, final int min, final int max, final int step) {
            return this.add(new Slider(path, label, min, max, step, true));
        }

        /** A number that can stop between whole numbers, such as a price. */
        public Category decimal(final String path, final String label, final double min, final double max, final double step) {
            return this.add(new Slider(path, label, min, max, step, false));
        }

        public Category text(final String path, final String label, final int maxLength) {
            return this.add(new Text(path, label, maxLength));
        }

        /** A list of text, edited as one comma-separated line. */
        public Category list(final String path, final String label) {
            return this.add(new ListSetting(path, label, false));
        }

        /** A list of whole numbers, edited as one comma-separated line; anything that is not one is dropped. */
        public Category numbers(final String path, final String label) {
            return this.add(new ListSetting(path, label, true));
        }

        /**
         * Settings that don't map onto one path, such as a location taken from where the player stands.
         *
         * @param save writes what was answered; return whether anything changed, so the config is saved
         */
        public Category custom(final BiConsumer<DialogForm, Player> fields, final SaveAction save) {
            return this.add(new Custom(fields, save));
        }

        /** Grey text after the last setting's label, such as "after a restart". */
        public Category note(final String note) {
            if (!this.settings.isEmpty()) {
                this.settings.get(this.settings.size() - 1).note = note;
            }
            return this;
        }

        /** A sprite of {@code material} at the start of the last setting's label, where the server can draw one. */
        public Category sprite(final Material material) {
            if (!this.settings.isEmpty()) {
                this.settings.get(this.settings.size() - 1).sprite = material;
            }
            return this;
        }

        private Category add(final Setting setting) {
            this.settings.add(setting);
            return this;
        }
    }

    @FunctionalInterface
    public interface SaveAction {
        boolean save(DialogForm.Answers answers, Player player);
    }

    private abstract static class Setting {
        final String path;
        final String label;
        String note;
        Material sprite;

        Setting(final String path, final String label) {
            this.path = path;
            this.label = label;
        }

        String label() {
            final String colored = this.label.startsWith("&") || this.label.startsWith("§") ? this.label : "&e" + this.label;
            return this.note == null ? colored : colored + " &7(" + this.note + ")";
        }

        void decorate(final DialogForm form) {
            if (this.sprite != null) {
                form.sprite(this.sprite);
            }
        }

        abstract void add(DialogForm form, SettingsStore store, Player p);

        /** @return whether anything was changed */
        abstract boolean save(DialogForm.Answers answers, SettingsStore store, Player p);
    }

    private static final class Toggle extends Setting {
        private boolean initial;

        Toggle(final String path, final String label) {
            super(path, label);
        }

        @Override
        void add(final DialogForm form, final SettingsStore store, final Player p) {
            this.initial = store.getBoolean(this.path, false);
            form.toggle(this.path, this.label(), this.initial);
            this.decorate(form);
        }

        @Override
        boolean save(final DialogForm.Answers answers, final SettingsStore store, final Player p) {
            final boolean value = answers.toggle(this.path, this.initial);
            if (value == this.initial) {
                return false;
            }
            store.set(this.path, value);
            return true;
        }
    }

    private static final class Slider extends Setting {
        private final double min;
        private final double max;
        private final double step;
        private final boolean whole;
        private double initial;

        Slider(final String path, final String label, final double min, final double max, final double step, final boolean whole) {
            super(path, label);
            this.min = min;
            this.max = max;
            this.step = step;
            this.whole = whole;
        }

        @Override
        void add(final DialogForm form, final SettingsStore store, final Player p) {
            this.initial = store.getDouble(this.path, this.min);
            form.slider(this.path, this.label(), (float) this.min, (float) this.max, (float) this.step, (float) this.initial);
            this.decorate(form);
        }

        @Override
        boolean save(final DialogForm.Answers answers, final SettingsStore store, final Player p) {
            //untouched, it keeps its exact value even when that sits between two steps
            final Double moved = answers.moved(this.path, this.initial, this.step);
            if (moved == null) {
                return false;
            }
            store.set(this.path, this.whole ? (Object) (int) Math.round(moved) : (Object) moved);
            return true;
        }
    }

    private static final class Text extends Setting {
        private final int maxLength;
        private String initial;

        Text(final String path, final String label, final int maxLength) {
            super(path, label);
            this.maxLength = maxLength;
        }

        @Override
        void add(final DialogForm form, final SettingsStore store, final Player p) {
            this.initial = store.getString(this.path, "");
            form.text(this.path, this.label(), this.initial, this.maxLength);
            this.decorate(form);
        }

        @Override
        boolean save(final DialogForm.Answers answers, final SettingsStore store, final Player p) {
            final String value = answers.text(this.path, this.initial);
            if (Objects.equals(value, this.initial)) {
                return false;
            }
            store.set(this.path, value);
            return true;
        }
    }

    private static final class ListSetting extends Setting {
        private final boolean numbers;
        private String initial;

        ListSetting(final String path, final String label, final boolean numbers) {
            super(path, label);
            this.numbers = numbers;
        }

        @Override
        void add(final DialogForm form, final SettingsStore store, final Player p) {
            this.initial = String.join(", ", store.getStringList(this.path));
            form.text(this.path, this.label() + " &7(comma separated)", this.initial, 512);
            this.decorate(form);
        }

        @Override
        boolean save(final DialogForm.Answers answers, final SettingsStore store, final Player p) {
            final String value = answers.text(this.path, this.initial);
            if (Objects.equals(value, this.initial)) {
                return false;
            }
            final List<Object> list = new ArrayList<>();
            for (final String part : value.split(",")) {
                final String entry = part.trim();
                if (entry.isEmpty()) {
                    continue;
                }
                if (!this.numbers) {
                    list.add(entry);
                    continue;
                }
                try {
                    list.add(Integer.parseInt(entry));
                } catch (final NumberFormatException ignored) {
                    //not a number, so a stray word does not throw the rest away
                }
            }
            store.set(this.path, list);
            return true;
        }
    }

    private static final class Custom extends Setting {
        private final BiConsumer<DialogForm, Player> fields;
        private final SaveAction action;

        Custom(final BiConsumer<DialogForm, Player> fields, final SaveAction action) {
            super("", "");
            this.fields = fields;
            this.action = action;
        }

        @Override
        void add(final DialogForm form, final SettingsStore store, final Player p) {
            this.fields.accept(form, p);
        }

        @Override
        boolean save(final DialogForm.Answers answers, final SettingsStore store, final Player p) {
            return this.action.save(answers, p);
        }
    }
}
