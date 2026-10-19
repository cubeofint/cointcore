package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.watchdog.stall.StallContext;
import com.mawlee.cointcore.watchdog.stall.StallWatchdogHotFlags;
import com.mawlee.cointcore.watchdog.stall.StallWatchdogService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class StallCommand {
    private StallCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("stall")
                .requires(StallCommand::canUse)
                .then(Commands.literal("status").executes(StallCommand::status))
                .then(Commands.literal("test")
                        .executes(context -> test(context, 16))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(context -> test(context, IntegerArgumentType.getInteger(context, "seconds")))));
    }

    private static boolean canUse(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.WATCHDOG);
        }
        return source.hasPermission(2);
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(StallWatchdogService.instance().statusLine()), false);
        return 1;
    }

    private static int test(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Simulating server-thread stall for " + seconds + "s"), true);
        StallWatchdogHotFlags.TEST_STALL = true;
        StallContext.enterTest();
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            StallWatchdogHotFlags.TEST_STALL = false;
            StallContext.clear();
        }
        source.sendSuccess(() -> Component.literal("Stall test finished"), true);
        return 1;
    }
}