package com.mawlee.cointcore.vanish;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishManager {
    private static final String VANISHED_KEY = "cointcore:vanished";
    private static final String MOB_STEALTH_KEY = "cointcore:vanish_mob_stealth";
    private static final String LEGACY_MOB_TARGET_KEY = "cointcore:vanish_mobs";
    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> HIDDEN_FROM_MOBS = ConcurrentHashMap.newKeySet();

    private VanishManager() {
    }

    public static Set<UUID> vanishedPlayers() {
        return Collections.unmodifiableSet(VANISHED);
    }

    public static boolean isVanished(ServerPlayer player) {
        return player != null && VANISHED.contains(player.getUUID());
    }

    public static boolean isVanished(UUID uuid) {
        return VANISHED.contains(uuid);
    }

    public static boolean canMobsTarget(ServerPlayer player) {
        return player != null && !HIDDEN_FROM_MOBS.contains(player.getUUID());
    }

    public static boolean isHiddenFromMobs(ServerPlayer player) {
        return player != null && HIDDEN_FROM_MOBS.contains(player.getUUID());
    }

    public static boolean shouldHideFrom(ServerPlayer vanished, ServerPlayer viewer) {
        if (vanished == null || viewer == null) {
            return false;
        }

        if (vanished.getUUID().equals(viewer.getUUID())) {
            return false;
        }

        return isVanished(vanished) && !VanishVisibility.canSeeVanished(viewer);
    }

    public static boolean shouldHideFrom(UUID vanishedId, ServerPlayer viewer) {
        if (viewer == null || vanishedId == null) {
            return false;
        }

        if (vanishedId.equals(viewer.getUUID())) {
            return false;
        }

        return isVanished(vanishedId) && !VanishVisibility.canSeeVanished(viewer);
    }

    public static void setVanished(ServerPlayer player, boolean vanished) {
        if (vanished) {
            VANISHED.add(player.getUUID());
        } else {
            VANISHED.remove(player.getUUID());
        }

        player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG)
                .putBoolean(VANISHED_KEY, vanished);

        VanishInteractionTracker.trackVanished(player, vanished);
    }

    public static void setHiddenFromMobs(ServerPlayer player, boolean hidden) {
        if (hidden) {
            HIDDEN_FROM_MOBS.add(player.getUUID());
        } else {
            HIDDEN_FROM_MOBS.remove(player.getUUID());
        }

        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        data.putBoolean(MOB_STEALTH_KEY, hidden);
        data.remove(LEGACY_MOB_TARGET_KEY);
    }

    public static void loadFromStorage(ServerPlayer player) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean storedVanished = data.getBoolean(VANISHED_KEY);
        boolean hiddenFromMobs = resolveHiddenFromMobs(data);

        if (storedVanished) {
            VANISHED.add(player.getUUID());
        } else {
            VANISHED.remove(player.getUUID());
        }

        if (hiddenFromMobs) {
            HIDDEN_FROM_MOBS.add(player.getUUID());
        } else {
            HIDDEN_FROM_MOBS.remove(player.getUUID());
        }

        VanishInteractionTracker.trackVanished(player, storedVanished);
    }

    private static boolean resolveHiddenFromMobs(net.minecraft.nbt.CompoundTag data) {
        if (data.contains(MOB_STEALTH_KEY)) {
            return data.getBoolean(MOB_STEALTH_KEY);
        }

        if (data.contains(LEGACY_MOB_TARGET_KEY)) {
            return !data.getBoolean(LEGACY_MOB_TARGET_KEY);
        }

        return false;
    }

    public static void clearRuntimeState() {
        VANISHED.clear();
        HIDDEN_FROM_MOBS.clear();
        VanishInteractionTracker.clear();
    }
}
