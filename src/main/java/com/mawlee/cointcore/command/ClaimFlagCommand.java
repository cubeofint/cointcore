package com.mawlee.cointcore.command;

import com.mawlee.cointcore.claim.ClaimFlagService;
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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

import java.util.ArrayList;
import java.util.List;

public final class ClaimFlagCommand {
    private static final SimpleCommandExceptionType FTB_UNAVAILABLE = new SimpleCommandExceptionType(
            Component.literal("FTB Chunks / FTB Teams is not loaded")
    );
    private static final SimpleCommandExceptionType TEAM_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("Team or player not found")
    );
    private static final SimpleCommandExceptionType INVALID_MODE = new SimpleCommandExceptionType(
            Component.literal("Mode must be allow or deny")
    );
    private static final SimpleCommandExceptionType UNKNOWN_MOB = new SimpleCommandExceptionType(
            Component.literal("Unknown mob")
    );

    private ClaimFlagCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> claimFlagCommand() {
        if (!FtbIntegration.isAvailable()) {
            return null;
        }

        return Commands.literal("claim")
                .requires(ClaimFlagCommand::canSeeAny)
                .then(Commands.literal("flag")
                        .then(Commands.literal("info")
                                .then(targetArgument().executes(ClaimFlagCommand::showFlags)))
                        .then(Commands.literal("mob-spawn")
                                .requires(source -> canEdit(source, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN))
                                .then(targetArgument()
                                        .then(modeArgument().executes(ClaimFlagCommand::setMobSpawnAll))
                                        .then(Commands.argument("mob", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        BuiltInRegistries.ENTITY_TYPE.keySet(),
                                                        builder
                                                ))
                                                .then(Commands.literal("clear").executes(context -> setMobRule(context, ClaimFlagService.MobSpawnRule.CLEAR)))
                                                .then(modeArgument().executes(ClaimFlagCommand::setMobRuleFromMode)))))
                        .then(boolFlag("mob-damage", CointPermissionNodes.CLAIM_FLAG_MOB_DAMAGE, ClaimFlagCommand::setMobDamage))
                        .then(boolFlag("fire-spread", CointPermissionNodes.CLAIM_FLAG_FIRE_SPREAD, ClaimFlagCommand::setFireSpread))
                        .then(boolFlag("pvp", CointPermissionNodes.CLAIM_FLAG_PVP, ClaimFlagCommand::setPvp))
                        .then(Commands.literal("entry")
                                .requires(source -> canEdit(source, CointPermissionNodes.CLAIM_FLAG_ENTRY))
                                .then(targetArgument().then(modeArgument().executes(ClaimFlagCommand::setEntry)))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> targetArgument() {
        return Commands.argument("target", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerNames(),
                        builder
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> modeArgument() {
        return Commands.argument("mode", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("allow", "deny"), builder));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> boolFlag(
            String name,
            PermissionNode<Boolean> node,
            com.mojang.brigadier.Command<CommandSourceStack> command
    ) {
        return Commands.literal(name)
                .requires(source -> canEdit(source, node))
                .then(targetArgument().then(Commands.argument("enabled", BoolArgumentType.bool()).executes(command)));
    }

    private static boolean canSeeAny(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(2);
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        return PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN)
                || PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_MOB_DAMAGE)
                || PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_FIRE_SPREAD)
                || PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_PVP)
                || PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_ENTRY);
    }

    private static boolean canEdit(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.hasPermissions(2) || PermissionService.has(player, node);
        }
        return source.hasPermission(2);
    }

    private static int showFlags(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        String exceptions = formatMobRules(team);
        source.sendSuccess(
                () -> messageFor(
                        source,
                        CointCoreMessages.CLAIM_FLAG_INFO,
                        team.getShortName(),
                        deniesAll(team) ? "deny" : "allow",
                        ClaimFlagService.allowsMobDamage(team),
                        ClaimFlagService.allowsFireSpread(team),
                        ClaimFlagService.allowsPvp(team),
                        ClaimFlagService.isEntryMembersOnly(team) ? "deny" : "allow",
                        exceptions
                ),
                false
        );
        return 1;
    }

    private static int setMobSpawnAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean deny = parseDeny(StringArgumentType.getString(context, "mode"));
        ClaimFlagService.setMobSpawnDeniedAll(source.getServer(), team, deny);
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.CLAIM_FLAG_MOB_SPAWN, team.getShortName(), deny ? "deny" : "allow"),
                true
        );
        return 1;
    }

    private static int setMobRuleFromMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean deny = parseDeny(StringArgumentType.getString(context, "mode"));
        return setMobRule(context, deny ? ClaimFlagService.MobSpawnRule.DENY : ClaimFlagService.MobSpawnRule.ALLOW);
    }

    private static int setMobRule(CommandContext<CommandSourceStack> context, ClaimFlagService.MobSpawnRule rule) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
            throw UNKNOWN_MOB.create();
        }
        ClaimFlagService.setMobSpawnRule(source.getServer(), team, mobId, rule);
        String key = rule == ClaimFlagService.MobSpawnRule.CLEAR
                ? CointCoreMessages.CLAIM_FLAG_MOB_SPAWN_CLEARED
                : CointCoreMessages.CLAIM_FLAG_MOB_SPAWN_MOB;
        String mode = switch (rule) {
            case ALLOW -> "allow";
            case DENY -> "deny";
            case CLEAR -> "clear";
        };
        source.sendSuccess(() -> messageFor(source, key, team.getShortName(), mobId.toString(), mode), true);
        return 1;
    }

    private static int setMobDamage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setBooleanFlag(context, CointCoreMessages.CLAIM_FLAG_MOB_DAMAGE, ClaimFlagService::setMobDamage);
    }

    private static int setFireSpread(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setBooleanFlag(context, CointCoreMessages.CLAIM_FLAG_FIRE_SPREAD, ClaimFlagService::setFireSpread);
    }

    private static int setPvp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setBooleanFlag(context, CointCoreMessages.CLAIM_FLAG_PVP, ClaimFlagService::setPvp);
    }

    private static int setEntry(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean membersOnly = parseDeny(StringArgumentType.getString(context, "mode"));
        ClaimFlagService.setEntryMembersOnly(source.getServer(), team, membersOnly);
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.CLAIM_FLAG_ENTRY, team.getShortName(), membersOnly ? "deny" : "allow"),
                true
        );
        return 1;
    }

    private static int setBooleanFlag(
            CommandContext<CommandSourceStack> context,
            String messageKey,
            BooleanFlagSetter setter
    ) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        Team team = resolveTeam(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        setter.set(source.getServer(), team, enabled);
        source.sendSuccess(() -> messageFor(source, messageKey, team.getShortName(), enabled), true);
        return 1;
    }

    private static boolean deniesAll(Team team) {
        return ClaimFlagService.deniesAllMobSpawn(team);
    }

    private static String formatMobRules(Team team) {
        List<String> parts = new ArrayList<>();
        for (String id : ClaimFlagService.mobSpawnDenies(team)) {
            parts.add(id + "=deny");
        }
        for (String id : ClaimFlagService.mobSpawnAllows(team)) {
            parts.add(id + "=allow");
        }
        if (parts.isEmpty()) {
            return "";
        }
        return " (" + String.join(", ", parts) + ")";
    }

    private static boolean parseDeny(String mode) throws CommandSyntaxException {
        return switch (mode) {
            case "allow" -> false;
            case "deny" -> true;
            default -> throw INVALID_MODE.create();
        };
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

    private static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }
        return CointCoreMessages.forConsole(key, args);
    }

    @FunctionalInterface
    private interface BooleanFlagSetter {
        void set(net.minecraft.server.MinecraftServer server, Team team, boolean value);
    }
}
