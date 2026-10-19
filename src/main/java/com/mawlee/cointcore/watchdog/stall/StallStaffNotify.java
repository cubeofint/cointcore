package com.mawlee.cointcore.watchdog.stall;

import com.mawlee.cointcore.chat.ChatDiscordRelay;
import com.mawlee.cointcore.config.StallWatchdogConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe staff alert: packet send on connections, Discord via JDA queue.
 * Must not schedule work on the frozen server thread.
 */
public final class StallStaffNotify {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StallStaffNotify() {
    }

    public static void notifyStaff(MinecraftServer server, String text) {
        Component message = Component.literal("§c" + text);
        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(message, false);
        if (server != null) {
            List<ServerPlayer> snapshot = new ArrayList<>();
            try {
                snapshot.addAll(server.getPlayerList().getPlayers());
            } catch (Throwable exception) {
                LOGGER.warn("Stall notify could not snapshot players: {}", exception.toString());
            }
            for (ServerPlayer player : snapshot) {
                try {
                    if (isStaff(server, player)) {
                        player.connection.send(packet);
                    }
                } catch (Throwable exception) {
                    LOGGER.debug("Stall notify send failed for {}", player.getGameProfile().getName(), exception);
                }
            }
        }
        if (StallWatchdogConfig.isNotifyDiscord()) {
            ChatDiscordRelay.relaySystemAlert(text);
        }
    }

    private static boolean isStaff(MinecraftServer server, ServerPlayer player) {
        if (PermissionService.has(player, CointPermissionNodes.WATCHDOG)) {
            return true;
        }
        return server.getPlayerList().isOp(player.getGameProfile());
    }
}
