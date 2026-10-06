package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.TickWatchdogConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.watchdog.OffenderAggregator;
import com.mawlee.cointcore.watchdog.TickStatsWindow;
import com.mawlee.cointcore.watchdog.TickWatchdogService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public final class WatchdogCommand {
    private WatchdogCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("watchdog")
                .requires(WatchdogCommand::canUse)
                .then(Commands.literal("report").executes(WatchdogCommand::report))
                .then(Commands.literal("top").executes(WatchdogCommand::top))
                .then(Commands.literal("tp")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1, 50))
                                .executes(WatchdogCommand::teleport)))
                .then(Commands.literal("start").executes(WatchdogCommand::start))
                .then(Commands.literal("stop").executes(WatchdogCommand::stop));
    }

    private static boolean canUse(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.WATCHDOG);
        }
        return source.hasPermission(2);
    }

    private static int report(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickWatchdogService service = TickWatchdogService.instance();
        if (!TickWatchdogConfig.isEnabled() && !service.isRunning()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_DISABLED));
            return 0;
        }
        String text = service.writeReportNow(source.getServer());
        String[] lines = text.split("\n");
        int shown = 0;
        for (String line : lines) {
            if (shown++ >= 40) {
                source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_REPORT_TRUNCATED), false);
                break;
            }
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int top(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickWatchdogService service = TickWatchdogService.instance();
        TickStatsWindow stats = service.stats();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Watchdog: avg=%.2f ms p95=%.2f ms max=%.2f ms TPS≈%.2f slow=%d running=%s",
                stats.averageMillis(),
                stats.percentile95Millis(),
                stats.maxMillis(),
                stats.estimatedTps(),
                stats.slowTicks(),
                service.isRunning()
        )), false);
        List<OffenderAggregator.OffenderSnapshot> list = service.currentTop(10);
        if (list.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_NO_DATA), false);
            return 1;
        }
        int index = 1;
        for (OffenderAggregator.OffenderSnapshot item : list) {
            int rank = index++;
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "%d) %s %s %s %d %d %d chunk=%d,%d %.2f ms ticks=%d",
                    rank,
                    item.kind().name().toLowerCase(Locale.ROOT),
                    item.typeId(),
                    item.dimension(),
                    item.x(),
                    item.y(),
                    item.z(),
                    item.chunkX(),
                    item.chunkZ(),
                    item.totalMillis(),
                    item.ticks()
            )), false);
        }
        return 1;
    }

    private static int teleport(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_PLAYER_ONLY));
            return 0;
        }
        int index = IntegerArgumentType.getInteger(context, "index");
        List<OffenderAggregator.OffenderSnapshot> list = TickWatchdogService.instance().lastTop();
        if (index > list.size()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_NO_ENTRY, index));
            return 0;
        }
        OffenderAggregator.OffenderSnapshot target = list.get(index - 1);
        ServerLevel level = null;
        for (ServerLevel candidate : source.getServer().getAllLevels()) {
            if (candidate.dimension().location().toString().equals(target.dimension())) {
                level = candidate;
                break;
            }
        }
        if (level == null) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_DIM_MISSING, target.dimension()));
            return 0;
        }
        player.teleportTo(
                level,
                target.x() + 0.5D,
                target.y() + 1.0D,
                target.z() + 0.5D,
                EnumSet.noneOf(RelativeMovement.class),
                player.getYRot(),
                player.getXRot()
        );
        ServerLevel dest = level;
        source.sendSuccess(() -> CointCoreMessages.forSource(
                source,
                CointCoreMessages.WATCHDOG_TP,
                index,
                target.typeId(),
                dest.dimension().location().toString(),
                target.x(),
                target.y(),
                target.z()
        ), true);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickWatchdogService.instance().start(source.getServer());
        TickWatchdogService.instance().setEnabled(true);
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_STARTED), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickWatchdogService.instance().setEnabled(false);
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.WATCHDOG_STOPPED), true);
        return 1;
    }
}
