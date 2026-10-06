package com.mawlee.cointcore.command;

import com.mawlee.cointcore.ban.BanService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.UsernameCache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BanCommand {
    private static final List<String> DURATION_SUGGESTIONS = List.of("10m", "30m", "1h", "12h", "1d", "7d", "30d", "perm");

    private BanCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "ban", banCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> banCommand() {
        return Commands.literal("ban")
                .requires(BanCommand::canBan)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                suggestPlayerNames(context.getSource()),
                                builder
                        ))
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        DURATION_SUGGESTIONS,
                                        builder
                                ))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(BanCommand::banPlayer))));
    }

    private static boolean canBan(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.BAN);
        }

        return source.hasPermission(3);
    }

    private static int banPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");
        String duration = StringArgumentType.getString(context, "duration");
        String reason = stripQuotes(StringArgumentType.getString(context, "reason"));

        if (reason.isBlank()) {
            return 0;
        }

        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            sendFailure(source, CointCoreMessages.BAN_PLAYER_NOT_FOUND, targetName);
            return 0;
        }

        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        return BanService.ban(source, targetId.get(), resolvedName, duration, reason) ? 1 : 0;
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        Set<String> names = new LinkedHashSet<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        for (String cached : UsernameCache.getMap().values()) {
            if (cached != null && !cached.isBlank()) {
                names.add(cached);
            }
        }
        return List.copyOf(names);
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

    private static void sendFailure(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(messageFor(source, key, args));
    }

    public static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }

        return CointCoreMessages.forConsole(key, args);
    }
}
