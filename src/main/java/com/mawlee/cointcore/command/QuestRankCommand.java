package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.QuestRankConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.questrank.QuestRankService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class QuestRankCommand {
    private QuestRankCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("questranks")
                .requires(QuestRankCommand::canAdmin)
                .then(Commands.literal("status")
                        .executes(QuestRankCommand::status))
                .then(Commands.literal("sync")
                        .executes(QuestRankCommand::syncAll)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        onlineNames(context.getSource()),
                                        builder
                                ))
                                .executes(QuestRankCommand::syncPlayer)));
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.RELOAD);
        }
        return source.hasPermission(2);
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.QUEST_RANKS_STATUS, QuestRankService.statusLine()),
                false
        );
        return 1;
    }

    private static int syncAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!QuestRankConfig.get().enabled()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUEST_RANKS_DISABLED));
            return 0;
        }
        if (!QuestRankService.ready()) {
            source.sendFailure(CointCoreMessages.forSource(
                    source,
                    CointCoreMessages.QUEST_RANKS_UNAVAILABLE,
                    QuestRankService.statusLine()
            ));
            return 0;
        }
        QuestRankService.SyncSummary summary = QuestRankService.sweep(source.getServer(), "command");
        source.sendSuccess(
                () -> CointCoreMessages.forSource(
                        source,
                        CointCoreMessages.QUEST_RANKS_SYNC_ALL,
                        summary.scanned(),
                        summary.granted(),
                        summary.already()
                ),
                true
        );
        return 1;
    }

    private static int syncPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "player");
        if (!QuestRankConfig.get().enabled()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUEST_RANKS_DISABLED));
            return 0;
        }
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        if (player == null) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUEST_RANKS_PLAYER_OFFLINE, name));
            return 0;
        }
        if (!QuestRankService.ready()) {
            source.sendFailure(CointCoreMessages.forSource(
                    source,
                    CointCoreMessages.QUEST_RANKS_UNAVAILABLE,
                    QuestRankService.statusLine()
            ));
            return 0;
        }
        QuestRankService.SyncSummary summary = QuestRankService.syncPlayer(player, "command");
        source.sendSuccess(
                () -> CointCoreMessages.forSource(
                        source,
                        CointCoreMessages.QUEST_RANKS_SYNC_PLAYER,
                        player.getGameProfile().getName(),
                        summary.granted(),
                        summary.already()
                ),
                true
        );
        return 1;
    }

    private static List<String> onlineNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }
}
