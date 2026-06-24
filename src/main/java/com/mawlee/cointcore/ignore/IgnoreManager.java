package com.mawlee.cointcore.ignore;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IgnoreManager {
    private static final String IGNORED_KEY = "cointcore:ignored";
    private static final Map<UUID, Set<UUID>> IGNORED = new ConcurrentHashMap<>();

    private IgnoreManager() {
    }

    public static boolean isIgnored(ServerPlayer viewer, UUID senderId) {
        if (viewer == null || senderId == null || viewer.getUUID().equals(senderId)) {
            return false;
        }

        Set<UUID> ignored = IGNORED.get(viewer.getUUID());
        return ignored != null && ignored.contains(senderId);
    }

    public static boolean toggle(ServerPlayer viewer, UUID targetId) {
        Set<UUID> ignored = IGNORED.computeIfAbsent(viewer.getUUID(), id -> ConcurrentHashMap.newKeySet());
        boolean nowIgnored;

        if (ignored.contains(targetId)) {
            ignored.remove(targetId);
            nowIgnored = false;
        } else {
            ignored.add(targetId);
            nowIgnored = true;
        }

        if (ignored.isEmpty()) {
            IGNORED.remove(viewer.getUUID());
        }

        saveToStorage(viewer, ignored);
        return nowIgnored;
    }

    public static void loadFromStorage(ServerPlayer player) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!data.contains(IGNORED_KEY, Tag.TAG_LIST)) {
            IGNORED.remove(player.getUUID());
            return;
        }

        ListTag list = data.getList(IGNORED_KEY, Tag.TAG_STRING);
        if (list.isEmpty()) {
            IGNORED.remove(player.getUUID());
            return;
        }

        Set<UUID> ignored = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < list.size(); i++) {
            try {
                ignored.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignoredException) {
            }
        }

        if (ignored.isEmpty()) {
            IGNORED.remove(player.getUUID());
        } else {
            IGNORED.put(player.getUUID(), ignored);
        }
    }

    public static Set<UUID> ignoredTargets(ServerPlayer viewer) {
        Set<UUID> ignored = IGNORED.get(viewer.getUUID());
        if (ignored == null || ignored.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(ignored);
    }

    public static void clearRuntimeState() {
        IGNORED.clear();
    }

    private static void saveToStorage(ServerPlayer player, Set<UUID> ignored) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (ignored.isEmpty()) {
            data.remove(IGNORED_KEY);
            return;
        }

        ListTag list = new ListTag();
        ignored.forEach(id -> list.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        data.put(IGNORED_KEY, list);
    }
}
