package com.mawlee.cointcore.command;

import com.mawlee.cointcore.watchdog.WatchdogAlertPreferences;
import com.mawlee.cointcore.watchdog.WatchdogAlertSubscriptions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Personal chat preference. It does not start/stop profiling or grant access to diagnostic commands. */
public final class WatchdogAlertsCommand {
    private WatchdogAlertsCommand() { }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "watchdog", Commands.literal("watchdog")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> status(context, null))
                .then(Commands.literal("on").executes(context -> status(context, true)))
                .then(Commands.literal("off").executes(context -> status(context, false))));
    }

    private static int status(CommandContext<CommandSourceStack> context, Boolean value)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        if (value != null) WatchdogAlertPreferences.set(player, value);
        boolean enabled = WatchdogAlertSubscriptions.isEnabled(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal("Сообщения watchdog "
                + (enabled ? "включены" : "выключены") + ". /watchdog on | off"), false);
        return 1;
    }
}
