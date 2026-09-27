package joserodpt.realutils.command;

/*
 * RealUtils - shared utilities for the Real* plugins
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealUtils
 */

import joserodpt.realutils.text.Text;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Usage;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;
import revxrsal.commands.command.ExecutableCommand;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Puts the errors Lamp raises through the plugin's own messages, so a mistyped command reads the
 * same as every other message it sends. Anything not overridden here keeps Lamp's own wording,
 * which is already specific about what it couldn't parse. Extend it to handle more.
 */
public class LampExceptionHandler extends BukkitExceptionHandler {

    private final Consumer<CommandSender> unknownCommand;
    private final Consumer<CommandSender> noPermission;
    private final Consumer<CommandSender> playerOnly;
    private final Supplier<String> genericUsage;

    /**
     * @param genericUsage what a command without its own {@link Usage} says when an argument is
     *                     missing, sent with the plugin's {@link Text#send prefix}
     */
    public LampExceptionHandler(final Consumer<CommandSender> unknownCommand, final Consumer<CommandSender> noPermission,
                                final Consumer<CommandSender> playerOnly, final Supplier<String> genericUsage) {
        this.unknownCommand = unknownCommand;
        this.noPermission = noPermission;
        this.playerOnly = playerOnly;
        this.genericUsage = genericUsage;
    }

    @Override
    public void onUnknownCommand(final UnknownCommandException e, final BukkitCommandActor actor) {
        this.unknownCommand.accept(actor.sender());
    }

    @Override
    public void onNoPermission(final NoPermissionException e, final BukkitCommandActor actor) {
        this.noPermission.accept(actor.sender());
    }

    /**
     * Player-only commands take a {@link org.bukkit.entity.Player} instead of a
     * {@link CommandSender}, and this is where console gets told so.
     */
    @Override
    public void onSenderNotPlayer(final SenderNotPlayerException e, final BukkitCommandActor actor) {
        this.playerOnly.accept(actor.sender());
    }

    @Override
    public void onMissingArgument(final MissingArgumentException e, final BukkitCommandActor actor,
                                  final ParameterNode<BukkitCommandActor, ?> parameter) {
        Text.send(actor.sender(), this.usageOf(e.command()));
    }

    /**
     * The handwritten {@link Usage} on the method, which spells the command out the way players are
     * used to seeing it. Lamp declares the annotation but never reads it itself.
     */
    private String usageOf(final ExecutableCommand<?> command) {
        final Usage usage = command.annotations().get(Usage.class);
        return usage == null ? this.genericUsage.get() : usage.value();
    }
}
