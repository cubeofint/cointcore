package com.mawlee.cointcore.message;

import com.mawlee.cointcore.chatspy.ChatSpyService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.ignore.IgnoreService;
import com.mawlee.cointcore.mute.MuteManager;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class PrivateMessageService {
    private PrivateMessageService() {
    }

    public static boolean send(ServerPlayer sender, ServerPlayer target, String message) {
        if (MuteManager.isMuted(sender)) {
            MuteService.notifyIfMuted(sender);
            return false;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.MSG_CANNOT_MESSAGE_SELF));
            return false;
        }

        if (!isReachable(target)) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.MSG_PLAYER_OFFLINE, target.getGameProfile().getName()));
            return false;
        }

        deliver(sender, target, message);
        return true;
    }

    public static boolean reply(ServerPlayer sender, String message) {
        if (MuteManager.isMuted(sender)) {
            MuteService.notifyIfMuted(sender);
            return false;
        }

        UUID partnerId = PrivateMessageTargets.getLastPartner(sender.getUUID());
        if (partnerId == null) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.MSG_NO_REPLY_TARGET));
            return false;
        }

        ServerPlayer target = sender.server.getPlayerList().getPlayer(partnerId);
        if (target == null || !isReachable(target)) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.MSG_PLAYER_OFFLINE, resolveName(sender.server, partnerId)));
            return false;
        }

        deliver(sender, target, message);
        return true;
    }

    public static ServerPlayer findReachableTarget(MinecraftServer server, ServerPlayer sender, String name) {
        ServerPlayer match = null;

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (!online.getGameProfile().getName().equalsIgnoreCase(name)) {
                continue;
            }

            if (match != null) {
                return null;
            }

            match = online;
        }

        if (match == null || !isReachable(match) || match.getUUID().equals(sender.getUUID())) {
            return null;
        }

        return match;
    }

    public static boolean isReachable(ServerPlayer target) {
        return target != null && !VanishManager.isVanished(target);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        PrivateMessageTargets.forget(player.getUUID());
    }

    private static void deliver(ServerPlayer sender, ServerPlayer target, String message) {
        String senderName = sender.getGameProfile().getName();
        String targetName = target.getGameProfile().getName();

        sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.MSG_SENT, targetName, message));
        if (!IgnoreService.shouldHideFrom(target, sender)) {
            target.sendSystemMessage(CointCoreMessages.forPlayer(target, CointCoreMessages.MSG_RECEIVED, senderName, message));
            PrivateMessageTargets.link(sender.getUUID(), target.getUUID());
        }

        ChatSpyService.notifyPrivateMessage(sender, target, message);
    }

    private static String resolveName(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        return server.getProfileCache()
                .get(playerId)
                .map(profile -> profile.getName())
                .orElse("?");
    }
}
