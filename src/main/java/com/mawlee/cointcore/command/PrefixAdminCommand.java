package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.prefix.PrefixBulkReportService;
import com.mawlee.cointcore.prefix.PrefixCatalogExporter;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class PrefixAdminCommand {
    private PrefixAdminCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("prefixes")
                .requires(PrefixAdminCommand::canAdmin)
                .then(Commands.literal("export")
                        .executes(context -> export(context, false))
                        .then(Commands.literal("force").executes(context -> export(context, true)))
                        .then(Commands.literal("--force").executes(context -> export(context, true))))
                .then(Commands.literal("report-all")
                        .executes(PrefixAdminCommand::reportAll));
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.PREFIX_ADMIN);
        }
        return source.hasPermission(2);
    }

    private static int export(CommandContext<CommandSourceStack> context, boolean force) {
        CommandSourceStack source = context.getSource();
        PrefixCatalogExporter.export(source.getServer(), force, result -> source.getServer().execute(() ->
                source.sendSuccess(() -> switch (result.kind()) {
                    case SENT -> CointCoreMessages.forSource(
                            source, CointCoreMessages.PREFIX_EXPORT_OK, result.groups(), result.hash());
                    case UNCHANGED -> CointCoreMessages.forSource(
                            source, CointCoreMessages.PREFIX_EXPORT_UNCHANGED, result.hash());
                    case BUSY, RETRYING -> CointCoreMessages.forSource(source, CointCoreMessages.PREFIX_EXPORT_BUSY);
                    case UNAVAILABLE -> CointCoreMessages.forSource(source, CointCoreMessages.PREFIX_ADMIN_UNAVAILABLE);
                    case FAILED -> CointCoreMessages.forSource(source, CointCoreMessages.PREFIX_EXPORT_FAILED);
                }, result.kind() == PrefixCatalogExporter.ExportResult.Kind.SENT)));
        return 1;
    }

    private static int reportAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!PrefixBulkReportService.start(source.getServer())) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.PREFIX_REPORT_ALL_ALREADY), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.PREFIX_REPORT_ALL_STARTED), true);
        return 1;
    }
}
