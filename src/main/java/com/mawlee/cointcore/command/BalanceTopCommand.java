package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.shop.GluonWalletSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code /balancetop} ({@code /baltop}): the 10 richest players by gluon balance, plus the caller's own place. */
public final class BalanceTopCommand {
    private static final int TOP = 10;

    private BalanceTopCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"balancetop", "baltop"}) {
            CommandRegistration.replaceRootLiteral(dispatcher, name, Commands.literal(name)
                    .requires(BalanceTopCommand::canUse)
                    .executes(context -> show(context.getSource())));
        }
    }

    private static boolean canUse(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.GLUONS_TOP);
        }
        return true;
    }

    private static int show(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<Map.Entry<UUID, Long>> ranking = GluonWalletSavedData.get(server).ranking();
        if (ranking.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.BALANCETOP_EMPTY), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.BALANCETOP_HEADER), false);
        UUID self = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        for (int place = 0; place < Math.min(TOP, ranking.size()); place++) {
            Map.Entry<UUID, Long> row = ranking.get(place);
            int shown = place + 1;
            String name = MuteService.resolveName(server, row.getKey()).orElse(row.getKey().toString().substring(0, 8));
            String key = row.getKey().equals(self) ? CointCoreMessages.BALANCETOP_SELF_ENTRY : CointCoreMessages.BALANCETOP_ENTRY;
            source.sendSuccess(() -> CointCoreMessages.forSource(source, key, shown, name, row.getValue()), false);
        }
        if (self != null) {
            for (int place = TOP; place < ranking.size(); place++) {
                if (ranking.get(place).getKey().equals(self)) {
                    int shown = place + 1;
                    long balance = ranking.get(place).getValue();
                    source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.BALANCETOP_YOUR_PLACE,
                            shown, balance), false);
                    break;
                }
            }
        }
        return Math.min(TOP, ranking.size());
    }
}
