package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.punishment.PunishmentFormatter;
import com.mawlee.cointcore.punishment.PunishmentHistory;
import com.mawlee.cointcore.punishment.PunishmentRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

public final class PunishmentsCommand {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private PunishmentsCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "punishments", punishmentsCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> punishmentsCommand() {
        return Commands.literal("punishments")
                .requires(PunishmentsCommand::canView)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                suggestPlayerNames(context.getSource()),
                                builder
                        ))
                        .executes(context -> showPunishments(context, DEFAULT_LIMIT))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                                .executes(context -> showPunishments(
                                        context,
                                        IntegerArgumentType.getInteger(context, "limit")
                                ))));
    }

    private static boolean canView(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.PUNISHMENTS);
        }

        return source.hasPermission(2);
    }

    private static int showPunishments(CommandContext<CommandSourceStack> context, int limit) {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");

        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            source.sendFailure(messageFor(source, CointCoreMessages.PUNISHMENTS_PLAYER_NOT_FOUND, targetName));
            return 0;
        }

        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        int total = PunishmentHistory.count(source.getServer(), targetId.get());
        List<PunishmentRecord> records = PunishmentHistory.getRecords(source.getServer(), targetId.get(), limit);

        if (records.isEmpty()) {
            source.sendSuccess(
                    () -> messageFor(source, CointCoreMessages.PUNISHMENTS_EMPTY, resolvedName),
                    false
            );
            return 1;
        }

        String language = source.getEntity() instanceof ServerPlayer player ? player.getLanguage() : "en_us";
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.PUNISHMENTS_HEADER, resolvedName, total),
                false
        );

        int index = 1;
        for (PunishmentRecord record : records) {
            int entryNumber = index++;
            source.sendSuccess(
                    () -> LegacyTextParser.parse(String.format(
                            translate(source, CointCoreMessages.PUNISHMENTS_ENTRY),
                            entryNumber,
                            PunishmentFormatter.formatType(language, record.type()),
                            PunishmentFormatter.formatIssuedAt(record.issuedAt()),
                            record.issuer(),
                            record.reason(),
                            PunishmentFormatter.formatDuration(language, record)
                    )),
                    false
            );
        }

        return records.size();
    }

    private static String translate(CommandSourceStack source, String key) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.translateKey(player.getLanguage(), key);
        }

        String text = CointCoreMessages.translateKey("en_us", key);
        return text != null ? text : key;
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }

        return CointCoreMessages.forConsole(key, args);
    }
}
