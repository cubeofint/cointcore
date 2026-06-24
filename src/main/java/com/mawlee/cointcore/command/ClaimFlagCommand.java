package com.mawlee.cointcore.command;

import com.mawlee.cointcore.claim.ClaimFlagService;
import com.mawlee.cointcore.claim.ClaimTeamFlags;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ClaimFlagCommand {
    private static final SimpleCommandExceptionType FTB_UNAVAILABLE = new SimpleCommandExceptionType(
            Component.literal("FTB Chunks / FTB Teams is not loaded")
    );
    private static final SimpleCommandExceptionType TEAM_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("Team or player not found")
    );

    private ClaimFlagCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> claimFlagCommand() {
        if (!FtbIntegration.isAvailable()) {
            return null;
        }

        return Commands.literal("claim")
                .requires(ClaimFlagCommand::canManageFlags)
                .then(Commands.literal("flag")
                        .then(Commands.literal("info")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                context.getSource().getServer().getPlayerNames(),
                                                builder
                                        ))
                                        .executes(ClaimFlagCommand::showFlags)))
                        .then(Commands.literal("no_player_damage")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                context.getSource().getServer().getPlayerNames(),
                                                builder
                                        ))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ClaimFlagCommand::setNoPlayerDamage))))
                        .then(Commands.literal("no_hostile_mob_spawn")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                context.getSource().getServer().getPlayerNames(),
                                                builder
                                        ))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ClaimFlagCommand::setNoHostileMobSpawn))))
                        .then(Commands.literal("protect_mobs")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                context.getSource().getServer().getPlayerNames(),
                                                builder
                                        ))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ClaimFlagCommand::setProtectMobs)))));
    }

    private static boolean canManageFlags(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.CLAIM_FLAGS);
        }

        return source.hasPermission(2);
    }

    private static int showFlags(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        ClaimTeamFlags flags = ClaimFlagService.getFlags(team);

        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.CLAIM_FLAG_INFO,
                        team.getShortName(),
                        formatFlag(flags.disablePlayerDamage()),
                        formatFlag(flags.disableHostileMobSpawn()),
                        formatFlag(flags.protectMobsFromOutsiders())),
                false
        );
        return 1;
    }

    private static int setNoPlayerDamage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        ClaimFlagService.setDisablePlayerDamage(source.getServer(), team, enabled);
        source.sendSuccess(
                () -> messageFor(source,
                        enabled ? CointCoreMessages.CLAIM_FLAG_NO_PLAYER_DAMAGE_ENABLED : CointCoreMessages.CLAIM_FLAG_NO_PLAYER_DAMAGE_DISABLED,
                        team.getShortName()),
                true
        );
        return 1;
    }

    private static int setNoHostileMobSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        ClaimFlagService.setDisableHostileMobSpawn(source.getServer(), team, enabled);
        source.sendSuccess(
                () -> messageFor(source,
                        enabled ? CointCoreMessages.CLAIM_FLAG_NO_HOSTILE_MOB_SPAWN_ENABLED : CointCoreMessages.CLAIM_FLAG_NO_HOSTILE_MOB_SPAWN_DISABLED,
                        team.getShortName()),
                true
        );
        return 1;
    }

    private static int setProtectMobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        ClaimFlagService.setProtectMobsFromOutsiders(source.getServer(), team, enabled);
        source.sendSuccess(
                () -> messageFor(source,
                        enabled ? CointCoreMessages.CLAIM_FLAG_PROTECT_MOBS_ENABLED : CointCoreMessages.CLAIM_FLAG_PROTECT_MOBS_DISABLED,
                        team.getShortName()),
                true
        );
        return 1;
    }

    private static Team resolveTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String target = StringArgumentType.getString(context, "target");
        return FtbIntegration.resolveTeam(context.getSource().getServer(), target)
                .orElseThrow(TEAM_NOT_FOUND::create);
    }

    private static void ensureFtbLoaded() throws CommandSyntaxException {
        if (!FtbIntegration.isAvailable()) {
            throw FTB_UNAVAILABLE.create();
        }
    }

    private static String formatFlag(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }

        return CointCoreMessages.forConsole(key, args);
    }
}
