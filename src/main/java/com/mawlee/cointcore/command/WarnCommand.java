package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.punishment.WarnService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WarnCommand {
    private WarnCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "warn", warnCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> warnCommand() {
        return Commands.literal("warn")
                .requires(WarnCommand::canWarn)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                suggestPlayerNames(context.getSource()),
                                builder
                        ))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(WarnCommand::warnPlayer)));
    }

    private static boolean canWarn(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.WARN);
        }

        return source.hasPermission(2);
    }

    private static int warnPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");
        String reason = stripQuotes(StringArgumentType.getString(context, "reason"));

        if (reason.isBlank()) {
            return 0;
        }

        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            source.sendFailure(messageFor(source, CointCoreMessages.WARN_PLAYER_NOT_FOUND, targetName));
            return 0;
        }

        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        return WarnService.warn(source, targetId.get(), resolvedName, reason) ? 1 : 0;
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    public static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }

        return CointCoreMessages.forConsole(key, args);
    }
}
