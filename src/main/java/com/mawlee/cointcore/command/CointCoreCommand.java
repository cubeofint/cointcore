package com.mawlee.cointcore.command;

import com.mawlee.cointcore.ae.MeUniqueFilterConfig;
import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.afk.AfkService;
import com.mawlee.cointcore.config.AfkConfig;
import com.mawlee.cointcore.config.ChatDiscordRelayConfig;
import com.mawlee.cointcore.config.ArsPerfConfigs;
import com.mawlee.cointcore.config.CataclysmRespawnConfigs;
import com.mawlee.cointcore.config.ChatConfigs;
import com.mawlee.cointcore.config.ClaimsConfigs;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.CleanupConfigs;
import com.mawlee.cointcore.config.NaturalSpawnConfig;
import com.mawlee.cointcore.cataclysm.CataclysmStructureRespawnService;
import com.mawlee.cointcore.cataclysm.SunkenCityRespawnService;
import com.mawlee.cointcore.config.WorldCleanupConfig;
import com.mawlee.cointcore.server.WorldCleanupService;
import com.mawlee.cointcore.config.RelpChatPrefixConfig;
import com.mawlee.cointcore.config.VoteConfig;
import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.config.SoulSurgePerfConfig;
import com.mawlee.cointcore.config.SoulSurgeDenyConfig;
import com.mawlee.cointcore.config.TickAccelerationDenyConfig;
import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.config.ItemPerfConfig;
import com.mawlee.cointcore.config.LagFixesConfigs;
import com.mawlee.cointcore.config.StoragePerfConfigs;
import com.mawlee.cointcore.config.TickThrottleConfigs;
import com.mawlee.cointcore.ars.ArsGlyphThrottle;
import com.mawlee.cointcore.ars.CrushRecipeCache;
import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import com.mawlee.cointcore.config.SpawnerByproductConfig;
import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mawlee.cointcore.config.DimensionWipeConfig;
import com.mawlee.cointcore.config.StarterKitConfig;
import com.mawlee.cointcore.server.PeriodicMessageService;
import com.mawlee.cointcore.server.ScheduledRestartService;
import com.mawlee.cointcore.server.ServerRestartService;
import com.mawlee.cointcore.server.DimensionWipeService;
import com.mawlee.cointcore.server.DimensionWipePending;
import com.mawlee.cointcore.kit.StarterKitService;
import com.mawlee.cointcore.vote.VoteService;
import com.mawlee.cointcore.ftb.ChunkBonusService;
import com.mawlee.cointcore.ftb.FtbEssentialsIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

