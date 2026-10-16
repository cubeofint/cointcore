package com.mawlee.cointcore.slots;

import com.mawlee.cointcore.config.ReservedSlotsConfig;
import com.mawlee.cointcore.config.ReservedSlotsParse;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.players.PlayerList;

public final class ReservedSlotsService {
    private ReservedSlotsService() {
    }

    public static int effectiveMaxPlayers(int vanillaMax) {
        ReservedSlotsParse.Parsed settings = ReservedSlotsConfig.get();
        if (!settings.enabled()) {
            return vanillaMax;
        }
        return Math.max(vanillaMax, settings.totalSlots());
    }

    public static ServerStatus.Players adjustPing(ServerStatus.Players players) {
        if (players == null) {
            return null;
        }
        ReservedSlotsParse.Parsed settings = ReservedSlotsConfig.get();
        if (!settings.enabled()) {
            return players;
        }
        return new ServerStatus.Players(settings.publicSlots(), players.online(), players.sample());
    }

    public static Component loginDenial(PlayerList playerList, GameProfile profile, Component vanillaDenial) {
        ReservedSlotsParse.Parsed settings = ReservedSlotsConfig.get();
        if (!settings.enabled() || profile == null || profile.getId() == null) {
            return vanillaDenial;
        }
        if (vanillaDenial != null && !isLikelyFull(vanillaDenial)) {
            return vanillaDenial;
        }
        int online = playerList.getPlayerCount();
        if (online < settings.publicSlots() && vanillaDenial == null) {
            return null;
        }
        boolean bypass = ReservedSlotsPermissions.hasBypass(profile.getId());
        boolean reserved = bypass || ReservedSlotsPermissions.hasReserved(profile.getId());
        if (vanillaDenial != null) {
            return bypass ? null : vanillaDenial;
        }
        ReservedSlotsRules.Decision decision = ReservedSlotsRules.decide(
                online,
                settings.publicSlots(),
                settings.reservedSlots(),
                reserved,
                bypass
        );
        switch (decision) {
            case ALLOW -> {
                return null;
            }
            case PUBLIC_FULL -> {
                return russian(CointCoreMessages.RESERVED_SLOTS_PUBLIC_FULL);
            }
            case TOTAL_FULL -> {
                return russian(CointCoreMessages.RESERVED_SLOTS_TOTAL_FULL);
            }
            default -> throw new IllegalStateException("Unhandled reserved-slot decision: " + decision);
        }
    }

    private static boolean isLikelyFull(Component vanillaDenial) {
        if (vanillaDenial.getContents() instanceof TranslatableContents contents) {
            return "multiplayer.disconnect.server_full".equals(contents.getKey());
        }
        return false;
    }

    private static Component russian(String key) {
        return Component.literal(CointCoreMessages.translateKey("ru_ru", key));
    }
}
