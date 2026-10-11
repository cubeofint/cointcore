package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.speedrunner.SpeedrunnerSavedData;
import com.mawlee.cointcore.speedrunner.SpeedrunnerService;
import com.mawlee.cointcore.speedrunner.SpeedrunnerTime;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SpeedrunnerCommand {
    private SpeedrunnerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("speedrunner")
                .requires(SpeedrunnerCommand::canAdmin)
                .then(Commands.literal("info").executes(SpeedrunnerCommand::info))
                .then(Commands.literal("reset").executes(SpeedrunnerCommand::reset))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .executes(SpeedrunnerCommand::setOnlineTime)
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(SpeedrunnerCommand::setExplicitTicks))));
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.SPEEDRUNNER_ADMIN);
        }
        return source.hasPermission(2);
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<SpeedrunnerSavedData.Record> record = SpeedrunnerService.info(source.getServer());
        if (record.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.SPEEDRUNNER_NONE), false);
            return 1;
        }
        SpeedrunnerSavedData.Record current = record.get();
        source.sendSuccess(() -> CointCoreMessages.forSource(
                source,
                CointCoreMessages.SPEEDRUNNER_INFO,
                current.holderName(),
                SpeedrunnerTime.formatPlayTime(current.ticks()),
                current.ticks(),
                SpeedrunnerTime.formatDate(current.craftedAtMs())
        ), false);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SpeedrunnerService.reset(source.getServer(), () -> source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.SPEEDRUNNER_RESET),
                true
        ));
        return 1;
    }

    private static int setOnlineTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "player"));
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
        if (online == null) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forSource(source, CointCoreMessages.SPEEDRUNNER_SET_NEEDS_TICKS, target.name())
            ).create();
        }
        applySet(source, target, SpeedrunnerService.playTimeTicks(online));
        return 1;
    }

    private static int setExplicitTicks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "player"));
        applySet(source, target, IntegerArgumentType.getInteger(context, "ticks"));
        return 1;
    }

    private static void applySet(CommandSourceStack source, ResolvedTarget target, int ticks) {
        SpeedrunnerService.adminSet(source.getServer(), target.id(), target.name(), ticks, () -> source.sendSuccess(
                () -> CointCoreMessages.forSource(
                        source,
                        CointCoreMessages.SPEEDRUNNER_SET,
                        target.name(),
                        SpeedrunnerTime.formatPlayTime(ticks),
                        ticks
                ),
                true
        ));
    }

    private static ResolvedTarget resolveTarget(CommandSourceStack source, String targetName)
            throws CommandSyntaxException {
        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forSource(source, CointCoreMessages.SPEEDRUNNER_PLAYER_NOT_FOUND, targetName)
            ).create();
        }
        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        return new ResolvedTarget(targetId.get(), resolvedName);
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private record ResolvedTarget(UUID id, String name) {
    }
}