public final class CointCoreCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CointCoreCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cointcore")
                .then(Commands.literal("reload")
                        .requires(CointCoreCommand::canReload)
                        .executes(CointCoreCommand::reloadConfig))
                .then(Commands.literal("restart")
                        .requires(CointCoreCommand::canRestart)
                        .then(Commands.literal("cancel")
                                .executes(CointCoreCommand::cancelRestart))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                                .executes(CointCoreCommand::scheduleRestart))
                        .executes(CointCoreCommand::scheduleRestartZero))
                .then(Commands.literal("worldcleanup")
                        .requires(CointCoreCommand::canReload)
                        .then(Commands.literal("status")
                                .executes(CointCoreCommand::worldCleanupStatus))
                        .then(Commands.literal("run")
                                .executes(CointCoreCommand::runWorldCleanup)))
                .then(Commands.literal("dimwipe")
                        .requires(CointCoreCommand::canRestart)
                        .then(Commands.literal("status")
                                .executes(CointCoreCommand::dimWipeStatus))
                        .then(Commands.literal("now")
                                .executes(CointCoreCommand::dimWipeNow)))
                .then(StarterKitCommand.cointCoreBranch())
                .then(Commands.literal("sunkencity")
                        .requires(CointCoreCommand::canReload)
                        .then(Commands.literal("status")
                                .executes(CointCoreCommand::sunkenCityStatus))
                        .then(Commands.literal("force")
                                .executes(CointCoreCommand::sunkenCityForce)))
                .then(Commands.literal("cataclysmspots")
                        .requires(CointCoreCommand::canReload)
                        .then(Commands.literal("status")
                                .executes(CointCoreCommand::cataclysmSpotsStatus))
                        .then(Commands.literal("force")
                                .executes(CointCoreCommand::cataclysmSpotsForce)))
                // Alias kept for older scripts/docs.
                .then(Commands.literal("frostedprison")
                        .requires(CointCoreCommand::canReload)
                        .then(Commands.literal("status")
                                .executes(CointCoreCommand::cataclysmSpotsStatus))
                        .then(Commands.literal("force")
                                .executes(CointCoreCommand::cataclysmSpotsForce)))
                .then(ChunkLimitCommand.chunkLimitCommand());

        var claimFlags = ClaimFlagCommand.claimFlagCommand();
        if (claimFlags != null) {
            root = root.then(claimFlags);
        }

        if (FtbEssentialsIntegration.isAvailable()) {
            root = root.then(KitCreditCommand.kitCreditCommand());
            event.getDispatcher().register(KitCreditCommand.playerKitBalanceCommand());
        }

        event.getDispatcher().register(root);
    }

    private static boolean canReload(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.RELOAD);
        }
        return source.hasPermission(2);
    }

    private static boolean canRestart(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.RESTART);
        }
        return source.hasPermission(2);
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        RelpChatPrefixConfig.load();
        if (ChatConfigs.reload()
                && AfkConfig.reload()
                && ChatDiscordRelayConfig.reload()
                && NaturalSpawnConfig.reload()
                && CleanupConfigs.reload()
                && CataclysmRespawnConfigs.reload()
                && ChunkLimitConfig.reload()
                && ClaimsConfigs.reload()
                && VoteConfig.reload()
                && ServerAutomationConfig.reload()
                && SparkProfilerConfig.reload()
                && TickThrottleConfigs.reload()
                && SoulSurgePerfConfig.reload()
                && SoulSurgeDenyConfig.reload()
                && TickAccelerationDenyConfig.reload()
                && SpawnerPerfConfig.reload()
                && ItemPerfConfig.reload()
                && ArsPerfConfigs.reload()
                && StoragePerfConfigs.reload()
                && LagFixesConfigs.reload()
                && ExplosionTerrainConfig.reload()
                && SpawnerByproductConfig.reload()
                && DimensionWipeConfig.reload()
                && StarterKitConfig.reload()
                && MeUniqueFilterConfig.reload()) {
            CrushRecipeCache.invalidate();
            ArsGlyphThrottle.clear();
            NonStackableItemTagPack.ensureGenerated();
            VoteService.applySleepPercentage(source.getServer());
            PeriodicMessageService.resetRuntimeState();
            ScheduledRestartService.resetRuntimeState();
            DimensionWipeService.resetRuntimeState();
            StarterKitService.syncCooldownFromConfig();
            WorldCleanupService.resetRuntimeState();
            ChunkBonusService.refreshAllOnlinePlayers(source.getServer());
            AfkService.rearmAllOnline(source.getServer());
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.CONFIG_RELOAD_SUCCESS), true);
            } else {
                source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.CONFIG_RELOAD_SUCCESS), true);
            }
            return 1;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.CONFIG_RELOAD_FAILED));
        } else {
            source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.CONFIG_RELOAD_FAILED));
        }
        return 0;
    }

    private static int scheduleRestart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");

        if (seconds <= 0) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_NOW), true);
            } else {
                source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_NOW), true);
            }
            source.getServer().execute(() -> ServerRestartService.executeImmediateRestart(source.getServer()));
            return 1;
        }

        try {
            ServerRestartService.scheduleRestart(
                    source.getServer(),
                    seconds,
                    CointCoreMessages.SERVER_RESTART_MANUAL_SCHEDULED,
                    seconds
            );
        } catch (Throwable exception) {
            LOGGER.error("Failed to schedule server restart", exception);
            sendRestartFailure(source);
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_MANUAL_SCHEDULED, seconds), true);
        } else {
            source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_MANUAL_SCHEDULED, seconds), true);
        }
        return 1;
    }

    private static int scheduleRestartZero(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_INVALID_DURATION));
        } else {
            source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_INVALID_DURATION));
        }
        return 0;
    }

    private static int cancelRestart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            if (ServerRestartService.cancelPendingRestart()) {
                DimensionWipePending.clear();
                if (source.getEntity() instanceof ServerPlayer player) {
                    source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_CANCELLED), true);
                } else {
                    source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_CANCELLED), true);
                }
                return 1;
            }
        } catch (Throwable exception) {
            LOGGER.error("Failed to cancel pending server restart", exception);
            sendRestartFailure(source);
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_NO_PENDING));
        } else {
            source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_NO_PENDING));
        }
        return 0;
    }

    private static void sendRestartFailure(CommandSourceStack source) {
        try {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_FAILED));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_FAILED));
            }
        } catch (Throwable messageFailure) {
            // Never let messaging take down the server (e.g. broken/hot-swapped lang classes).
            source.sendFailure(Component.literal("CointCore: failed to schedule restart."));
        }
    }

    private static int worldCleanupStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!WorldCleanupConfig.getItemClear().enabled()) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.WORLD_CLEANUP_DISABLED));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.WORLD_CLEANUP_DISABLED));
            }
            return 0;
        }

        WorldCleanupService.triggerManualStatus(source.getServer());
        return 1;
    }

    private static int runWorldCleanup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!WorldCleanupConfig.getItemClear().enabled()) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.WORLD_CLEANUP_DISABLED));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.WORLD_CLEANUP_DISABLED));
            }
            return 0;
        }

        WorldCleanupService.triggerManualCleanup(source.getServer());
        return 1;
    }

    private static int dimWipeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String summary = DimensionWipeService.statusSummary();
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.DIMWIPE_STATUS, summary), false);
        } else {
            source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.DIMWIPE_STATUS, summary), false);
        }
        return 1;
    }

    private static int dimWipeNow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DimensionWipeConfig.Settings settings = DimensionWipeConfig.get();
        if (settings.dimensions().isEmpty()) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.DIMWIPE_DISABLED));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.DIMWIPE_DISABLED));
            }
            return 0;
        }

        int delay = settings.restartDelaySeconds();
        boolean started = DimensionWipeService.triggerNow(source.getServer(), delay);
        if (!started) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.DIMWIPE_NOW_FAILED));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.DIMWIPE_NOW_FAILED));
            }
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.DIMWIPE_NOW_OK), true);
        } else {
            source.sendSuccess(() -> CointCoreMessages.forConsole(CointCoreMessages.DIMWIPE_NOW_OK), true);
        }
        return 1;
    }

    private static int sunkenCityStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player only"));
            return 0;
        }
        String status = SunkenCityRespawnService.statusNear(player);
        source.sendSuccess(() -> Component.literal(status), false);
        return 1;
    }

    private static int sunkenCityForce(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player only"));
            return 0;
        }
        String result = SunkenCityRespawnService.forceRepopulate(player);
        source.sendSuccess(() -> Component.literal(result), true);
        return 1;
    }

    private static int cataclysmSpotsStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player only"));
            return 0;
        }
        String status = CataclysmStructureRespawnService.statusNear(player);
        source.sendSuccess(() -> Component.literal(status), false);
        return 1;
    }

    private static int cataclysmSpotsForce(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player only"));
            return 0;
        }
        String result = CataclysmStructureRespawnService.forceRepopulate(player);
        source.sendSuccess(() -> Component.literal(result), true);
        return 1;
    }
}