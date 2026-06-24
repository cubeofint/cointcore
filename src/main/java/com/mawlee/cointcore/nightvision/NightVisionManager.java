package com.mawlee.cointcore.nightvision;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NightVisionManager {
    private static final String NIGHT_VISION_KEY = "cointcore:night_vision";
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private NightVisionManager() {
    }

    public static Set<UUID> enabledPlayers() {
        return Collections.unmodifiableSet(ENABLED);
    }

    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        if (enabled) {
            ENABLED.add(player.getUUID());
        } else {
            ENABLED.remove(player.getUUID());
        }

        player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG)
                .putBoolean(NIGHT_VISION_KEY, enabled);
    }

    public static void loadFromStorage(ServerPlayer player) {
        boolean enabled = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG)
                .getBoolean(NIGHT_VISION_KEY);

        if (enabled) {
            ENABLED.add(player.getUUID());
        } else {
            ENABLED.remove(player.getUUID());
        }
    }

    public static void clearRuntimeState() {
        ENABLED.clear();
    }
}
