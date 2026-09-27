package joserodpt.realutils.input;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.dialog.DialogForm;
import joserodpt.realutils.dialog.Dialogs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Asks a player to type something: in a dialog's text box on servers that have them, otherwise in
 * chat with a title on screen until they answer. Typing {@code cancel} cancels.
 *
 * <p>Set up once per plugin with {@link #setup}, which is also where its words come from, and
 * register {@link #getListener()}. Every callback runs on the main thread, a few ticks after the
 * answer, and only while the player is still online.</p>
 */
public final class PlayerInput {

    private static final String FIELD = "input";

    //read and taken from the async chat thread, written from the main thread
    private static final Map<UUID, PlayerInput> inputs = new ConcurrentHashMap<>();

    private static Plugin plugin;
    private static Function<Player, List<String>> defaultTitles = p -> Collections.emptyList();
    private static Function<Player, List<String>> defaultDialog = p -> Collections.emptyList();
    private static Consumer<Player> cancelledMessage = p -> { };
    private static Consumer<Player> errorMessage = p -> { };

    private final UUID uuid;
    private final List<String> titles;
    private final boolean clearInput;
    private final InputRunnable runGo;
    private final InputRunnable runCancel;
    /** The title reminder, or null while the question is asked in a dialog instead. */
    private BukkitTask titleTask;

    /**
     * @param titles    the two title lines shown while waiting for chat input
     * @param dialog    the title and description of the text box asked in on servers with dialogs
     * @param cancelled tells a player their input was cancelled
     * @param error     tells a player something went wrong with what they typed
     */
    public static void setup(final Plugin owner, final Function<Player, List<String>> titles, final Function<Player, List<String>> dialog,
                             final Consumer<Player> cancelled, final Consumer<Player> error) {
        plugin = owner;
        defaultTitles = titles;
        defaultDialog = dialog;
        cancelledMessage = cancelled;
        errorMessage = error;
    }

    /** Asks with the plugin's own title lines and text box. */
    public PlayerInput(final Player p, final boolean clearInput, final InputRunnable correct, final InputRunnable cancel) {
        this(p, clearInput, null, null, correct, cancel);
    }

    /**
     * @param clearInput strip colour codes from what is typed. Without it, {@code &} codes reach the
     *                   callback as typed, for input such as a name that may be coloured
     * @param titles     two title lines for the chat prompt, or null for the plugin's own
     * @param dialog     the text box's title and description, null for the plugin's own, or an
     *                   empty list to always ask in chat
     */
    public PlayerInput(final Player p, final boolean clearInput, final List<String> titles, final List<String> dialog,
                       final InputRunnable correct, final InputRunnable cancel) {
        this.uuid = p.getUniqueId();
        this.clearInput = clearInput;
        this.runGo = correct;
        this.runCancel = cancel;
        final List<String> lines = titles != null && titles.size() >= 2 ? titles : defaultTitles.apply(p);
        this.titles = lines.size() >= 2 ? lines : java.util.Arrays.asList("", "");
        p.closeInventory();
        this.register();

        final List<String> box = dialog == null ? defaultDialog.apply(p) : dialog;
        final boolean asked = box.size() >= 2 && new DialogForm(box.get(0), box.get(1))
                .text(FIELD, "", "", 256)
                //Escape would close it without telling the server, leaving the prompt waiting forever
                .closeWithEscape(false)
                .open(p, answers -> this.answered(p, answers.text(FIELD, "")), () -> this.cancelled(p), () -> {
                    //shown in chat instead, unless it has been answered or replaced since
                    if (inputs.get(this.uuid) == this) {
                        this.startTitles(p);
                    }
                });
        if (!asked) {
            this.startTitles(p);
        }
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onPlayerChat(final AsyncPlayerChatEvent event) {
                //taken in one step, so two quick messages can't both answer the same prompt
                final PlayerInput current = inputs.remove(event.getPlayer().getUniqueId());
                if (current != null) {
                    event.setCancelled(true);
                    final String input = event.getMessage();
                    //this is the async chat thread: the task, the title and the callbacks belong on the main one
                    Bukkit.getScheduler().runTask(plugin, () -> current.handle(event.getPlayer(), input));
                }
            }

            @EventHandler
            public void onQuit(final PlayerQuitEvent event) {
                //otherwise the title task runs forever and their first chat line after rejoining answers
                //a prompt from a previous session
                final PlayerInput current = inputs.remove(event.getPlayer().getUniqueId());
                if (current != null) {
                    current.stop();
                }
            }
        };
    }

    /** Drops every unanswered prompt, for a reload: their callbacks hold objects about to be replaced. */
    public static void cancelAll() {
        for (final UUID uuid : inputs.keySet()) {
            final PlayerInput current = inputs.remove(uuid);
            if (current != null) {
                current.stop();
                final Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendTitle("", "", 0, 1, 0);
                }
            }
        }
    }

    private void startTitles(final Player p) {
        final String top = Dialogs.colorText(this.titles.get(0));
        final String bottom = Dialogs.colorText(this.titles.get(1));
        this.titleTask = new BukkitRunnable() {
            public void run() {
                p.sendTitle(top, bottom, 0, 21, 0);
            }
        }.runTaskTimer(plugin, 0L, 20);
    }

    /** Stops whatever is asking the question: the title reminder, or the text box. */
    private void stop() {
        if (this.titleTask != null) {
            this.titleTask.cancel();
        }
        Dialogs.close(this.uuid);
    }

    /** What was typed in the text box, answered as if it had been typed in chat. */
    private void answered(final Player p, final String input) {
        //only while this is still the prompt waiting, not one a newer prompt replaced
        if (inputs.remove(this.uuid, this)) {
            this.handle(p, input);
        }
    }

    private void cancelled(final Player p) {
        if (inputs.remove(this.uuid, this)) {
            this.stop();
            cancelledMessage.accept(p);
            this.later(p, this.runCancel, "");
        }
    }

    private void handle(final Player p, final String typed) {
        this.stop();
        p.sendTitle("", "", 0, 1, 0);
        final String input = this.clearInput ? ChatColor.stripColor(Dialogs.colorText(typed)).trim() : typed.trim();
        if (input.equalsIgnoreCase("cancel")) {
            cancelledMessage.accept(p);
            this.later(p, this.runCancel, input);
        } else {
            this.later(p, this.runGo, input);
        }
    }

    private void later(final Player p, final InputRunnable runnable, final String input) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            try {
                runnable.run(input);
            } catch (final Exception e) {
                errorMessage.accept(p);
                plugin.getLogger().log(Level.WARNING, "Could not handle " + p.getName() + "'s input \"" + input + "\"", e);
            }
        }, 3);
    }

    private void register() {
        final PlayerInput previous = inputs.put(this.uuid, this);
        //a new prompt replaces an unanswered one, whose title task would otherwise never stop
        if (previous != null) {
            previous.stop();
        }
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input) throws Exception;
    }
}
