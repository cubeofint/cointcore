package com.mawlee.cointcore.command;

import com.mawlee.cointcore.ae.MeUniqueFilterConfig;
import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.config.JoinMessagesConfig;
import com.mawlee.cointcore.config.AdminChatConfig;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.MobCleanupConfig;
import com.mawlee.cointcore.config.WorldCleanupConfig;
import com.mawlee.cointcore.server.WorldCleanupService;
import com.mawlee.cointcore.config.RelpChatPrefixConfig;
import com.mawlee.cointcore.config.VoteConfig;
import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mawlee.cointcore.server.PeriodicMessageService;
import com.mawlee.cointcore.server.ScheduledRestartService;
import com.mawlee.cointcore.server.ServerRestartService;
import com.mawlee.cointcore.vote.VoteService;
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
        if (JoinMessagesConfig.reload()
                && MobCleanupConfig.reload()
                && WorldCleanupConfig.reload()
                && ChunkLimitConfig.reload()
                && VoteConfig.reload()
                && ServerAutomationConfig.reload()
                && SparkProfilerConfig.reload()
                && AdminChatConfig.reload()
                && MeUniqueFilterConfig.reload()) {
            NonStackableItemTagPack.ensureGenerated();
            VoteService.applySleepPercentage(source.getServer());
            PeriodicMessageService.resetRuntimeState();
            ScheduledRestartService.resetRuntimeState();
            WorldCleanupService.resetRuntimeState();
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
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_FAILED));
        } else {
            source.sendFailure(CointCoreMessages.forConsole(CointCoreMessages.SERVER_RESTART_FAILED));
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
}