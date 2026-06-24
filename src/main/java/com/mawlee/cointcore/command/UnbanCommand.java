package com.mawlee.cointcore.command;

import com.mawlee.cointcore.punishment.PunishmentHistory;
import com.mawlee.cointcore.punishment.PunishmentType;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.UserBanList;

import java.util.Collection;

public final class UnbanCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(
            Component.translatable("commands.pardon.failed")
    );

    private UnbanCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "pardon", pardonCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pardonCommand() {
        return Commands.literal("pardon")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerList().getBans().getUserList(),
                                builder
                        ))
                        .executes(context -> pardonPlayers(
                                context.getSource(),
                                GameProfileArgument.getGameProfiles(context, "targets")
                        )));
    }

    private static int pardonPlayers(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
        UserBanList bans = source.getServer().getPlayerList().getBans();
        int unbanned = 0;

        for (GameProfile profile : gameProfiles) {
            if (!bans.isBanned(profile)) {
                continue;
            }

            bans.remove(profile);
            unbanned++;
            PunishmentHistory.record(
                    source.getServer(),
                    PunishmentType.UNBAN,
                    profile.getId(),
                    profile.getName(),
                    source.getTextName(),
                    "",
                    PunishmentHistory.NO_EXPIRY
            );
            source.sendSuccess(
                    () -> Component.translatable("commands.pardon.success", Component.literal(profile.getName())),
                    true
            );
        }

        if (unbanned == 0) {
            throw ERROR_NOT_BANNED.create();
        }

        return unbanned;
    }
}
